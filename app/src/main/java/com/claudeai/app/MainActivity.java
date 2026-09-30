package com.claudeai.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Hosts assets/claude.html in a WebView.
 *
 * Fixes vs. the previous build (white screen on first prompt):
 *  1. The page is served with an https base URL instead of file:///android_asset.
 *     Puter's sign-in popup talks back to the page with postMessage; from a file://
 *     page the origin is "null", so the handshake never completes and the popup
 *     stays blank/white.
 *  2. setJavaScriptCanOpenWindowsAutomatically(true): Puter opens its login popup
 *     from inside an async call (not a direct tap), which WebView blocks by default.
 *  3. The popup window no longer has a hard white background, has a close button,
 *     shows progress, and shows an error instead of staying blank.
 *  4. The "; wv" marker is removed from the user agent so login pages that reject
 *     embedded WebViews behave like a normal Chrome.
 *  5. FLAG_FULLSCREEN removed: it disables adjustResize, so the keyboard covered the input.
 *  6. onRenderProcessGone is handled so a WebView renderer crash recreates the view
 *     instead of leaving a blank screen.
 */
public class MainActivity extends Activity {

    private static final String BASE_URL = "https://claude-app.local/";
    private static final String ASSET = "claude.html";
    private static final int BG = 0xFF1A1A1A;

