# Claude AI (Android WebView + Puter.js)

Full project. The white-screen fix is already included.

## Build the APK

**Option A: GitHub (no local setup)**
1. Create a new repo, upload everything in this folder (keep the `.github` folder).
2. Open the *Actions* tab, run **Build APK** (it also runs on every push).
3. When it finishes, download the `app-debug` artifact: that is your APK.

**Option B: Android Studio**
Open this folder, wait for Gradle sync, then Build > Build APK(s).

**Option C: command line** (JDK 17 + Android SDK installed)
`./gradlew assembleDebug` -> `app/build/outputs/apk/debug/app-debug.apk`

## Notes
- `app/src/main/assets/claude.html` is the whole UI. Edit it directly; there is no patch.py step anymore.
- First message: a Puter sign-in window opens. Finish signing in, then the chat continues.
- Uninstall the old APK first if the new one won't install over it (different signing key).
