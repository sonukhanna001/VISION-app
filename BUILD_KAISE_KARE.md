# VISION V1 — APK Kaise Banayein

Is sandbox me Android SDK/build-tools aur internet access nahi hai, isliye
seedha .apk compile nahi ho saka. Poora Android Studio project taiyaar hai,
ab background bhi Iron Man ke JARVIS jaisa glowing cyan HUD look me hai —
bas neeche diye steps follow karke APK ban jaayegi.

## Steps (GitHub Actions se — bina Android Studio install kiye, sabse aasaan)

1. GitHub par ek naya **empty repository** banayein (public ya private, koi
   README add mat karein).
2. Is poore `VISION_V1` folder (jisme `.github/workflows/build-apk.yml` bhi
   hai) ko us repo me push kar dein:
   ```bash
   cd VISION_V1
   git init
   git add .
   git commit -m "VISION V1 - Jarvis style"
   git branch -M main
   git remote add origin https://github.com/<aapka-username>/<repo-naam>.git
   git push -u origin main
   ```
3. Push hote hi GitHub khud-ba-khud APK build karega — repo ke **Actions**
   tab me jaake "Build VISION APK" workflow khulega.
4. Build complete (green tick) hone par us run ke **Artifacts** section me
   `VISION-debug-apk` milega — usse download karke unzip karein, andar
   `app-debug.apk` hogi.
5. Us APK ko phone me bhej kar install kar lein (Settings me "Install
   unknown apps" allow karna padega).

Is tarike me aapko Android Studio install karne ki zaroorat nahi — GitHub
ke servers par hi build hoti hai.

## Steps (Android Studio se — sabse aasaan)

1. [Android Studio](https://developer.android.com/studio) install karein
   (agar pehle se nahi hai).
2. Is folder (`VISION_V1`) ko **File → Open** se Android Studio me kholein.
3. Studio khud Gradle sync karega aur zaroori SDK components download karega
   (internet chahiye hoga).
4. Upar menu se **Build → Build Bundle(s) / APK(s) → Build APK(s)** dabayein.
5. Build complete hone par "locate" link se APK milega:
   `app/build/outputs/apk/debug/app-debug.apk`
6. Us APK file ko apne phone me bhej kar install kar lein
   (Settings me "Install unknown apps" allow karna padega).

## Steps (Command line se, agar SDK already installed hai)

```bash
cd VISION_V1
./gradlew assembleDebug
```
APK yahin milegi: `app/build/outputs/apk/debug/app-debug.apk`

(Agar `gradlew` executable nahi hai to pehle Android Studio me ek baar kholein,
wo wrapper khud generate kar dega, ya `chmod +x gradlew` chalayein.)

## App me kya hai

- Hindi voice input (bolke command dena)
- Text-to-speech (Hindi me jawab bolna)
- Contact naam se dhundh kar dialer / SMS screen kholna
- YouTube, WhatsApp, Chrome launch karna
- Basic command parsing (jaise "Rahul ko call karo", "YouTube kholo")

## Permissions

App pehli baar chalane par Microphone aur Contacts ki permission maangega —
allow karna zaroori hai warna voice commands aur contact-lookup kaam nahi
karenge.

## Aage V2 me kya add hoga (README ke mutabik)

- Live AI / server-side API integration
- Seedha call karna (dialer ke bina)