    private FrameLayout root;
    private WebView web;
    private ProgressBar progress;
    private View errorOverlay;
    private Dialog popupDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(BG);
            getWindow().setNavigationBarColor(BG);
        }

        CookieManager.getInstance().setAcceptCookie(true);

        root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        errorOverlay = buildErrorOverlay();
        errorOverlay.setVisibility(View.GONE);

        createWebView();
        root.addView(progress, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3), Gravity.TOP));
        root.addView(errorOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        loadApp();
    }

    // ---------------------------------------------------------------- WebView setup

    private void createWebView() {
        web = new WebView(this);
        web.setBackgroundColor(BG);
        configure(web);
        web.setWebViewClient(new AppWebViewClient());
        web.setWebChromeClient(new AppWebChromeClient());
        web.addJavascriptInterface(new AndroidBridge(this), "AndroidBridge");
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        root.addView(web, 0, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void configure(WebView w) {
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        String ua = s.getUserAgentString();
        if (ua != null) {
            s.setUserAgentString(ua.replace("; wv", ""));
        }
    }

    private void loadApp() {
        errorOverlay.setVisibility(View.GONE);
        try {
            String html = readAsset(ASSET);
            web.loadDataWithBaseURL(BASE_URL, html, "text/html", "UTF-8", null);
        } catch (IOException e) {
            showError("Could not read app files: " + e.getMessage());
        }
    }

    private String readAsset(String name) throws IOException {
        InputStream in = getAssets().open(name);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    // ---------------------------------------------------------------- clients

    private class AppWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progress.setVisibility(View.GONE);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                showError(null);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String host = uri.getHost();
            boolean internal = host == null
                    || host.equals("claude-app.local")
                    || host.equals("puter.com") || host.endsWith(".puter.com");
            if (internal) return false;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception ignored) { }
            return true;
        }

        @android.annotation.TargetApi(26)
        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            // Renderer died (low memory / crash). Rebuild instead of showing a blank view.
            root.removeView(web);
            web.destroy();
            createWebView();
            loadApp();
            return true;
        }
    }

    private class AppWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progress.setProgress(newProgress);
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            closePopup();

            final WebView popup = new WebView(MainActivity.this);
            popup.setBackgroundColor(BG);
            configure(popup);
            CookieManager.getInstance().setAcceptThirdPartyCookies(popup, true);

            final ProgressBar bar = new ProgressBar(MainActivity.this, null,
                    android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);

            popup.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                    if (r.isForMainFrame()) {
                        v.loadDataWithBaseURL(null,
                                "<body style='background:#1a1a1a;color:#eee;font-family:sans-serif;padding:32px'>"
                                        + "<h3>Couldn't load sign-in page</h3>"
                                        + "<p>Check your internet connection, close this window and try again.</p></body>",
                                "text/html", "UTF-8", null);
                    }
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                    return false;
                }
            });
            popup.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView v, int p) {
                    bar.setProgress(p);
                    bar.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
                }

                @Override
                public void onCloseWindow(WebView v) {
                    closePopup();
                }
            });

            FrameLayout frame = new FrameLayout(MainActivity.this);
            frame.setBackgroundColor(BG);
            frame.addView(popup, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            frame.addView(bar, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(3), Gravity.TOP));

            Button close = new Button(MainActivity.this);
            close.setText("\u2715");
            close.setTextColor(Color.WHITE);
            close.setBackgroundColor(0xAA000000);
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closePopup();
                }
            });
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(44), dp(44),
                    Gravity.TOP | Gravity.END);
            lp.setMargins(0, dp(8), dp(8), 0);
            frame.addView(close, lp);

            Dialog d = new Dialog(MainActivity.this, android.R.style.Theme_DeviceDefault_NoActionBar);
            d.setContentView(frame);
            d.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(android.content.DialogInterface dialog) {
                    popup.stopLoading();
                    popup.destroy();
                    if (popupDialog == dialog) popupDialog = null;
                }
            });
            popupDialog = d;
            d.show();

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(popup);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            closePopup();
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(message)
                    .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int w) { result.confirm(); }
                    })
                    .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                        @Override public void onCancel(android.content.DialogInterface d) { result.cancel(); }
                    })
                    .show();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(message)
                    .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int w) { result.confirm(); }
                    })
                    .setNegativeButton("Cancel", new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int w) { result.cancel(); }
                    })
                    .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                        @Override public void onCancel(android.content.DialogInterface d) { result.cancel(); }
                    })
                    .show();
            return true;
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
                                  final JsPromptResult result) {
            final EditText input = new EditText(MainActivity.this);
            input.setText(defaultValue);
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(message)
                    .setView(input)
                    .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int w) {
                            result.confirm(input.getText().toString());
                        }
                    })
                    .setNegativeButton("Cancel", new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int w) { result.cancel(); }
                    })
                    .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                        @Override public void onCancel(android.content.DialogInterface d) { result.cancel(); }
                    })
                    .show();
            return true;
        }
    }

    private void closePopup() {
        if (popupDialog != null) {
            Dialog d = popupDialog;
            popupDialog = null;
            try { d.dismiss(); } catch (Exception ignored) { }
        }
    }

    // ---------------------------------------------------------------- error overlay

    private void showError(String detail) {
        progress.setVisibility(View.GONE);
        errorOverlay.setVisibility(View.VISIBLE);
        TextView t = (TextView) errorOverlay.findViewWithTag("detail");
        if (t != null && detail != null) t.setText(detail);
    }

    private View buildErrorOverlay() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(32), 0, dp(32), 0);
        box.setBackgroundColor(BG);
        box.setClickable(true);

        TextView title = new TextView(this);
        title.setText("Something went wrong");
        title.setTextColor(0xFFE5E5E5);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView detail = new TextView(this);
        detail.setTag("detail");
        detail.setText("Claude AI needs a connection to reach the Puter servers.\nCheck your network and try again.");
        detail.setTextColor(0xFF9A9A9A);
        detail.setTextSize(14);
        detail.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, dp(20));
        box.addView(detail, lp);

        Button retry = new Button(this);
        retry.setText("Retry");
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadApp();
            }
        });
        box.addView(retry);
        return box;
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    public void onBackPressed() {
        if (popupDialog != null) {
            closePopup();
            return;
        }
        if (web != null && web.canGoBack()) {
            web.goBack();
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage("Exit Claude AI?")
                .setPositiveButton("Exit", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) { finish(); }
                })
                .setNegativeButton("Stay", null)
                .show();
    }

    @Override protected void onPause()  { super.onPause();  if (web != null) web.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }

    @Override
    protected void onDestroy() {
        closePopup();
        if (web != null) {
            ViewGroup parent = (ViewGroup) web.getParent();
            if (parent != null) parent.removeView(web);
            web.removeAllViews();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
