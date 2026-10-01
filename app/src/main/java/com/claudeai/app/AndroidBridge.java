package com.claudeai.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/** JS bridge: copy text, save files to Downloads, and print HTML (Save as PDF). */
public class AndroidBridge {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Activity activity;
    private WebView printView; // kept so the print WebView isn't garbage collected mid-job

    public AndroidBridge(Activity activity) {
        this.activity = activity;
    }

    private void toast(final String msg, final int length) {
        handler.post(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, msg, length).show();
            }
        });
    }

    @JavascriptInterface
    public void copyText(final String text) {
        handler.post(new Runnable() {
            @Override public void run() {
                try {
                    ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("text", text));
                    Toast.makeText(activity, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(activity, "Copy failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /** Text files (code). */
    @JavascriptInterface
    public void saveFile(final String filename, final String content) {
        try {
            saveBytes(filename, "text/plain", content.getBytes("UTF-8"));
        } catch (Exception e) {
            toast("Save failed: " + e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    /** Binary files (zip, docx, xlsx...) sent from JS as base64. */
    @JavascriptInterface
    public void saveBase64(final String filename, final String base64, final String mime) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    saveBytes(filename, mime, Base64.decode(base64, Base64.DEFAULT));
                } catch (Exception e) {
                    toast("Save failed: " + e.getMessage(), Toast.LENGTH_LONG);
                }
            }
        }).start();
    }

    private void saveBytes(String filename, String mime, byte[] data) throws Exception {
        String name = filename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[:*?\"<>|]", "_");
        if (name.isEmpty()) name = "claude-file";
        if (mime == null || mime.isEmpty()) mime = "application/octet-stream";

        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME, name);
            v.put(MediaStore.Downloads.MIME_TYPE, mime);
            v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new Exception("Could not create file in Downloads");
            OutputStream os = activity.getContentResolver().openOutputStream(uri);
            try {
                os.write(data);
            } finally {
                os.close();
            }
            toast("Saved to Downloads: " + name, Toast.LENGTH_LONG);
        } else {
            // Android 7-9: no permission-free public folder, so use the app's own folder.
            File dir = activity.getExternalFilesDir(null);
            if (dir == null) dir = activity.getFilesDir();
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, uniqueName(dir, name));
            FileOutputStream fos = new FileOutputStream(out);
            try {
                fos.write(data);
            } finally {
                fos.close();
            }
            toast("Saved: " + out.getAbsolutePath(), Toast.LENGTH_LONG);
        }
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

    /** Opens Android's print dialog for the HTML; choose "Save as PDF" as the printer. */
    @JavascriptInterface
    public void printHtml(final String title, final String html) {
        handler.post(new Runnable() {
            @Override public void run() {
                try {
                    final WebView wv = new WebView(activity);
                    wv.getSettings().setJavaScriptEnabled(false);
                    wv.setWebViewClient(new WebViewClient() {
                        @Override public void onPageFinished(WebView view, String url) {
                            PrintManager pm = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
                            PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(title);
                            pm.print(title, adapter, new PrintAttributes.Builder()
                                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
                            Toast.makeText(activity, "Choose \"Save as PDF\" as the printer", Toast.LENGTH_LONG).show();
                        }
                    });
                    printView = wv;
                    wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                } catch (Exception e) {
                    Toast.makeText(activity, "Print failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
