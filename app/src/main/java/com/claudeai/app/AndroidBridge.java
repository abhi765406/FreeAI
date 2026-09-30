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

/** JS bridge: copy code to clipboard and save code files from the chat UI. */
public class AndroidBridge {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Activity activity;

    public AndroidBridge(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void copyText(final String text) {
        handler.post(new Runnable() {
            @Override public void run() {
                try {
                    ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("code", text));
                    Toast.makeText(activity, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(activity, "Copy failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @JavascriptInterface
    public void saveFile(final String filename, final String content) {
        handler.post(new Runnable() {
            @Override public void run() {
                try {
                    File dir = activity.getExternalFilesDir(null);
                    if (dir == null) dir = activity.getFilesDir();
                    if (!dir.exists()) dir.mkdirs();
                    File out = new File(dir, uniqueName(dir, filename));
                    FileOutputStream fos = new FileOutputStream(out);
                    fos.write(content.getBytes("UTF-8"));
                    fos.close();
                    Toast.makeText(activity, "Saved: " + out.getAbsolutePath(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(activity, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
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
