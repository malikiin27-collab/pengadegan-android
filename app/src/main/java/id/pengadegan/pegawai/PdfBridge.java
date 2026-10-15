package id.pengadegan.pegawai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Handles authenticated PPPK PDF downloads without relying on blob: URL support in WebView. */
final class PdfBridge {
    private static final int MAX_PDF_BYTES = 30 * 1024 * 1024;
    private final Activity activity;
    private final WebView web;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean working = new AtomicBoolean(false);
    private AlertDialog dialog;
    private TextView progressText;

    PdfBridge(Activity activity, WebView web) {
        this.activity = activity;
        this.web = web;
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(web, "PengadeganFiles", Collections.singleton(AppConfig.ORIGIN),
                (view, message, sourceOrigin, isMainFrame, replyProxy) -> {
                    if (!isMainFrame || !AppConfig.ORIGIN.equals(sourceOrigin.toString())) return;
                    try {
                        JSONObject data = new JSONObject(message.getData());
                        if (!"pppk_pdf".equals(data.optString("type"))) return;
                        Uri url = Uri.parse(data.optString("url", ""));
                        if (!safePdfUrl(url)) return;
                        boolean preview = data.optBoolean("preview", false);
                        activity.runOnUiThread(() -> request(url.toString(), preview));
                    } catch (Exception ignored) {
                        // Invalid messages do not trigger downloads.
                    }
                });
        }
    }

    private static boolean safePdfUrl(Uri url) {
        if (!AppConfig.trusted(url)) return false;
        if (!"/pppk_kinerja_pdf.php".equals(url.getPath())) return false;
        try { return Integer.parseInt(url.getQueryParameter("id")) > 0; }
        catch (Exception e) { return false; }
    }

    void inject() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return;
        // Window capture handler runs before the existing document-level PDF progress handler.
        String script = "(function(){"
            + "if(window.__pengadeganPdfNative16)return;window.__pengadeganPdfNative16=true;"
            + "window.addEventListener('click',function(ev){"
            + "var target=ev.target; if(!(target instanceof Element))return;"
            + "var link=target.closest('a[href]');if(!link)return;"
            + "var u;try{u=new URL(link.href,location.href);}catch(e){return;}"
            + "if(u.origin!==location.origin||u.pathname!=='/pppk_kinerja_pdf.php'||!/^\\d+$/.test(u.searchParams.get('id')||''))return;"
            + "if(!window.PengadeganFiles||!window.PengadeganFiles.postMessage)return;"
            + "ev.preventDefault();ev.stopImmediatePropagation();"
            + "var preview=u.searchParams.get('view')==='1'||link.target==='_blank';"
            + "window.PengadeganFiles.postMessage(JSON.stringify({type:'pppk_pdf',url:u.href,preview:preview}));"
            + "},true);"
            + "})();";
        web.evaluateJavascript(script, null);
    }

    private void show(String title, String state) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        progressText = new TextView(activity);
        int pad = (int) (20 * activity.getResources().getDisplayMetrics().density);
        progressText.setPadding(pad, pad, pad, pad);
        progressText.setText(state);
        dialog = new AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(progressText)
            .setCancelable(false)
            .create();
        dialog.show();
    }

    private void status(String state) {
        activity.runOnUiThread(() -> {
            if (progressText != null && !activity.isFinishing() && !activity.isDestroyed())
                progressText.setText(state);
        });
    }

    private void dismiss() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        progressText = null;
        dialog = null;
    }

    private void request(String url, boolean preview) {
        if (!working.compareAndSet(false, true)) {
            Toast.makeText(activity, "Tunggu proses PDF sebelumnya selesai.", Toast.LENGTH_SHORT).show();
            return;
        }
        String cookies = CookieManager.getInstance().getCookie(AppConfig.ORIGIN);
        String userAgent = web.getSettings().getUserAgentString();
        show(preview ? "Memuat PDF PPPK" : "Mengunduh PDF PPPK",
            "Menghubungi server Pengadegan...\nProgres menunggu respons server.");
        worker.execute(() -> {
            try {
                byte[] bytes = fetch(url, cookies, userAgent);
                String name = "Laporan-PPPK-Pengadegan-" + Uri.parse(url).getQueryParameter("id") + ".pdf";
                if (preview) {
                    File dir = new File(activity.getCacheDir(), "pdf");
                    if (!dir.isDirectory() && !dir.mkdirs()) throw new Exception("Folder PDF tidak tersedia.");
                    File temp = new File(dir, name);
                    try (OutputStream out = new FileOutputStream(temp)) { out.write(bytes); }
                    activity.runOnUiThread(() -> {
                        dismiss();
                        try {
                            Uri uri = FileProvider.getUriForFile(activity,
                                activity.getPackageName() + ".fileprovider", temp);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(uri, "application/pdf");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            activity.startActivity(intent);
                        } catch (ActivityNotFoundException notFound) {
                            new AlertDialog.Builder(activity).setTitle("Pembaca PDF belum tersedia")
                                .setMessage("Instal aplikasi pembaca PDF pada perangkat untuk melihat dokumen. Anda juga dapat memilih Unduh PDF.")
                                .setPositiveButton("Tutup", null).show();
                        } catch (Exception e) { noticeFailure("Dokumen belum dapat dibuka: " + e.getMessage()); }
                    });
                } else {
                    savePdf(bytes, name);
                    activity.runOnUiThread(() -> {
                        dismiss();
                        new AlertDialog.Builder(activity).setTitle("Unduhan berhasil")
                            .setMessage(Build.VERSION.SDK_INT >= 29 ? "PDF tersimpan di Unduhan/Pengadegan." : "PDF tersimpan pada penyimpanan aplikasi Pengadegan.")
                            .setPositiveButton("Selesai", null).show();
                    });
                }
            } catch (Exception err) {
                activity.runOnUiThread(() -> {
                    dismiss();
                    noticeFailure("PDF belum dapat diproses. " + err.getMessage());
                });
            } finally {
                working.set(false);
            }
        });
    }

    private byte[] fetch(String address, String cookie, String agent) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(70000);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("Accept", "application/pdf");
        conn.setRequestProperty("User-Agent", agent);
        if (cookie != null && !cookie.isEmpty()) conn.setRequestProperty("Cookie", cookie);
        try {
            int code = conn.getResponseCode();
            if (code != 200) throw new Exception(code == 401 || code == 403 || code == 302
                ? "Sesi login berakhir. Masuk ulang kemudian coba kembali."
                : "Server merespons HTTP " + code + ".");
            int total = conn.getContentLength();
            if (total > MAX_PDF_BYTES) throw new Exception("Ukuran PDF melebihi batas 30 MB.");
            status("Server menyiapkan berkas PDF...");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream in = conn.getInputStream()) {
                byte[] chunk = new byte[8192];
                int n, loaded = 0, lastShown = -1;
                while ((n = in.read(chunk)) != -1) {
                    loaded += n;
                    if (loaded > MAX_PDF_BYTES) throw new Exception("PDF melebihi batas 30 MB.");
                    buffer.write(chunk, 0, n);
                    if (total > 0) {
                        int pct = Math.min(99, (int) ((100L * loaded) / total));
                        if (pct >= lastShown + 5) {
                            lastShown = pct;
                            status("Menerima berkas dari server: " + pct + "%");
                        }
                    } else if (loaded % (256 * 1024) < n) {
                        status(String.format(Locale.US, "Menerima berkas PDF: %.1f MB", loaded / 1048576.0));
                    }
                }
            }
            byte[] bytes = buffer.toByteArray();
            if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D'
                || bytes[3] != 'F' || bytes[4] != '-')
                throw new Exception("Server tidak mengirimkan PDF yang valid. Periksa foto atau mesin PDF di cPanel.");
            status("Berkas berhasil diterima. Menyimpan hasil...");
            return bytes;
        } finally {
            conn.disconnect();
        }
    }

    private void savePdf(byte[] bytes, String filename) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Pengadegan");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri dest = activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (dest == null) throw new Exception("Gagal menyiapkan folder Unduhan.");
            boolean done = false;
            try {
                try (OutputStream out = activity.getContentResolver().openOutputStream(dest)) {
                    if (out == null) throw new Exception("Berkas tidak dapat ditulis.");
                    out.write(bytes);
                }
                values.clear();
                values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                activity.getContentResolver().update(dest, values, null, null);
                done = true;
            } finally {
                if (!done) activity.getContentResolver().delete(dest, null, null);
            }
        } else {
            // Android 8/9: use app-specific external Downloads, no broad storage permission.
            File dir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null || (!dir.isDirectory() && !dir.mkdirs()))
                throw new Exception("Penyimpanan perangkat tidak tersedia.");
            try (OutputStream out = new FileOutputStream(new File(dir, filename))) { out.write(bytes); }
        }
    }

    private void noticeFailure(String message) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        new AlertDialog.Builder(activity).setTitle("Proses PDF gagal")
            .setMessage(message).setPositiveButton("Tutup", null).show();
    }

    void close() {
        worker.shutdownNow();
        dismiss();
    }
}
