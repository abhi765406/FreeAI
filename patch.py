#!/usr/bin/env python3
# Generates app/src/main/assets/claude.html from the original Claude.html
import os, sys

src = os.path.join("unlimited claude", "Claude.html")
dst = os.path.join("app", "src", "main", "assets", "claude.html")

if not os.path.exists(src):
    sys.exit("Place this script next to the 'unlimited claude' folder")

h = open(src, encoding="utf-8").read()

def rep(old, new):
    global h
    assert old in h, "Pattern not found - HTML may differ from expected version"
    h = h.replace(old, new, 1)

# 1. viewport
rep('<meta name="viewport" content="width=device-width, initial-scale=1.0">',
    '<meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">')

# 2. mobile CSS (drawer sidebar + hamburger)
rep("    </style>", """        /* ---- Android / small-screen adaptation ---- */
        .menu-toggle { display:none; position:fixed; top:10px; left:10px; z-index:210;
            width:40px; height:40px; border-radius:12px; border:1px solid #404040;
            background-color:rgba(38,38,38,0.92); color:#e5e5e5; font-size:18px;
            cursor:pointer; align-items:center; justify-content:center; padding:0; }
        .sidebar-backdrop { display:none; position:fixed; inset:0; z-index:90;
            background-color:rgba(0,0,0,0.55); }
        .sidebar-backdrop.show { display:block; }
        @media (max-width: 820px) {
            .sidebar { position:fixed; left:0; top:0; bottom:0; z-index:100;
                transform:translateX(-105%); transition:transform 0.22s ease;
                box-shadow:0 0 44px rgba(0,0,0,0.6); width:280px; }
            .sidebar.open { transform:translateX(0); }
            .menu-toggle { display:flex; }
            .content-area { padding:12px; }
            .welcome-screen { padding:52px 16px 16px; }
        }
    </style>""")

# 3. hamburger JS
rep("</body>", """    <script>
    (function () {
        var sb = document.querySelector('.sidebar');
        if (!sb) return;
        var backdrop = document.createElement('div');
        backdrop.className = 'sidebar-backdrop';
        document.body.appendChild(backdrop);
        var btn = document.createElement('button');
        btn.className = 'menu-toggle';
        btn.setAttribute('aria-label', 'Menu');
        btn.textContent = '\\u2630';
        document.body.appendChild(btn);
        function close() { sb.classList.remove('open'); backdrop.classList.remove('show'); }
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            if (sb.classList.contains('open')) close(); else sb.classList.add('open'), backdrop.classList.add('show');
        });
        backdrop.addEventListener('click', close);
        sb.addEventListener('click', function (e) {
            var t = e.target;
            if (t && (t.classList.contains('recent-item') || t.id === 'newChatBtn')) close();
        });
    })();
    </script>
</body>""")

# 4. streaming: for-await -> universal reader (works on old WebViews)
rep("""        async function sendMessage(message) {""", """        function handleStreamPart(part, onText) {
            if (part && part.text) { onText(part.text); return; }
            if (typeof part === 'string') { onText(part); return; }
            if (part && part.value && part.value.text) { onText(part.value.text); }
        }
        async function readStreamParts(response, onText) {
            if (response && typeof response.getReader === 'function') {
                const reader = response.getReader();
                while (true) { const r = await reader.read(); if (r.done) break; handleStreamPart(r.value, onText); }
                return;
            }
            if (typeof Symbol !== 'undefined' && Symbol.asyncIterator && response && response[Symbol.asyncIterator]) {
                const it = response[Symbol.asyncIterator]();
                while (true) { const r = await it.next(); if (r.done) break; handleStreamPart(r.value, onText); }
                return;
            }
            if (response && response[Symbol.iterator]) {
                for (const part of response) { handleStreamPart(part, onText); }
                return;
            }
            handleStreamPart(response, onText);
        }
        async function sendMessage(message) {""")

rep("""                for await (const part of response) {
                    if (part?.text) {
                        fullResponse += part.text;
                        renderFormattedMessage(fullResponse, messageContent);
                        scrollToBottom();
                    }
                }""",
"""                await readStreamParts(response, function (text) {
                    fullResponse += text;
                    renderFormattedMessage(fullResponse, messageContent);
                    scrollToBottom();
                });""")

# 5-6. optional chaining (Chrome 80+) -> safe forms
rep("if (authResponse && (authResponse.message?.content || authResponse.success !== false)) {",
    "if (authResponse && ((authResponse.message && authResponse.message.content) || authResponse.success !== false)) {")
rep("document.getElementById('chatInput')?.focus();",
    "var _ci = document.getElementById('chatInput'); if (_ci) _ci.focus();")

# 7. copy button bridge
rep("""            copyBtn.onclick = () => {
                navigator.clipboard.writeText(code).then(() => {
                    copyBtn.textContent = 'Copied!';
                    setTimeout(() => { copyBtn.innerHTML = copyIcon; }, 2000);
                });
            };""",
"""            copyBtn.onclick = () => {
                const markCopied = () => { copyBtn.textContent = 'Copied!'; setTimeout(() => { copyBtn.innerHTML = copyIcon; }, 2000); };
                try {
                    if (window.AndroidBridge) { AndroidBridge.copyText(code); markCopied(); return; }
                    if (navigator.clipboard && navigator.clipboard.writeText) {
                        navigator.clipboard.writeText(code).then(markCopied).catch(function () {});
                    }
                } catch (err) { }
            };""")

# 8. download button bridge
rep("""                const filename = `claude-code.${extension}`;
                const blob = new Blob([code], { type: 'text/plain' });""",
"""                const filename = `claude-code.${extension}`;
                if (window.AndroidBridge) { AndroidBridge.saveFile(filename, code); return; }
                const blob = new Blob([code], { type: 'text/plain' });""")

rep("<title>Claude Interface - Fully Functional</title>", "<title>Claude AI</title>")

# 9. Explain the Android asset origin instead of warning about file://.
rep("""                if (window.location.protocol === 'file:') {
                    protocolNotice.innerHTML = '✅ <strong>File mode:</strong> Authentication should work perfectly here!';
                    protocolNotice.style.color = '#22c55e';
                } else if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {""",
"""                if (window.location.hostname === 'appassets.androidplatform.net') {
                    protocolNotice.innerHTML = '📱 <strong>Android mode:</strong> Secure WebView + Puter authentication enabled.';
                    protocolNotice.style.color = '#22c55e';
                } else if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {""")

os.makedirs(os.path.dirname(dst), exist_ok=True)
open(dst, "w", encoding="utf-8").write(h)
print("OK ->", dst, len(h), "chars")
