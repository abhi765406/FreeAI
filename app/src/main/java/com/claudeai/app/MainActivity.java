package com.claudeai.app;

import android.app.*;import android.os.*;import android.graphics.Color;import android.net.Uri;import android.view.*;import android.webkit.*;import android.widget.*;import android.content.*;
import androidx.appcompat.app.AppCompatActivity;import androidx.webkit.WebSettingsCompat;import androidx.webkit.WebViewFeature;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(Color.rgb(26,26,26));
        webView=new WebView(this); setContentView(webView); configure(webView); webView.loadUrl("https://appassets.androidplatform.net/assets/claude.html"); }
    private void configure(WebView w){
        WebSettings s=w.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true); s.setJavaScriptCanOpenWindowsAutomatically(true); s.setSupportMultipleWindows(true); s.setAllowFileAccess(false); s.setAllowContentAccess(true); s.setMediaPlaybackRequiresUserGesture(false); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false); s.setUserAgentString(s.getUserAgentString()+" ClaudeAI-Android/1.1");
        if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) WebViewSettingsCompatHelper.forceDark(w);
        CookieManager cm=CookieManager.getInstance(); cm.setAcceptCookie(true); if(Build.VERSION.SDK_INT>=21) cm.setAcceptThirdPartyCookies(w,true);
        w.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r){String u=r.getUrl().toString(); if(u.startsWith("http://")||u.startsWith("https://")){ if(u.contains("puter.com")||u.contains("auth")||u.contains("oauth")){v.loadUrl(u);return true;} try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}catch(Exception ignored){} return true;} return false;}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){super.onReceivedError(v,r,e); if(r.isForMainFrame()) Toast.makeText(MainActivity.this,"Page failed to load: "+e.getDescription(),Toast.LENGTH_LONG).show();}
        });
        w.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onCreateWindow(WebView parent, boolean dialog, boolean userGesture, Message resultMsg){
                final WebView popup=new WebView(MainActivity.this); configure(popup); popup.setWebChromeClient(new WebChromeClient(){@Override public void onCloseWindow(WebView v){((ViewGroup)v.getParent()).removeView(v);}});
                final Dialog d=new Dialog(MainActivity.this); d.setTitle("Sign in"); d.setContentView(popup); Window win=d.getWindow(); if(win!=null)win.setLayout(-1,-1); d.setOnDismissListener(x->{popup.destroy();}); d.show(); if(win!=null)win.setLayout(-1,-1);
                ((WebView.WebViewTransport)resultMsg.obj).setWebView(popup); resultMsg.sendToTarget(); return true;
            }
            @Override public boolean onJsAlert(WebView v,String url,String msg,JsResult r){new AlertDialog.Builder(MainActivity.this).setMessage(msg).setPositiveButton("OK",(d,w)->r.confirm()).setOnCancelListener(d->r.cancel()).show();return true;}
        });
    }
    @Override public void onBackPressed(){if(webView.canGoBack())webView.goBack();else super.onBackPressed();}
}
class WebViewSettingsCompatHelper { static void forceDark(WebView w){ try{WebSettingsCompat.setForceDark(w.getSettings(),WebSettingsCompat.FORCE_DARK_OFF);}catch(Exception ignored){} } }
