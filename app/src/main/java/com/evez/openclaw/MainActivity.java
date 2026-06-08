package com.evez.openclaw;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.app.AlertDialog;

public class MainActivity extends Activity {

    private WebView webView;
    private String gatewayUrl;
    private static final String PREFS = "openclaw_prefs";
    private static final String KEY_URL = "gateway_url";
    private static final String DEFAULT_URL = "http://localhost:18789";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        gatewayUrl = prefs.getString(KEY_URL, DEFAULT_URL);

        webView = new WebView(this);
        setupWebView();
        setContentView(webView);

        // Check connectivity and load
        loadGateway();
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " EVEZOpenClaw/1.0");
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // Enable WebSocket support
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) {
                    showConnectionDialog();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://localhost") || url.startsWith("http://127.0.0.1") ||
                    url.startsWith("http://192.168") || url.startsWith("http://10.") ||
                    url.contains("openclaw") || url.startsWith("https://")) {
                    return false; // Load in WebView
                }
                // External links open in browser
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        // JavaScript interface for native features
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void setGatewayUrl(String url) {
                SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                prefs.edit().putString(KEY_URL, url).apply();
                gatewayUrl = url;
                runOnUiThread(() -> loadGateway());
            }

            @android.webkit.JavascriptInterface
            public String getGatewayUrl() {
                return gatewayUrl;
            }

            @android.webkit.JavascriptInterface
            public void showSettings() {
                runOnUiThread(() -> showConnectionDialog());
            }
        }, "EVEZBridge");
    }

    private void loadGateway() {
        webView.loadUrl(gatewayUrl);
    }

    private void showConnectionDialog() {
        runOnUiThread(() -> {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 40, 50, 10);

            EditText input = new EditText(this);
            input.setHint("Gateway URL (e.g. http://192.168.1.100:18789)");
            input.setText(gatewayUrl);
            input.setSelectAllOnFocus(true);
            layout.addView(input);

            new AlertDialog.Builder(this)
                .setTitle("EVEZ OpenClaw Gateway")
                .setMessage("Enter your OpenClaw gateway URL.\n\nFor local: http://localhost:18789\nFor network: http://YOUR_IP:18789\nFor cloud: https://your-deploy.fly.dev")
                .setView(layout)
                .setPositiveButton("Connect", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                        prefs.edit().putString(KEY_URL, url).apply();
                        gatewayUrl = url;
                        loadGateway();
                    }
                })
                .setNeutralButton("Fly.io Deploy", (dialog, which) -> {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/EvezArt/evez-openclaw-deploy")));
                })
                .setCancelable(false)
                .show();
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            showConnectionDialog();
        }
    }
}
