package com.claudeai.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Bridge exposed to the WebView as window.AndroidBridge.
 * Lets the page copy code and save artifact files without
 * WebView clipboard/blob limitations.
 */
public class AndroidBridge {
    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public AndroidBridge(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void copyText(final String text) {
        handler.post(() -> {
            try {
                ClipboardManager cm = (ClipboardManager)
                        activity.getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("code", text));
                Toast.makeText(activity, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(activity, "Copy failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @JavascriptInterface
    public void saveFile(final String name, final String content) {
        handler.post(() -> {
            try {
                // getExternalFilesDir needs NO permission on any Android version
                File dir = activity.getExternalFilesDir(null);
                if (dir == null) dir = activity.getFilesDir();
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, uniqueName(dir, name));
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(content.getBytes("UTF-8"));
                fos.close();
                Toast.makeText(activity, "Saved: " + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(activity, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String uniqueName(File dir, String name) {
        if (!new File(dir, name).exists()) return name;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int i = 1; i < 100; i++) {
            String candidate = base + " (" + i + ")" + ext;
            if (!new File(dir, candidate).exists()) return candidate;
        }
        return System.currentTimeMillis() + "_" + name;
    }
}
