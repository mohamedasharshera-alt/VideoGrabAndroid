package com.example.videograb;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.webkit.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    WebView web;
    EditText address;
    LinearLayout foundList, downloadsList;
    TextView status;
    Set<String> found = new LinkedHashSet<>();
    DownloadManager manager;
    static final String[] EXT = {".mp4", ".webm", ".mov", ".m4v", ".3gp"};
    int blue = Color.rgb(26, 86, 210);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        manager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setPadding(12, 12, 12, 8); root.setBackgroundColor(Color.WHITE);
        address = new EditText(this); address.setSingleLine(true); address.setHint("رابط الموقع أو الفيديو"); address.setTextSize(15);
        root.addView(address, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout actions = new LinearLayout(this);
        Button go = btn("فتح الموقع"); Button direct = btn("تحميل الرابط"); Button refresh = btn("التحميلات");
        actions.addView(go, new LinearLayout.LayoutParams(0, -2, 1)); actions.addView(direct, new LinearLayout.LayoutParams(0, -2, 1)); actions.addView(refresh, new LinearLayout.LayoutParams(0, -2, 1)); root.addView(actions);
        status = new TextView(this); status.setText("افتح صفحة فيديو أو الصق رابطًا مباشرًا"); status.setTextColor(Color.DKGRAY); status.setPadding(6, 8, 6, 8); root.addView(status);
        web = new WebView(this); web.getSettings().setJavaScriptEnabled(true); web.getSettings().setDomStorageEnabled(true); web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView v, String url, android.graphics.Bitmap favicon) { found.clear(); foundList.removeAllViews(); address.setText(url); status.setText("جارٍ فتح الصفحة..."); }
            @Override public void onPageFinished(WebView v, String url) { status.setText("الصفحة جاهزة — شغّل الفيديو للكشف عن رابطه"); scanPage(); }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (isMedia(url)) runOnUiThread(() -> addFound(url));
                return null;
            }
        });
        web.setDownloadListener((url, userAgent, disposition, mime, size) -> addFound(url));
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView heading = new TextView(this); heading.setText("الفيديوهات المكتشفة"); heading.setTextSize(17); heading.setTextColor(blue); root.addView(heading);
        ScrollView foundScroll = new ScrollView(this); foundList = new LinearLayout(this); foundList.setOrientation(1); foundScroll.addView(foundList); root.addView(foundScroll, new LinearLayout.LayoutParams(-1, dp(115)));
        TextView heading2 = new TextView(this); heading2.setText("قائمة التحميلات"); heading2.setTextSize(17); heading2.setTextColor(blue); root.addView(heading2);
        ScrollView dlScroll = new ScrollView(this); downloadsList = new LinearLayout(this); downloadsList.setOrientation(1); dlScroll.addView(downloadsList); root.addView(dlScroll, new LinearLayout.LayoutParams(-1, dp(125)));
        setContentView(root);
        go.setOnClickListener(v -> { String u = normalize(address.getText().toString()); if (u != null) web.loadUrl(u); else toast("الرابط غير صالح"); });
        direct.setOnClickListener(v -> { String u = normalize(address.getText().toString()); if (u == null) { toast("الرابط غير صالح"); return; } if (isMedia(u)) download(u); else web.loadUrl(u); });
        refresh.setOnClickListener(v -> updateDownloads());
        web.loadUrl("https://www.google.com");
    }
    Button btn(String s) { Button b = new Button(this); b.setText(s); b.setTextSize(11); b.setAllCaps(false); return b; }
    int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density); }
    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    String normalize(String s) { s = s.trim(); if (s.isEmpty()) return null; if (!s.contains("://")) s = "https://" + s; Uri u = Uri.parse(s); return ("https".equalsIgnoreCase(u.getScheme()) && u.getHost()!=null) ? s : null; }
    boolean isMedia(String u) { try { String path = Uri.parse(u).getPath().toLowerCase(Locale.ROOT); for(String ext:EXT) if(path.endsWith(ext)) return true; } catch(Exception ignored) {} return false; }
    void scanPage() {
        web.evaluateJavascript("(function(){var a=[];document.querySelectorAll('video,video source').forEach(function(e){if(e.src)a.push(e.src);if(e.currentSrc)a.push(e.currentSrc)});return JSON.stringify(a)})()", result -> {
            try { String inner = new org.json.JSONTokener(result).nextValue().toString(); org.json.JSONArray arr = new org.json.JSONArray(inner); for(int i=0;i<arr.length();i++){String u=arr.optString(i);if(u.startsWith("https://") && isMedia(u))addFound(u);} } catch(Exception ignored){}
        });
    }
    void addFound(String u) {
        if (!u.startsWith("https://") || !isMedia(u) || !found.add(u)) return;
        LinearLayout row = new LinearLayout(this); row.setOrientation(1);
        TextView label = new TextView(this); label.setText(Uri.decode(Uri.parse(u).getLastPathSegment())); label.setMaxLines(2); row.addView(label);
        Button b = btn("تحميل الفيديو"); b.setOnClickListener(v -> download(u)); row.addView(b); foundList.addView(row);
        status.setText("تم اكتشاف " + found.size() + " رابط فيديو");
    }
    void download(String u) {
        try {
            Uri uri = Uri.parse(u); String filename = uri.getLastPathSegment();
            if (filename == null || filename.trim().isEmpty()) filename = "video_" + System.currentTimeMillis() + ".mp4";
            filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            DownloadManager.Request r = new DownloadManager.Request(uri);
            r.setTitle(filename); r.setDescription("VideoGrab"); r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            String cookie = CookieManager.getInstance().getCookie(u); if (cookie != null) r.addRequestHeader("Cookie", cookie);
            r.addRequestHeader("User-Agent", web.getSettings().getUserAgentString());
            String referer = web.getUrl(); if (referer != null && referer.startsWith("https://")) r.addRequestHeader("Referer", referer);
            long id = manager.enqueue(r); getPreferences(0).edit().putLong("d_"+id,id).apply(); toast("بدأ التحميل"); updateDownloads();
        } catch(Exception e) { toast("تعذر بدء التحميل: " + e.getMessage()); }
    }
    void updateDownloads() {
        downloadsList.removeAllViews();
        for(String key:getPreferences(0).getAll().keySet()) {
            if(!key.startsWith("d_"))continue;
            long id = getPreferences(0).getLong(key,-1);
            try (android.database.Cursor c = manager.query(new DownloadManager.Query().setFilterById(id))) {
                if (!c.moveToFirst()) continue;
                String title = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE));
                int state = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                long bytes = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                long total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                String progress = total > 0 ? (bytes*100/total)+"%" : (bytes/1024)+" KB";
                TextView item = new TextView(this); item.setText(title + " — " + (state==DownloadManager.STATUS_SUCCESSFUL?"اكتمل":state==DownloadManager.STATUS_FAILED?"فشل":progress)); item.setPadding(4,5,4,5); downloadsList.addView(item);
                if(state!=DownloadManager.STATUS_SUCCESSFUL && state!=DownloadManager.STATUS_FAILED) {
                    Button cancel=btn("إلغاء: " + title); cancel.setOnClickListener(v->{manager.remove(id);getPreferences(0).edit().remove(key).apply();updateDownloads();}); downloadsList.addView(cancel);
                }
            } catch(Exception ignored){}
        }
    }
    @Override public void onBackPressed() { if(web.canGoBack())web.goBack();else super.onBackPressed(); }
    @Override protected void onDestroy() { web.destroy(); super.onDestroy(); }
}
