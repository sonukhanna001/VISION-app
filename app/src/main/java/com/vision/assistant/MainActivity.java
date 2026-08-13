package com.vision.assistant;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;

    private TextView statusText;
    private TextView recognizedText;
    private TextView logText;
    private ImageButton micButton;

    private boolean ttsReady = false;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grantResults -> {
                boolean allGranted = true;
                for (Boolean granted : grantResults.values()) {
                    if (granted == null || !granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    startListening();
                } else {
                    statusText.setText(R.string.perm_needed);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        recognizedText = findViewById(R.id.recognizedText);
        logText = findViewById(R.id.logText);
        micButton = findViewById(R.id.micButton);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("hi", "IN"));
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        micButton.setOnClickListener(v -> checkPermissionsAndListen());
    }

    private void checkPermissionsAndListen() {
        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.READ_CONTACTS);
        }
        if (needed.isEmpty()) {
            startListening();
        } else {
            permissionLauncher.launch(needed.toArray(new String[0]));
        }
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.setText("इस डिवाइस पर वॉइस पहचान उपलब्ध नहीं है");
            return;
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                statusText.setText(R.string.listening);
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override public void onError(int error) {
                statusText.setText(R.string.tap_to_speak);
                log("त्रुटि कोड: " + error);
            }

            @Override public void onResults(Bundle results) {
                statusText.setText(R.string.tap_to_speak);
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spoken = matches.get(0);
                    recognizedText.setText(spoken);
                    handleCommand(spoken);
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(intent);
    }

    // ---------- Command parsing ----------

    private void handleCommand(String rawText) {
        String text = rawText.toLowerCase(Locale.forLanguageTag("hi-IN")).trim();
        log("सुना: " + rawText);

        if (containsAny(text, "यूट्यूब", "youtube")) {
            launchApp("com.google.android.youtube", "https://www.youtube.com");
            speak("यूट्यूब खोल रहा हूँ");
            return;
        }

        if (containsAny(text, "व्हाट्सएप", "whatsapp")) {
            launchApp("com.whatsapp", null);
            speak("व्हाट्सएप खोल रहा हूँ");
            return;
        }

        if (containsAny(text, "क्रोम", "chrome", "ब्राउज़र")) {
            launchApp("com.android.chrome", "https://www.google.com");
            speak("क्रोम खोल रहा हूँ");
            return;
        }

        if (containsAny(text, "कॉल", "call", "फ़ोन करो", "फोन करो")) {
            String name = extractNameAfterKeywords(text, "कॉल", "call", "को", "फ़ोन करो", "फोन करो");
            handleCall(name);
            return;
        }

        if (containsAny(text, "मैसेज", "sms", "संदेश")) {
            String name = extractNameAfterKeywords(text, "मैसेज", "sms", "संदेश", "को");
            handleSms(name);
            return;
        }

        speak("माफ़ कीजिए, मुझे यह आदेश समझ नहीं आया");
        log("अज्ञात कमांड");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    /**
     * Very simple heuristic: strips known command keywords out of the
     * recognized sentence and returns whatever text remains as the
     * assumed contact name. Good enough for a V1 prototype.
     */
    private String extractNameAfterKeywords(String text, String... keywords) {
        String remaining = text;
        for (String k : keywords) {
            remaining = remaining.replace(k, " ");
        }
        return remaining.trim();
    }

    // ---------- Contacts / Dialer / SMS ----------

    private void handleCall(String name) {
        if (name.isEmpty()) {
            speak("किसे कॉल करना है?");
            return;
        }
        String number = lookupContactNumber(name);
        if (number == null) {
            speak(name + " नाम का कोई कॉन्टैक्ट नहीं मिला");
            log("कॉन्टैक्ट नहीं मिला: " + name);
            return;
        }
        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
        startActivityIfPossible(dialIntent, "डायलर नहीं खुल पाया");
        speak(name + " को कॉल करने के लिए डायलर खोल रहा हूँ");
    }

    private void handleSms(String name) {
        if (name.isEmpty()) {
            speak("किसे संदेश भेजना है?");
            return;
        }
        String number = lookupContactNumber(name);
        if (number == null) {
            speak(name + " नाम का कोई कॉन्टैक्ट नहीं मिला");
            log("कॉन्टैक्ट नहीं मिला: " + name);
            return;
        }
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
        startActivityIfPossible(smsIntent, "संदेश ऐप नहीं खुल पाया");
        speak(name + " के लिए संदेश स्क्रीन खोल रहा हूँ");
    }

    private String lookupContactNumber(String name) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        };
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
        String[] selectionArgs = {"%" + name + "%"};

        try (Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                return cursor.getString(numberIndex);
            }
        } catch (SecurityException e) {
            log("कॉन्टैक्ट्स अनुमति त्रुटि");
        }
        return null;
    }

    // ---------- App launching ----------

    private void launchApp(String packageName, String fallbackUrl) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            startActivity(launchIntent);
            return;
        }
        if (fallbackUrl != null) {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl));
            startActivityIfPossible(viewIntent, "ऐप नहीं मिला");
        } else {
            Toast.makeText(this, "ऐप इंस्टॉल नहीं है", Toast.LENGTH_SHORT).show();
        }
    }

    private void startActivityIfPossible(Intent intent, String errorMessage) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            log(errorMessage);
        }
    }

    // ---------- Feedback helpers ----------

    private void speak(String message) {
        if (ttsReady && textToSpeech != null) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "vision_utt");
        }
        log(message);
    }

    private void log(String message) {
        logText.setText(message);
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
