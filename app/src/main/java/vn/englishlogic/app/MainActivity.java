package vn.englishlogic.app;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String APP_URL = "https://english-logic-v0.nguyenphuong230301.chatgpt.site";
    private static final String APP_HOST = "english-logic-v0.nguyenphuong230301.chatgpt.site";
    private static final int FILE_CHOOSER_REQUEST = 501;
    private static final int MICROPHONE_REQUEST = 502;

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingWebPermission;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(7, 17, 31));
        window.setNavigationBarColor(Color.rgb(7, 17, 31));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(7, 17, 31));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        setContentView(webView);

        configureWebView();
        if (state == null) webView.loadUrl(APP_URL);
        else webView.restoreState(state);
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.setSafeBrowsingEnabled(true);

        webView.addJavascriptInterface(new AndroidBridge(), "EnglishLogicAndroid");
        webView.setWebViewClient(new EnglishLogicClient());
        webView.setWebChromeClient(new EnglishLogicChromeClient());
        webView.setDownloadListener((url, userAgent, disposition, mimeType, size) -> {
            if (url != null && url.startsWith("blob:")) {
                exportBlob(url, fileNameFromDisposition(disposition));
            } else if (url != null) {
                openExternal(Uri.parse(url));
            }
        });
    }

    private boolean isTrustedInAppHost(Uri uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
        String host = uri.getHost();
        if (host == null) return false;
        host = host.toLowerCase(Locale.ROOT);
        return host.equals(APP_HOST) || host.equals("chatgpt.com") || host.endsWith(".chatgpt.com") || host.equals("openai.com") || host.endsWith(".openai.com");
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Không tìm thấy ứng dụng để mở liên kết.", Toast.LENGTH_SHORT).show();
        }
    }

    private String fileNameFromDisposition(String disposition) {
        if (disposition != null) {
            int marker = disposition.indexOf("filename=");
            if (marker >= 0) return disposition.substring(marker + 9).replace("\"", "").trim();
        }
        return "english-logic-progress.json";
    }

    private void exportBlob(String blobUrl, String fileName) {
        String script = "(async()=>{try{" +
            "const r=await fetch(" + quoteJs(blobUrl) + ");const b=await r.blob();" +
            "const reader=new FileReader();reader.onloadend=()=>EnglishLogicAndroid.saveBase64(" + quoteJs(fileName) + ",b.type||'application/json',reader.result.split(',')[1]);reader.readAsDataURL(b);" +
            "}catch(e){EnglishLogicAndroid.showMessage('Không thể xuất tệp.')}})();";
        webView.evaluateJavascript(script, null);
    }

    private String quoteJs(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n") + "'";
    }

    private String offlinePage() {
        return "<!doctype html><html><meta name='viewport' content='width=device-width,initial-scale=1'><body style='margin:0;background:#07111f;color:#fff;font-family:sans-serif;display:grid;place-items:center;min-height:100vh'><main style='max-width:420px;padding:28px;text-align:center'><div style='width:64px;height:64px;display:grid;place-items:center;margin:auto;border:2px solid #8bc832;border-radius:18px;background:#06351d;font-weight:800'>EL</div><h1>Chưa thể kết nối</h1><p style='color:#a9bdcc;line-height:1.6'>Hãy kiểm tra Internet rồi thử lại. Dữ liệu học đã lưu trên máy không bị mất.</p><a href='" + APP_URL + "' style='display:inline-block;margin-top:10px;padding:13px 18px;border-radius:12px;background:#c9ff65;color:#07111f;text-decoration:none;font-weight:800'>Thử lại</a></main></body></html>";
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        webView.saveState(state);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) result = new Uri[]{data.getData()};
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == MICROPHONE_REQUEST && pendingWebPermission != null) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) pendingWebPermission.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
            else pendingWebPermission.deny();
            pendingWebPermission = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("EnglishLogicAndroid");
            webView.destroy();
        }
        super.onDestroy();
    }

    private final class EnglishLogicClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (isTrustedInAppHost(uri)) return false;
            openExternal(uri);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) view.loadDataWithBaseURL(APP_URL, offlinePage(), "text/html", "UTF-8", null);
        }
    }

    private final class EnglishLogicChromeClient extends WebChromeClient {
        @Override
        public void onPermissionRequest(PermissionRequest request) {
            if (!APP_HOST.equalsIgnoreCase(request.getOrigin().getHost())) {
                request.deny();
                return;
            }
            boolean asksForAudio = false;
            for (String resource : request.getResources()) if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) asksForAudio = true;
            if (!asksForAudio) {
                request.deny();
                return;
            }
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
            else {
                pendingWebPermission = request;
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_REQUEST);
            }
        }

        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = callback;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            try {
                startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                return true;
            } catch (ActivityNotFoundException error) {
                fileCallback = null;
                return false;
            }
        }
    }

    public final class AndroidBridge {
        @JavascriptInterface
        public void showMessage(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void saveBase64(String fileName, String mimeType, String payload) {
            try {
                byte[] bytes = Base64.decode(payload, Base64.DEFAULT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) throw new IllegalStateException("Cannot create download");
                    try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                        if (output == null) throw new IllegalStateException("Cannot open download");
                        output.write(bytes);
                    }
                } else {
                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
                    try (FileOutputStream output = new FileOutputStream(file)) { output.write(bytes); }
                }
                showMessage("Đã lưu dữ liệu vào thư mục Tải xuống.");
            } catch (Exception error) {
                showMessage("Không thể lưu tệp dữ liệu.");
            }
        }
    }
}
