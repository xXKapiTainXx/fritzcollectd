package com.threedpilot.mobile;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String PREFS = "3d_pilot";
    private static final String KEY_SERVER = "server_ip";
    private static final int DEFAULT_PORT = 17888;

    private LinearLayout startPanel;
    private EditText serverEdit;
    private TextView statusText;
    private FrameLayout webContainer;
    private WebView webView;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            buildInterface();
            prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            serverEdit.setText(prefs.getString(KEY_SERVER, ""));
            initWebViewSafe();
            updateVpnStatus();
        } catch (Throwable error) {
            showEmergencyScreen(error);
        }
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        startPanel = new LinearLayout(this);
        startPanel.setOrientation(LinearLayout.VERTICAL);
        startPanel.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(startPanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("3D Pilot");
        title.setTextSize(30f);
        title.setTextColor(Color.rgb(25, 25, 25));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = dp(12);
        startPanel.addView(title, titleParams);

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.threedpilot.mobile.R.drawable.app_icon);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(170), dp(170));
        logoParams.bottomMargin = dp(20);
        startPanel.addView(logo, logoParams);

        TextView serverLabel = new TextView(this);
        serverLabel.setText("Server");
        serverLabel.setTextSize(17f);
        serverLabel.setTextColor(Color.rgb(35, 35, 35));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.bottomMargin = dp(6);
        startPanel.addView(serverLabel, labelParams);

        serverEdit = new EditText(this);
        serverEdit.setSingleLine(true);
        serverEdit.setHint("IP-Adresse");
        serverEdit.setTextSize(17f);
        serverEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_URI);
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        editParams.bottomMargin = dp(12);
        startPanel.addView(serverEdit, editParams);

        Button connectButton = new Button(this);
        connectButton.setText("Verbinden");
        connectButton.setAllCaps(false);
        connectButton.setTextSize(17f);
        connectButton.setOnClickListener(v -> connectToServer());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        buttonParams.bottomMargin = dp(12);
        startPanel.addView(connectButton, buttonParams);

        statusText = new TextView(this);
        statusText.setTextSize(15f);
        statusText.setTextColor(Color.rgb(70, 70, 70));
        statusText.setGravity(Gravity.CENTER);
        startPanel.addView(statusText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        webContainer = new FrameLayout(this);
        webContainer.setVisibility(View.GONE);
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(webContainer, webParams);
    }

    private void initWebViewSafe() {
        try {
            webView = new WebView(this);
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setLoadsImagesAutomatically(true);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(false);

            CookieManager cookies = CookieManager.getInstance();
            cookies.setAcceptCookie(true);
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                cookies.setAcceptThirdPartyCookies(webView, true);
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return false;
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    return false;
                }

                @Override
                public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    if (!isVpnActive()) {
                        try { view.stopLoading(); } catch (Throwable ignored) { }
                        showStartPanel("VPN nicht aktiv. Verbindung wurde gesperrt.");
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                }
            });

            webContainer.addView(webView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (Throwable error) {
            webView = null;
            setStatus("Android System WebView ist nicht verfügbar. Bitte Chrome/WebView aktualisieren.");
        }
    }

    private void connectToServer() {
        try {
            hideKeyboard();
            if (!isVpnActive()) {
                setStatus("VPN nicht aktiv. Bitte zuerst VPN verbinden.");
                return;
            }

            String raw = serverEdit.getText().toString().trim();
            if (raw.length() == 0) {
                setStatus("Bitte Server-IP eingeben.");
                return;
            }

            if (webView == null) {
                initWebViewSafe();
                if (webView == null) {
                    return;
                }
            }

            String loginUrl = buildLoginUrl(raw);
            prefs.edit().putString(KEY_SERVER, raw).apply();
            statusText.setText("");
            startPanel.setVisibility(View.GONE);
            webContainer.setVisibility(View.VISIBLE);
            webView.loadUrl(loginUrl);
        } catch (Throwable error) {
            showStartPanel("Verbindung konnte nicht geöffnet werden: " + safeMessage(error));
        }
    }

    private String buildLoginUrl(String raw) {
        String value = raw.trim();
        if (!value.contains("://")) {
            value = "http://" + value;
        }

        Uri parsed = Uri.parse(value);
        String scheme = parsed.getScheme();
        String host = parsed.getHost();
        int port = parsed.getPort();

        if (scheme == null || scheme.length() == 0) {
            scheme = "http";
        }
        if (host == null || host.length() == 0) {
            throw new IllegalArgumentException("Ungültige Server-IP");
        }
        if (port < 0) {
            port = DEFAULT_PORT;
        }
        return scheme + "://" + host + ":" + port + "/login";
    }

    private boolean isVpnActive() {
        try {
            ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) return false;
            Network[] networks = manager.getAllNetworks();
            if (networks == null) return false;
            for (Network network : networks) {
                NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
                if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private void updateVpnStatus() {
        if (isVpnActive()) {
            setStatus("VPN aktiv");
        } else {
            setStatus("VPN nicht aktiv");
        }
    }

    private void showStartPanel(String message) {
        try {
            if (webView != null) webView.stopLoading();
        } catch (Throwable ignored) { }
        if (webContainer != null) webContainer.setVisibility(View.GONE);
        if (startPanel != null) startPanel.setVisibility(View.VISIBLE);
        setStatus(message);
    }

    private void hideKeyboard() {
        try {
            serverEdit.clearFocus();
            InputMethodManager input = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (input != null) input.hideSoftInputFromWindow(serverEdit.getWindowToken(), 0);
        } catch (Throwable ignored) { }
    }

    private void setStatus(String message) {
        if (statusText != null) statusText.setText(message == null ? "" : message);
    }

    private void showEmergencyScreen(Throwable error) {
        try {
            LinearLayout emergency = new LinearLayout(this);
            emergency.setOrientation(LinearLayout.VERTICAL);
            emergency.setGravity(Gravity.CENTER);
            emergency.setPadding(dp(24), dp(24), dp(24), dp(24));
            emergency.setBackgroundColor(Color.WHITE);
            TextView text = new TextView(this);
            text.setText("3D Pilot\n\nStartfehler: " + safeMessage(error));
            text.setGravity(Gravity.CENTER);
            text.setTextColor(Color.BLACK);
            text.setTextSize(18f);
            emergency.addView(text, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            setContentView(emergency);
        } catch (Throwable ignored) {
        }
    }

    private String safeMessage(Throwable error) {
        if (error == null) return "Unbekannter Fehler";
        String message = error.getMessage();
        if (message == null || message.length() == 0) return error.getClass().getSimpleName();
        return message;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (webContainer != null && webContainer.getVisibility() == View.VISIBLE && !isVpnActive()) {
                showStartPanel("VPN nicht aktiv. Verbindung wurde gesperrt.");
            } else if (startPanel != null && startPanel.getVisibility() == View.VISIBLE) {
                updateVpnStatus();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (webContainer != null && webContainer.getVisibility() == View.VISIBLE) {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    showStartPanel(isVpnActive() ? "VPN aktiv" : "VPN nicht aktiv");
                }
                return;
            }
        } catch (Throwable ignored) {
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        try {
            if (webView != null) {
                webView.stopLoading();
                webView.destroy();
            }
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }
}
