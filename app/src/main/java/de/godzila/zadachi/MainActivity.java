package de.godzila.zadachi;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;
    private boolean askedNotif = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sp = getSharedPreferences("ui", MODE_PRIVATE);
        if (sp.contains("bg")) paint(sp.getInt("bg", 0), sp.getBoolean("dark", false));
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);   // localStorage: задачи хранятся на телефоне
        s.setAllowFileAccess(true);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setBackgroundColor(sp.contains("bg") ? sp.getInt("bg", 0) : getResources().getColor(R.color.bg, getTheme()));
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    private void paint(int c, boolean dark) {
        Window w = getWindow();
        w.getDecorView().setBackgroundColor(c);
        w.setStatusBarColor(c);
        w.setNavigationBarColor(c);
        if (web != null) web.setBackgroundColor(c);
        View d = w.getDecorView();
        int f = d.getSystemUiVisibility();
        int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        d.setSystemUiVisibility(dark ? (f & ~light) : (f | light));
    }

    class Bridge {
        @JavascriptInterface
        public void setTheme(final String bg, final boolean dark) {
            final int c = Color.parseColor(bg);
            getSharedPreferences("ui", MODE_PRIVATE).edit().putInt("bg", c).putBoolean("dark", dark).apply();
            runOnUiThread(() -> paint(c, dark));
        }
        @JavascriptInterface
        public void syncReminders(final String json) {
            Reminders.sync(getApplicationContext(), json);
            if (!askedNotif && json.length() > 2 && Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                askedNotif = true;
                runOnUiThread(() -> requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1));
            }
        }
        @JavascriptInterface
        public void setDark(final boolean dark) {
            setTheme(dark ? "#0F0F0E" : "#F3F1EC", dark);
        }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.onAndroidBack && window.onAndroidBack()", v -> {
            if (!"true".equals(v)) MainActivity.super.onBackPressed();
        });
    }
}
