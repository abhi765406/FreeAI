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

## Attachments (v1.2)
Tap the paperclip next to the message box.
- Images (JPG/PNG/WebP/GIF): resized to 1568px and sent to the model as vision input
- PDF: uploaded to your Puter storage (folder `claude-uploads`) and read by the model
- Zip: file list + contents of text/code files are sent (node_modules, .git, binaries skipped)
- Word (.docx): text is extracted
- Text/code/CSV/JSON/etc.: sent as text
Limits: 8 files per message, PDF 25 MB, zip 60 MB, long text is truncated.
