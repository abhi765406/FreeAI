# Claude AI (Android)

Native Android APK of the open-source self-hosted Claude interface
(https://github.com/hassanmsthf11/unlimited-claude-AI), wrapped in a
zero-bloat WebView shell. Streams Claude answers via the free Puter.js tier.

## Build with GitHub Actions
1. Create a new repo, upload ALL contents of this folder to the repo root.
2. Actions -> "Build APK" -> Run workflow.
3. Download the ClaudeAI-app artifact - that is your APK.

## How to use the app
1. Install the APK (allow "install from unknown sources" if asked).
2. Open it - internet is required.
3. Type a message and send it. On first use a full-screen Puter sign-in
   popup appears - create / sign in to your free Puter account.
   This is the one-time authentication that unlocks free Claude access.
4. Tap the model name to switch between Sonnet 4 / Opus 4 / Sonnet 3.7.
5. Tap the hamburger (top-left) for chat history and New chat.
6. Code answers render as artifacts - use Copy / Download on any code block.
   Downloads are saved to Android/data/com.claudeai.app/files (visible in any
   file manager), no storage permission needed.
7. Your recent chats are remembered on the device (local storage only).

## Troubleshooting
- "Unsupported Protocol" warning: fixed in v1.1 - the page is now served
  from an https://localhost/ origin inside the WebView.
- White popup on Authenticate: make sure the device has a working internet
  connection, then tap Authenticate again.
- Popup did not appear: press Back to close the app fully and open it again.

## Notes
- Only permissions: INTERNET + network state (the AI answers must come from
  somewhere).
- Back button goes back in history; on the first screen it asks before exit.
- Rotation does not lose your chat.
