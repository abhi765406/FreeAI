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

## v1.3 features
- **Markdown** replies (headings, lists, tables, bold, links).
- **Download all (.zip):** shown when a reply has 2+ named files. Files go to your Downloads folder (Android 10+).
- **Live preview:** HTML/SVG code cards get a Preview button (runs in a sandbox) plus full screen. Multi-file web projects (index.html + style.css + app.js) are stitched together automatically.
- **Web search:** toggle "Web search" above the message box. Claude calls a search tool; the app runs it through Puter's OpenAI web-search model and gives the result back to Claude. If tools are rejected, it answers without search.
- **Saved chats:** stored on the phone, survive closing the app, delete with the x in the side menu (last 50 chats).
- **Documents:** ask for a Word / Excel / PDF file. Word and Excel are generated in the app. PDF opens Android's print dialog: choose "Save as PDF".
- Back button closes full-screen preview / side menu before leaving the app.

## v1.4: more models (Claude is untouched and stays the default)
Tap the model name above the message box to open the picker.
- **Claude** (4 models): uses your Puter credits, as before.
- **Free & fast, your own API key** (does NOT use Puter credits): OpenRouter free models, Groq, Cerebras, or any OpenAI-compatible service. Add a key once; the list of models is loaded live.
  - OpenRouter: openrouter.ai/keys. Free models: 20 requests/min, 50/day (1,000/day after a one-time $10 top-up).
  - Groq: console.groq.com/keys. Free plan, per-model limits (about 30 requests/min plus a daily token cap).
- **Open-source via Puter**: live list with prices per 1M tokens. Uses Puter credits (cheaper than Claude).
Your last picked model is remembered for new chats. Pick a Claude model to go back.
Limits of non-Claude models here: no web search, no PDF reading; images only on vision-capable models.
API keys are stored only on the phone (plain text in app storage).
