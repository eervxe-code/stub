package com.braymi.browser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    // ═══════════════════════════════════════════════════════════════════════
    // MATERIAL 3 DARK COLOR SYSTEM
    // ═══════════════════════════════════════════════════════════════════════
    private static final int C_BG          = 0xFF0D1117;
    private static final int C_SURFACE     = 0xFF161B22;
    private static final int C_SURFACE2    = 0xFF21262D;
    private static final int C_SURFACE3    = 0xFF30363D;
    private static final int C_PRIMARY     = 0xFF79C0FF;
    private static final int C_PRIMARY_BG  = 0xFF0D2A4A;
    private static final int C_ON_PRI      = 0xFF0D1117;
    private static final int C_SECONDARY   = 0xFFD2A8FF;
    private static final int C_TERTIARY    = 0xFF56D364;
    private static final int C_ERROR       = 0xFFF85149;
    private static final int C_TEXT        = 0xFFE6EDF3;
    private static final int C_TEXT_DIM    = 0xFF8B949E;
    private static final int C_TEXT_HINT   = 0xFF484F58;
    private static final int C_DIVIDER     = 0xFF21262D;
    private static final int C_GREEN       = 0xFF56D364;
    private static final int C_GREEN_BG    = 0xFF0A2A14;
    private static final int C_ORANGE      = 0xFFE3B341;
    private static final int C_ORANGE_BG   = 0xFF2A1F0A;
    private static final int C_RED         = 0xFFF85149;
    private static final int C_RED_BG      = 0xFF2A0A0A;
    private static final int C_PURPLE      = 0xFFD2A8FF;
    private static final int C_PURPLE_BG   = 0xFF1E1030;
    private static final int C_BLUE_BG     = 0xFF0D2A4A;

    // ═══════════════════════════════════════════════════════════════════════
    // REQUEST CODES
    // ═══════════════════════════════════════════════════════════════════════
    private static final int REQ_FILE_CHOOSER     = 1001;
    private static final int REQ_IMAGE_CHOOSER    = 1002;
    private static final int REQ_WRITE_PERMISSION = 2001;
    private static final int REQ_READ_PERMISSION  = 2002;
    private static final int REQ_CAMERA           = 2003;
    private static final int REQ_LOCATION         = 2004;
    private static final int REQ_AUDIO            = 2005;

    // ═══════════════════════════════════════════════════════════════════════
    // UI COMPONENTS
    // ═══════════════════════════════════════════════════════════════════════
    private LinearLayout  rootLayout;
    private LinearLayout  topBar;
    private LinearLayout  navBar;
    private LinearLayout  tabBar;
    private EditText      urlInput;
    private ProgressBar   progressBar;
    private WebView       webView;
    private TextView      statusDot;
    private TextView      sandboxLabel;
    private TextView      secureIcon;
    private FrameLayout   webContainer;

    // ── Download snackbar ────────────────────────────────────────────────────
    private LinearLayout  snackBar;
    private TextView      snackText;
    private boolean       snackVisible = false;

    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════
    private SharedPreferences    prefs;
    private SandboxProfile       profile;
    private final List<TabEntry> tabs       = new ArrayList<>();
    private int                  currentTab = -1;
    private boolean              incognito  = false;
    private ValueCallback<Uri[]> fileChooserCallback;
    private String               pendingDownloadUrl;
    private String               pendingDownloadMime;
    private String               pendingDownloadFileName;

    // Download manager
    private DownloadManager downloadManager;
    private BroadcastReceiver downloadReceiver;

    // ═══════════════════════════════════════════════════════════════════════
    // BLOCK LIST — Ad / Tracker
    // ═══════════════════════════════════════════════════════════════════════
    private static final Set<String> BLOCK_LIST = new HashSet<String>() {{
        add("doubleclick.net");       add("googlesyndication.com");
        add("googletagmanager.com");  add("google-analytics.com");
        add("analytics.google.com"); add("adservice.google.com");
        add("pagead2.googlesyndication.com"); add("connect.facebook.net");
        add("platform.twitter.com"); add("scorecardresearch.com");
        add("quantserve.com");       add("outbrain.com");
        add("taboola.com");          add("ads.yahoo.com");
        add("advertising.com");      add("moatads.com");
        add("hotjar.com");           add("mouseflow.com");
        add("crazyegg.com");         add("fullstory.com");
        add("segment.com");          add("mixpanel.com");
        add("amplitude.com");        add("intercom.io");
        add("zendesk.com/embeddable"); add("tiktok.com/i18n/pixel");
    }};

    // ═══════════════════════════════════════════════════════════════════════
    // MODELS
    // ═══════════════════════════════════════════════════════════════════════
    static class TabEntry {
        String   title  = "New Tab";
        String   url    = "";
        TextView chip;
        boolean  loaded = false;
    }

    static class SandboxProfile {
        String  ua             = "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36";
        String  deviceModel    = "Pixel 9";
        boolean jsEnabled      = true;
        boolean cookiesEnabled = false;
        boolean locationEnabled  = false;
        boolean mediaEnabled     = false;
        boolean adBlock          = true;
        boolean dnt              = true;
        boolean darkMode         = true;
        boolean clearOnExit      = true;
        boolean downloadImages   = true;
        boolean downloadFiles    = true;
        String  customJs         = "";
        String  level            = "HIGH";
        String  searchEngine     = "https://www.google.com/search?q=";
        String  homePage         = "about:blank";
        int     textZoom         = 100;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(C_BG);
        getWindow().setNavigationBarColor(C_BG);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        prefs   = getSharedPreferences("braymi_browser", Context.MODE_PRIVATE);
        profile = loadProfile();
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        buildUI();
        requestEssentialPermissions();
        registerDownloadReceiver();

        String startUrl = profile.level.equals("OFF") ? getLastUrl() : profile.homePage;
        newTab(TextUtils.isEmpty(startUrl) ? "about:blank" : startUrl);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else {
            new AlertDialog.Builder(this)
                .setTitle("Exit Braymi?")
                .setMessage("Do you want to close the browser?")
                .setPositiveButton("Exit", (d, w) -> {
                    if (profile.clearOnExit) performFullClear();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
            styleAlertDialog(null);
        }
    }

    @Override
    protected void onPause()   { super.onPause(); webView.onPause(); saveProfile(); }
    @Override
    protected void onResume()  { super.onResume(); webView.onResume(); }

    @Override
    protected void onDestroy() {
        if (profile.clearOnExit) performFullClear();
        if (downloadReceiver != null) {
            try { unregisterReceiver(downloadReceiver); } catch (Exception ignored) {}
        }
        webView.destroy();
        super.onDestroy();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PERMISSIONS
    // ═══════════════════════════════════════════════════════════════════════
    private void requestEssentialPermissions() {
        List<String> needed = new ArrayList<>();
        // Storage read (always needed for file chooser)
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 33) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.READ_MEDIA_IMAGES);
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.READ_MEDIA_VIDEO);
        }
        if (!needed.isEmpty()) {
            requestPermissions(needed.toArray(new String[0]), REQ_READ_PERMISSION);
        }
    }

    private void requestWritePermission(Runnable onGranted) {
        if (Build.VERSION.SDK_INT >= 29) {
            // Android 10+ — scoped storage, no write permission needed
            if (onGranted != null) onGranted.run();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                if (onGranted != null) onGranted.run();
            } else {
                pendingDownloadCallback = onGranted;
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE_PERMISSION);
            }
        } else {
            if (onGranted != null) onGranted.run();
        }
    }

    private Runnable pendingDownloadCallback;

    private void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            }
        }
    }

    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            }
        }
    }

    private void requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int reqCode, String[] perms, int[] results) {
        super.onRequestPermissionsResult(reqCode, perms, results);
        switch (reqCode) {
            case REQ_WRITE_PERMISSION:
                if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                    if (pendingDownloadCallback != null) { pendingDownloadCallback.run(); pendingDownloadCallback = null; }
                } else {
                    toast("⚠️ Storage permission denied — download cancelled");
                }
                break;
            case REQ_LOCATION:
                if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                    profile.locationEnabled = true; applyWebSettings();
                    toast("📍 Location permission granted");
                } else {
                    toast("📍 Location permission denied");
                }
                break;
            case REQ_CAMERA:
                if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                    toast("📷 Camera permission granted");
                } else {
                    toast("📷 Camera permission denied");
                }
                break;
            case REQ_AUDIO:
                if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                    profile.mediaEnabled = true; applyWebSettings();
                    toast("🎙️ Audio permission granted");
                }
                break;
            case REQ_READ_PERMISSION:
                if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                    toast("✅ Storage access granted");
                }
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DOWNLOAD MANAGER
    // ═══════════════════════════════════════════════════════════════════════
    private void registerDownloadReceiver() {
    downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            showSnack("✅ Download complete!", C_GREEN, 3000);
        }
    };
    IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
    
    // Fix: specify export flag for Android 13+
    if (Build.VERSION.SDK_INT >= 33) {
        registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    } else {
        registerReceiver(downloadReceiver, filter);
    }
}

    private void startDownload(String url, String mimeType, String fileName) {
        requestWritePermission(() -> {
            try {
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Braymi/" + fileName);
                req.setTitle(fileName);
                req.setDescription("Downloading via Braymi Browser");
                if (!TextUtils.isEmpty(mimeType)) req.setMimeType(mimeType);
                // Forward cookies
                String cookies = CookieManager.getInstance().getCookie(url);
                if (!TextUtils.isEmpty(cookies)) req.addRequestHeader("Cookie", cookies);
                req.addRequestHeader("User-Agent", profile.ua);
                req.setAllowedOverMetered(true);
                req.setAllowedOverRoaming(true);
                downloadManager.enqueue(req);
                showSnack("⬇️ Downloading: " + fileName, C_PRIMARY, 4000);
            } catch (Exception e) {
                showSnack("❌ Download failed: " + e.getMessage(), C_RED, 4000);
            }
        });
    }

    private void showDownloadDialog(final String url, final String mime, final String suggestedName) {
        LinearLayout v = buildDialogLayout();

        TextView urlLbl = makeDialogLabel("File URL");
        EditText urlEt  = makeDialogInput(url, "URL", false);

        TextView nameLbl = makeDialogLabel("File Name");
        EditText nameEt  = makeDialogInput(suggestedName, "filename.ext", false);

        TextView mimeLbl = makeDialogLabel("MIME Type");
        EditText mimeEt  = makeDialogInput(mime, "e.g. image/jpeg", false);

        v.addView(urlLbl); v.addView(urlEt);
        v.addView(nameLbl); v.addView(nameEt);
        v.addView(mimeLbl); v.addView(mimeEt);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("⬇️  Download File");
        b.setView(wrapInScroll(v));
        b.setPositiveButton("Download", (d, w) -> {
            String finalName = nameEt.getText().toString().trim();
            if (TextUtils.isEmpty(finalName)) finalName = suggestedName;
            startDownload(urlEt.getText().toString().trim(), mimeEt.getText().toString().trim(), finalName);
        });
        b.setNegativeButton("Cancel", null);
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        dlg.show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UI BUILD — MAIN
    // ═══════════════════════════════════════════════════════════════════════
    @SuppressLint({"SetJavaScriptEnabled","ClickableViewAccessibility"})
    private void buildUI() {
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(C_BG);

        // ── Status bar spacer ─────────────────────────────────────────────
        View statusSpacer = new View(this);
        statusSpacer.setBackgroundColor(C_BG);
        rootLayout.addView(statusSpacer, new LinearLayout.LayoutParams(-1, getStatusBarHeight()));

        // ── Top bar ───────────────────────────────────────────────────────
        buildTopBar();

        // ── Tab strip ─────────────────────────────────────────────────────
        LinearLayout tabBarOuter = new LinearLayout(this);
        tabBarOuter.setOrientation(LinearLayout.HORIZONTAL);
        tabBarOuter.setBackgroundColor(C_BG);
        tabBarOuter.setGravity(Gravity.CENTER_VERTICAL);
        tabBarOuter.setPadding(0, 0, 0, dp(2));

        // divider above tab strip
        View tabDiv = new View(this);
        tabDiv.setBackgroundColor(C_DIVIDER);

        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setPadding(dp(4), dp(3), dp(4), dp(3));
        tabScroll.addView(tabBar);

        TextView newTabBtn = new TextView(this);
        newTabBtn.setText("+");
        newTabBtn.setTextColor(C_PRIMARY);
        newTabBtn.setTextSize(18);
        newTabBtn.setGravity(Gravity.CENTER);
        newTabBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
        newTabBtn.setTypeface(null, Typeface.BOLD);
        newTabBtn.setOnClickListener(v -> newTab("about:blank"));

        tabBarOuter.addView(tabScroll, new LinearLayout.LayoutParams(0, -2, 1f));
        tabBarOuter.addView(newTabBtn);

        // ── Progress ──────────────────────────────────────────────────────
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= 21)
            progressBar.getProgressDrawable().setTint(C_PRIMARY);

        // ── WebView container ─────────────────────────────────────────────
        webContainer = new FrameLayout(this);
        webView = new WebView(this);
        webView.setBackgroundColor(C_BG);
        applyWebSettings();
        webView.setWebViewClient(buildWebViewClient());
        webView.setWebChromeClient(buildWebChromeClient());
        addJsBridge(webView);
        setupDownloadListener();

        webContainer.addView(webView,
            new FrameLayout.LayoutParams(-1, -1));

        // Snackbar (inside web container, bottom overlay)
        buildSnackBar();
        webContainer.addView(snackBar,
            new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        // ── Nav bar ───────────────────────────────────────────────────────
        buildNavBar();

        // ── Nav spacer ────────────────────────────────────────────────────
        View navSpacer = new View(this);
        navSpacer.setBackgroundColor(C_BG);
        int navH = getNavBarHeight();

        // ── Assemble ──────────────────────────────────────────────────────
        rootLayout.addView(tabDiv,         new LinearLayout.LayoutParams(-1, dp(1)));
        rootLayout.addView(topBar,         new LinearLayout.LayoutParams(-1, -2));
        rootLayout.addView(tabBarOuter,    new LinearLayout.LayoutParams(-1, -2));
        rootLayout.addView(progressBar,    new LinearLayout.LayoutParams(-1, dp(3)));
        rootLayout.addView(webContainer,   new LinearLayout.LayoutParams(-1, 0, 1f));
        rootLayout.addView(navBar,         new LinearLayout.LayoutParams(-1, -2));
        if (navH > 0)
            rootLayout.addView(navSpacer,  new LinearLayout.LayoutParams(-1, navH));

        setContentView(rootLayout);
    }

    private void buildTopBar() {
        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(C_SURFACE);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(8), dp(7), dp(8), dp(7));

        // Draw bottom border on top bar
        GradientDrawable topBarBg = new GradientDrawable();
        topBarBg.setColor(C_SURFACE);
        topBar.setBackground(topBarBg);

        // Status dot
        statusDot = new TextView(this);
        statusDot.setText("⬤");
        statusDot.setTextSize(8);
        statusDot.setPadding(dp(4), 0, dp(6), 0);
        updateStatusDot();

        // Secure icon (lock)
        secureIcon = new TextView(this);
        secureIcon.setText("🔓");
        secureIcon.setTextSize(12);
        secureIcon.setPadding(0, 0, dp(4), 0);

        // URL input
        urlInput = new EditText(this);
        urlInput.setTextColor(C_TEXT);
        urlInput.setHintTextColor(C_TEXT_HINT);
        urlInput.setHint("Search or URL...");
        urlInput.setSingleLine(true);
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        urlInput.setTextSize(13);
        urlInput.setEllipsize(TextUtils.TruncateAt.END);
        setRoundBg(urlInput, C_SURFACE2, dp(18));
        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
               (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                navigate(urlInput.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });
        urlInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                urlInput.selectAll();
                setRoundBg(urlInput, C_SURFACE3, dp(18));
            } else {
                setRoundBg(urlInput, C_SURFACE2, dp(18));
            }
        });

        // Menu button
        TextView menuBtn = makeTopBarIconBtn("⋮");
        menuBtn.setOnClickListener(v -> showMenu());

        // Sandbox badge
        sandboxLabel = new TextView(this);
        sandboxLabel.setTextSize(9);
        sandboxLabel.setPadding(dp(7), dp(3), dp(7), dp(3));
        sandboxLabel.setTypeface(null, Typeface.BOLD);
        sandboxLabel.setLetterSpacing(0.05f);
        updateSandboxLabel();

        topBar.addView(statusDot);
        topBar.addView(secureIcon);
        LinearLayout.LayoutParams urlP = new LinearLayout.LayoutParams(0, -2, 1f);
        urlP.setMargins(dp(2), 0, dp(6), 0);
        topBar.addView(urlInput, urlP);
        topBar.addView(sandboxLabel);
        topBar.addView(menuBtn);
    }

    private void buildNavBar() {
        navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setBackgroundColor(C_SURFACE);
        navBar.setGravity(Gravity.CENTER);
        navBar.setPadding(dp(4), dp(6), dp(4), dp(6));

        navBar.addView(makeNavBtn("←",  v -> { if (webView.canGoBack()) webView.goBack(); }));
        navBar.addView(makeNavBtn("→",  v -> { if (webView.canGoForward()) webView.goForward(); }));
        navBar.addView(makeNavBtn("↺",  v -> webView.reload()));
        navBar.addView(makeNavBtn("⌂",  v -> navigate(profile.homePage)));
        navBar.addView(makeNavBtn("⬇",  v -> showDownloadsMenu()));
        navBar.addView(makeNavBtn("🔒",  v -> showSandboxControl()));
        navBar.addView(makeNavBtn("⋯",  v -> showMenu()));
    }

    private void buildSnackBar() {
        snackBar = new LinearLayout(this);
        snackBar.setOrientation(LinearLayout.HORIZONTAL);
        snackBar.setGravity(Gravity.CENTER_VERTICAL);
        snackBar.setPadding(dp(16), dp(12), dp(16), dp(12));
        snackBar.setVisibility(View.GONE);

        // margin bottom
        FrameLayout.LayoutParams snackP = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        snackP.setMargins(dp(16), 0, dp(16), dp(12));
        snackBar.setLayoutParams(snackP);

        snackText = new TextView(this);
        snackText.setTextColor(C_TEXT);
        snackText.setTextSize(13);
        snackBar.addView(snackText, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView dismiss = new TextView(this);
        dismiss.setText("✕");
        dismiss.setTextColor(C_TEXT_DIM);
        dismiss.setTextSize(14);
        dismiss.setPadding(dp(12), 0, 0, 0);
        dismiss.setOnClickListener(v -> hideSnack());
        snackBar.addView(dismiss);

        setRoundBg(snackBar, C_SURFACE3, dp(12));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WEB SETTINGS
    // ═══════════════════════════════════════════════════════════════════════
    @SuppressLint("SetJavaScriptEnabled")
    private void applyWebSettings() {
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(profile.jsEnabled);
        ws.setUserAgentString(profile.ua);
        ws.setDomStorageEnabled(!profile.clearOnExit);
        ws.setDatabaseEnabled(!profile.clearOnExit);
        ws.setSaveFormData(false);
        ws.setSavePassword(false);
        ws.setGeolocationEnabled(profile.locationEnabled);
        ws.setMediaPlaybackRequiresUserGesture(!profile.mediaEnabled);
        ws.setLoadsImagesAutomatically(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        ws.setSupportMultipleWindows(false);
        ws.setAllowFileAccess(true);          // needed for file:// from chooser
        ws.setAllowContentAccess(true);       // needed for content:// URIs
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        ws.setDefaultTextEncodingName("UTF-8");
        ws.setTextZoom(profile.textZoom);
        if (Build.VERSION.SDK_INT >= 26)
            ws.setDisabledActionModeMenuItems(WebSettings.MENU_ITEM_SHARE | WebSettings.MENU_ITEM_PROCESS_TEXT);
        if (Build.VERSION.SDK_INT >= 29) {
            webView.setForceDarkAllowed(true);
            ws.setForceDark(profile.darkMode ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
        }
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(profile.cookiesEnabled);
        cm.setAcceptThirdPartyCookies(webView, false);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DOWNLOAD LISTENER
    // ═══════════════════════════════════════════════════════════════════════
    private void setupDownloadListener() {
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            if (TextUtils.isEmpty(fileName)) fileName = "download_" + System.currentTimeMillis();
            showDownloadDialog(url, mimeType, fileName);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WEBCLIENT
    // ═══════════════════════════════════════════════════════════════════════
    private WebViewClient buildWebViewClient() {
        return new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                if (profile.adBlock) {
                    String host = req.getUrl().getHost();
                    if (host != null) {
                        for (String blocked : BLOCK_LIST) {
                            if (host.contains(blocked)) {
                                return new WebResourceResponse("text/plain", "utf-8",
                                    new ByteArrayInputStream(new byte[0]));
                            }
                        }
                    }
                }
                return super.shouldInterceptRequest(view, req);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(10);
                if (url != null) {
                    urlInput.setText(shortenUrl(url));
                    urlInput.clearFocus();
                    updateCurrentTabUrl(url);
                    updateSecureIcon(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                String title = view.getTitle();
                updateCurrentTabTitle(title != null && !title.isEmpty() ? title : (url != null ? url : "Tab"));
                if (url != null) {
                    saveLastUrl(url);
                    updateSecureIcon(url);
                }
                if (!TextUtils.isEmpty(profile.customJs))
                    view.evaluateJavascript(profile.customJs, null);
                if (profile.dnt)
                    view.evaluateJavascript("Object.defineProperty(navigator,'doNotTrack',{get:function(){return'1'}})", null);
                injectPrivacyJS(view);
                if (currentTab >= 0 && currentTab < tabs.size())
                    tabs.get(currentTab).loaded = true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("intent:")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                    catch (Exception ignored) {}
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                String html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                    "<style>body{background:#0D1117;color:#E6EDF3;font-family:sans-serif;display:flex;" +
                    "flex-direction:column;align-items:center;justify-content:center;height:100vh;gap:12px;margin:0;padding:24px;box-sizing:border-box}" +
                    "h2{color:#F85149;font-size:20px;margin:0}.url{color:#8B949E;font-size:12px;word-break:break-all;text-align:center}" +
                    ".code{color:#E3B341;font-size:13px}.btn{background:#21262D;color:#79C0FF;padding:10px 20px;" +
                    "border-radius:8px;cursor:pointer;border:none;font-size:14px;margin-top:8px}" +
                    "</style></head><body>" +
                    "<div style='font-size:48px'>⚠️</div>" +
                    "<h2>Page Failed to Load</h2>" +
                    "<p class='code'>Error " + errorCode + ": " + description + "</p>" +
                    "<p class='url'>" + failingUrl + "</p>" +
                    "<button class='btn' onclick='history.back()'>← Go Back</button>" +
                    "</body></html>";
                view.loadData(html, "text/html", "utf-8");
            }
        };
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CHROMECLIENT
    // ═══════════════════════════════════════════════════════════════════════
    private WebChromeClient buildWebChromeClient() {
        return new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
                if (progress == 100) {
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                        progressBar.setVisibility(View.GONE), 400);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                updateCurrentTabTitle(title);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                if (profile.locationEnabled) {
                    cb.invoke(origin, true, false);
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("📍 Location Request")
                        .setMessage(origin + " wants to access your location.\nEnable location in Sandbox settings?")
                        .setPositiveButton("Enable", (d, w) -> {
                            profile.locationEnabled = true;
                            requestLocationPermission();
                            applyWebSettings();
                            cb.invoke(origin, true, false);
                        })
                        .setNegativeButton("Deny", (d, w) -> cb.invoke(origin, false, false))
                        .show();
                    styleAlertDialog(null);
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (profile.mediaEnabled) {
                    request.grant(request.getResources());
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("🎙️ Media Permission")
                        .setMessage("A page wants to access camera/microphone.\nGrant access?")
                        .setPositiveButton("Grant", (d, w) -> {
                            profile.mediaEnabled = true;
                            requestCameraPermission();
                            requestAudioPermission();
                            applyWebSettings();
                            request.grant(request.getResources());
                        })
                        .setNegativeButton("Deny", (d, w) -> request.deny())
                        .show();
                    styleAlertDialog(null);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> cb, FileChooserParams params) {
                fileChooserCallback = cb;
                showFileChooserDialog(params);
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, android.webkit.JsResult result) {
                showMaterialDialog("Page Alert", message, "OK", () -> result.confirm(), null, null);
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, android.webkit.JsResult result) {
                showMaterialDialog("Page Confirm", message, "OK", () -> result.confirm(), "Cancel", () -> result.cancel());
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String def, android.webkit.JsPromptResult result) {
                final EditText et = makeDialogInput(def, "Enter value...", false);
                LinearLayout v = buildDialogLayout();
                TextView lbl = new TextView(this == null ? MainActivity.this : MainActivity.this);
                lbl.setText(message);
                lbl.setTextColor(C_TEXT);
                lbl.setTextSize(14);
                lbl.setPadding(0, 0, 0, dp(8));
                v.addView(lbl);
                v.addView(et);
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                b.setTitle("Page Prompt");
                b.setView(v);
                b.setPositiveButton("OK",     (d, w) -> result.confirm(et.getText().toString()));
                b.setNegativeButton("Cancel", (d, w) -> result.cancel());
                AlertDialog dlg = b.create();
                styleAlertDialog(dlg);
                dlg.show();
                return true;
            }
        };
    }

    // ── File Chooser ─────────────────────────────────────────────────────────
    private void showFileChooserDialog(WebChromeClient.FileChooserParams params) {
        String[] options = {
            "🖼️  Images / Photos",
            "📹  Video Files",
            "🎵  Audio Files",
            "📄  Documents (PDF, DOC…)",
            "📁  All Files",
            "📷  Take Photo (Camera)"
        };

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("📂 Choose File");
        b.setItems(options, (d, which) -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            switch (which) {
                case 0: intent.setType("image/*"); break;
                case 1: intent.setType("video/*"); break;
                case 2: intent.setType("audio/*"); break;
                case 3: intent.setType("application/pdf");
                        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                            new String[]{"application/pdf","application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "text/plain"}); break;
                case 4: intent.setType("*/*"); break;
                case 5:
                    requestCameraPermission();
                    Intent cam = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    try { startActivityForResult(cam, REQ_IMAGE_CHOOSER); }
                    catch (Exception e) { fileChooserCallback.onReceiveValue(null); fileChooserCallback = null; }
                    return;
            }
            // Allow multiple selection for all types
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            Intent chooser = Intent.createChooser(intent, "Select File");
            try { startActivityForResult(chooser, REQ_FILE_CHOOSER); }
            catch (Exception e) { fileChooserCallback.onReceiveValue(null); fileChooserCallback = null; }
        });
        b.setNegativeButton("Cancel", (d, w) -> {
            if (fileChooserCallback != null) { fileChooserCallback.onReceiveValue(null); fileChooserCallback = null; }
        });
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        dlg.show();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE_CHOOSER || req == REQ_IMAGE_CHOOSER) {
            if (fileChooserCallback == null) return;
            if (res == RESULT_OK && data != null) {
                List<Uri> uris = new ArrayList<>();
                if (data.getClipData() != null) {
                    // Multiple files
                    for (int i = 0; i < data.getClipData().getItemCount(); i++)
                        uris.add(data.getClipData().getItemAt(i).getUri());
                } else if (data.getData() != null) {
                    uris.add(data.getData());
                }
                fileChooserCallback.onReceiveValue(uris.toArray(new Uri[0]));
            } else {
                fileChooserCallback.onReceiveValue(null);
            }
            fileChooserCallback = null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // JS BRIDGE
    // ═══════════════════════════════════════════════════════════════════════
    private void addJsBridge(WebView wv) {
        wv.addJavascriptInterface(new Object() {
            @JavascriptInterface public String getSandboxLevel() { return profile.level; }
            @JavascriptInterface public String getDeviceModel()  { return profile.deviceModel; }
            @JavascriptInterface public boolean isAdBlockEnabled() { return profile.adBlock; }
            @JavascriptInterface public boolean isDntEnabled()     { return profile.dnt; }
            @JavascriptInterface public void    showToast(String msg) {
                new Handler(Looper.getMainLooper()).post(() -> toast(msg));
            }
        }, "BraymiSandbox");
    }

    private void injectPrivacyJS(WebView wv) {
        if (profile.level.equals("OFF")) return;
        String js =
            "try{" +
            "Object.defineProperty(navigator,'platform',{get:()=>'Linux aarch64'});" +
            "Object.defineProperty(navigator,'hardwareConcurrency',{get:()=>4});" +
            "Object.defineProperty(navigator,'deviceMemory',{get:()=>4});" +
            "Object.defineProperty(navigator,'maxTouchPoints',{get:()=>5});" +
            "Object.defineProperty(screen,'colorDepth',{get:()=>24});" +
            "Object.defineProperty(screen,'pixelDepth',{get:()=>24});" +
            "window.chrome=undefined;" +
            (profile.dnt ? "Object.defineProperty(navigator,'doNotTrack',{get:()=>'1'});" : "") +
            (profile.level.equals("HIGH") ?
                "Object.defineProperty(navigator,'webdriver',{get:()=>false});" +
                "HTMLCanvasElement.prototype.toDataURL=function(){return 'data:image/png;base64,iVBORw0KGgo='};" +
                "var _gc=HTMLCanvasElement.prototype.getContext;" +
                "HTMLCanvasElement.prototype.getContext=function(t,a){var c=_gc.call(this,t,a);" +
                "if(c&&t==='2d'){var _gi=c.getImageData;c.getImageData=function(x,y,w,h)" +
                "{var d=_gi.call(this,x,y,w,h);for(var i=0;i<d.data.length;i+=4){d.data[i]^=1;d.data[i+1]^=1;}return d;};}return c;};" +
                "try{delete window.Notification;}catch(e){}" : "") +
            "}catch(e){}";
        wv.evaluateJavascript(js, null);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TABS
    // ═══════════════════════════════════════════════════════════════════════
    private void newTab(String url) {
        TabEntry tab = new TabEntry();
        tab.url = url;
        final int idx = tabs.size();
        tabs.add(tab);

        TextView chip = new TextView(this);
        chip.setText("New Tab");
        chip.setTextColor(C_TEXT_DIM);
        chip.setTextSize(11.5f);
        chip.setSingleLine(true);
        chip.setMaxWidth(dp(130));
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setPadding(dp(10), dp(5), dp(24), dp(5));

        // Close button overlaid on chip
        FrameLayout chipWrapper = new FrameLayout(this);
        LinearLayout.LayoutParams chipWrapP = new LinearLayout.LayoutParams(-2, -2);
        chipWrapP.setMargins(dp(2), 0, dp(2), 0);

        TextView closeBtn = new TextView(this);
        closeBtn.setText("×");
        closeBtn.setTextSize(13);
        closeBtn.setTextColor(C_TEXT_DIM);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setPadding(0, 0, dp(4), 0);
        FrameLayout.LayoutParams closeBtnP = new FrameLayout.LayoutParams(dp(20), -1, Gravity.END);
        closeBtn.setOnClickListener(v -> closeTab(idx));

        setRoundBg(chip, C_SURFACE2, dp(10));
        chipWrapper.addView(chip);
        chipWrapper.addView(closeBtn, closeBtnP);

        chip.setOnClickListener(v -> switchTab(idx));
        chip.setOnLongClickListener(v -> { showTabOptions(idx); return true; });

        tab.chip = chip;
        tabBar.addView(chipWrapper, chipWrapP);
        switchTab(idx);
        navigate(url);
    }

    private void switchTab(int idx) {
        if (idx < 0 || idx >= tabs.size()) return;
        for (int i = 0; i < tabs.size(); i++) {
            TabEntry t = tabs.get(i);
            if (t.chip == null) continue;
            boolean active = (i == idx);
            setRoundBg(t.chip, active ? C_PRIMARY_BG : C_SURFACE2, dp(10));
            t.chip.setTextColor(active ? C_PRIMARY : C_TEXT_DIM);
        }
        currentTab = idx;
        TabEntry t = tabs.get(idx);
        urlInput.setText(shortenUrl(t.url));
        if (!TextUtils.isEmpty(t.url) && !t.url.equals("about:blank"))
            webView.loadUrl(t.url);
        else
            webView.loadData(getBlankPage(), "text/html", "utf-8");
    }

    private void closeTab(int idx) {
        if (idx < 0 || idx >= tabs.size()) return;
        if (tabs.size() <= 1) { navigate("about:blank"); return; }
        // remove chip wrapper (parent of chip)
        View chipWrapper = (View) tabs.get(idx).chip.getParent();
        if (chipWrapper != null) tabBar.removeView(chipWrapper);
        tabs.remove(idx);
        int next = Math.min(Math.max(0, idx - 1), tabs.size() - 1);
        currentTab = -1; // reset so switchTab triggers load
        switchTab(next);
    }

    private void showTabOptions(int idx) {
        if (idx < 0 || idx >= tabs.size()) return;
        TabEntry t = tabs.get(idx);
        String[] options = {"📋  Copy URL", "↻  Reload Tab", "🆕  Duplicate Tab", "❌  Close Tab"};
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(t.title);
        b.setItems(options, (d, which) -> {
            switch (which) {
                case 0: copyText(t.url); toast("URL copied"); break;
                case 1: if (idx == currentTab) webView.reload(); break;
                case 2: newTab(t.url); break;
                case 3: closeTab(idx); break;
            }
        });
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        dlg.show();
    }

    private void updateCurrentTabTitle(String title) {
        if (currentTab >= 0 && currentTab < tabs.size()) {
            tabs.get(currentTab).title = title;
            if (tabs.get(currentTab).chip != null) tabs.get(currentTab).chip.setText(title);
        }
    }

    private void updateCurrentTabUrl(String url) {
        if (currentTab >= 0 && currentTab < tabs.size()) tabs.get(currentTab).url = url;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════
    private void navigate(String input) {
        if (TextUtils.isEmpty(input) || input.equals("about:blank")) {
            webView.loadData(getBlankPage(), "text/html", "utf-8");
            urlInput.setText("");
            updateSecureIcon("");
            return;
        }
        String url;
        if (input.startsWith("http://") || input.startsWith("https://") ||
            input.startsWith("file://") || input.startsWith("data:") || input.startsWith("about:")) {
            url = input;
        } else if (input.contains(".") && !input.contains(" ")) {
            url = "https://" + input;
        } else {
            url = profile.searchEngine + Uri.encode(input);
        }
        webView.loadUrl(url);
        urlInput.setText(shortenUrl(url));
        urlInput.clearFocus();
        updateSecureIcon(url);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MENUS
    // ═══════════════════════════════════════════════════════════════════════
    private void showMenu() {
        String[][] items = {
            {"🔒", "Sandbox Control"},
            {"📋", "Device Spoofer"},
            {"🍪", "Cookie Manager"},
            {"📝", "Custom JavaScript"},
            {"📊", "Site Info"},
            {"🔍", "Find in Page"},
            {"⬇", "Downloads"},
            {"🖼️", "Save Image"},
            {"📋", "Copy URL"},
            {"🕵️", incognito ? "Exit Incognito" : "Enter Incognito"},
            {"🗑️", "Clear All Data"},
            {"⚙️", "Settings"}
        };

        LinearLayout v = buildDialogLayout();
        v.setPadding(dp(8), dp(4), dp(8), dp(4));

        AlertDialog[] holder = {null};
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Braymi Browser");

        // Build menu rows
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            final String[][] fi = items;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(11), dp(8), dp(11));
            row.setBackground(getRipple(C_SURFACE3));

            TextView icon = new TextView(this);
            icon.setText(fi[idx][0]);
            icon.setTextSize(16);
            icon.setMinWidth(dp(30));
            icon.setGravity(Gravity.CENTER);

            TextView label = new TextView(this);
            label.setText(fi[idx][1]);
            label.setTextColor(C_TEXT);
            label.setTextSize(14);
            label.setPadding(dp(10), 0, 0, 0);

            row.addView(icon);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));

            if (i > 0) {
                View div = new View(this);
                div.setBackgroundColor(C_DIVIDER);
                v.addView(div, new LinearLayout.LayoutParams(-1, 1));
            }

            row.setOnClickListener(vv -> {
                if (holder[0] != null) holder[0].dismiss();
                handleMenuAction(idx);
            });
            v.addView(row);
        }

        b.setView(wrapInScroll(v));
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        holder[0] = dlg;
        dlg.show();
    }

    private void handleMenuAction(int idx) {
        switch (idx) {
            case 0:  showSandboxControl(); break;
            case 1:  showDeviceSpoofer(); break;
            case 2:  showCookieManager(); break;
            case 3:  showCustomJsEditor(); break;
            case 4:  showSiteInfo(); break;
            case 5:  showFindInPage(); break;
            case 6:  showDownloadsMenu(); break;
            case 7:  saveCurrentPageImage(); break;
            case 8:  copyUrl(); break;
            case 9:  toggleIncognito(); break;
            case 10: confirmClearAllData(); break;
            case 11: showSettings(); break;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SANDBOX CONTROL (Material 3 style)
    // ═══════════════════════════════════════════════════════════════════════
    private void showSandboxControl() {
        LinearLayout v = buildDialogLayout();

        // Level selector header
        TextView levelHeader = makeSectionHeader("Fingerprint Protection");
        v.addView(levelHeader);

        LinearLayout levelRow = new LinearLayout(this);
        levelRow.setOrientation(LinearLayout.HORIZONTAL);
        levelRow.setGravity(Gravity.CENTER);
        levelRow.setPadding(0, dp(4), 0, dp(12));

        for (String lv : new String[]{"OFF", "MEDIUM", "HIGH"}) {
            int[] lvColor = getLevelColors(lv);
            TextView btn = new TextView(this);
            btn.setText(lv);
            btn.setTextSize(11);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(dp(10), dp(7), dp(10), dp(7));
            btn.setTypeface(null, Typeface.BOLD);
            btn.setLetterSpacing(0.05f);
            boolean active = profile.level.equals(lv);
            btn.setTextColor(active ? C_TEXT : C_TEXT_DIM);
            setRoundBg(btn, active ? lvColor[0] : C_SURFACE2, dp(8));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.setMargins(dp(3), 0, dp(3), 0);
            btn.setOnClickListener(vv -> {
                profile.level = lv;
                applyWebSettings();
                updateStatusDot();
                showSandboxControl(); // refresh
            });
            levelRow.addView(btn, p);
        }
        v.addView(levelRow);

        // Divider
        v.addView(makeHDivider());
        v.addView(makeSectionHeader("Permissions & Features"));

        v.addView(makeSwitchRow("⚡  JavaScript",         profile.jsEnabled,       b -> profile.jsEnabled = b));
        v.addView(makeSwitchRow("🍪  Cookies",            profile.cookiesEnabled,  b -> profile.cookiesEnabled = b));
        v.addView(makeSwitchRow("📍  Location",           profile.locationEnabled, b -> {
            profile.locationEnabled = b;
            if (b) requestLocationPermission();
        }));
        v.addView(makeSwitchRow("🎙️  Mic / Camera",       profile.mediaEnabled,    b -> {
            profile.mediaEnabled = b;
            if (b) { requestCameraPermission(); requestAudioPermission(); }
        }));
        v.addView(makeHDivider());
        v.addView(makeSectionHeader("Privacy"));
        v.addView(makeSwitchRow("🚫  Ad & Tracker Block", profile.adBlock,         b -> profile.adBlock = b));
        v.addView(makeSwitchRow("🙈  Do Not Track",       profile.dnt,             b -> profile.dnt = b));
        v.addView(makeSwitchRow("🌑  Force Dark Mode",    profile.darkMode,        b -> profile.darkMode = b));
        v.addView(makeSwitchRow("💨  Clear on Exit",      profile.clearOnExit,     b -> profile.clearOnExit = b));
        v.addView(makeHDivider());
        v.addView(makeSectionHeader("Downloads"));
        v.addView(makeSwitchRow("🖼️  Allow Image DL",     profile.downloadImages,  b -> profile.downloadImages = b));
        v.addView(makeSwitchRow("📁  Allow File DL",      profile.downloadFiles,   b -> profile.downloadFiles = b));

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("🔒  Sandbox Control");
        b.setView(wrapInScroll(v));
        b.setPositiveButton("Apply & Reload", (d, w) -> {
            saveProfile();
            applyWebSettings();
            updateStatusDot();
            updateSandboxLabel();
            webView.reload();
            toast("✅ Sandbox applied");
        });
        b.setNegativeButton("Close", null);
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        dlg.show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DEVICE SPOOFER
    // ═══════════════════════════════════════════════════════════════════════
    private void showDeviceSpoofer() {
        final String[][] devices = {
            {"Pixel 9 Pro (Android 15)",  "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"},
            {"Pixel 9 (Android 15)",      "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"},
            {"Pixel 7 (Android 13)",      "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"},
            {"Samsung Galaxy S24 Ultra",  "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 SamsungBrowser/25.0"},
            {"Samsung Galaxy S22",        "Mozilla/5.0 (Linux; Android 12; SM-S908B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36 SamsungBrowser/19.0"},
            {"OnePlus 12 (OxygenOS)",     "Mozilla/5.0 (Linux; Android 14; CPH2581) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"},
            {"Xiaomi 14 Ultra",           "Mozilla/5.0 (Linux; Android 14; 2403PN0DC) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"},
            {"iPhone 16 Pro (iOS 18)",    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"},
            {"iPhone 15 Pro (iOS 17)",    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1"},
            {"iPad Pro M4",               "Mozilla/5.0 (iPad; CPU OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1"},
            {"Windows 11 Chrome 124",     "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"},
            {"macOS Sonoma Safari",       "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15"},
            {"Linux Firefox 124",         "Mozilla/5.0 (X11; Linux x86_64; rv:124.0) Gecko/20100101 Firefox/124.0"},
            {"Googlebot 2.1",             "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"},
            {"✏️  Custom User-Agent...",   ""}
        };

        LinearLayout v = buildDialogLayout();
        v.setPadding(dp(4), dp(4), dp(4), dp(4));

        AlertDialog[] holder = {null};

        for (int i = 0; i < devices.length; i++) {
            final int fi = i;
            boolean active = profile.deviceModel.equals(devices[i][0].replace("✏️  Custom User-Agent...", "Custom"));
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(10), dp(10), dp(10));
            row.setBackground(getRipple(C_SURFACE3));

            if (active) setRoundBg(row, C_PRIMARY_BG, dp(8));

            TextView lbl = new TextView(this);
            lbl.setText(devices[fi][0]);
            lbl.setTextColor(active ? C_PRIMARY : C_TEXT);
            lbl.setTextSize(13);
            lbl.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
            lbl.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

            if (active) {
                TextView check = new TextView(this);
                check.setText("✓");
                check.setTextColor(C_PRIMARY);
                check.setTextSize(14);
                row.addView(check);
            }
            row.addView(lbl);
            row.setOnClickListener(vv -> {
                if (holder[0] != null) holder[0].dismiss();
                if (fi == devices.length - 1) { showCustomUaDialog(); return; }
                profile.deviceModel = devices[fi][0];
                profile.ua          = devices[fi][1];
                applyWebSettings(); webView.reload();
                toast("📱 Device: " + profile.deviceModel);
            });

            if (i > 0) { v.addView(makeHDivider()); }
            v.addView(row);
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("📋  Device Spoofer");
        b.setView(wrapInScroll(v));
        b.setNegativeButton("Close", null);
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        holder[0] = dlg;
        dlg.show();
    }

    private void showCustomUaDialog() {
        LinearLayout v = buildDialogLayout();
        TextView lbl = makeDialogLabel("Custom User-Agent String");
        EditText et  = makeDialogInput(profile.ua, "Mozilla/5.0 ...", true);
        v.addView(lbl);
        v.addView(et);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("✏️  Custom User-Agent");
        b.setView(v);
        b.setPositiveButton("Set", (d, w) -> {
            profile.ua = et.getText().toString().trim();
            profile.deviceModel = "Custom";
            applyWebSettings(); webView.reload();
            toast("✅ UA set");
        });
        b.setNegativeButton("Cancel", null);
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        dlg.show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COOKIE MANAGER
    // ═══════════════════════════════════════════════════════════════════════
    private void showCookieManager() {
        String url = webView.getUrl();
        String cookies = url != null ? CookieManager.getInstance().getCookie(url) : null;
        boolean hasCookies = !TextUtils.isEmpty(cookies);

        LinearLayout v = buildDialogLayout();

        // Status card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        setRoundBg(card, C_SURFACE2, dp(10));
        LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(-1, -2);
        cardP.setMargins(0, 0, 0, dp(12));

        TextView siteLbl = new TextView(this);
        siteLbl.setText("🌐  " + (url != null ? url : "—"));
        siteLbl.setTextColor(C_TEXT_DIM);
        siteLbl.setTextSize(11);
        siteLbl.setSingleLine(true);
        siteLbl.setEllipsize(TextUtils.TruncateAt.END);

        TextView cookieLbl = new TextView(this);
        cookieLbl.setText(hasCookies ? cookies : "No cookies found for this site.");
        cookieLbl.setTextColor(hasCookies ? C_TEXT : C_TEXT_DIM);
        cookieLbl.setTextSize(11);

        card.addView(siteLbl); card.addView(cookieLbl);
        v.addView(card, cardP);

        // Global cookie state
        v.addView(makeSwitchRow("Accept Cookies (globally)", profile.cookiesEnabled, b -> {
            profile.cookiesEnabled = b;
            applyWebSettings();
        }));

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("🍪  Cookie Manager");
        b.setView(wrapInScroll(v));
        b.setPositiveButton("Clear All Cookies", (d, w) -> {
            CookieManager.getInstance().removeAllCookies(null);
            toast("🍪 Cookies cleared");
        });
        if (hasCookies) b.setNeutralButton("Copy", (d, w) -> { copyText(cookies); toast("Copied"); });
        b.setNegativeButton("Close", null);
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        dlg.show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CUSTOM JS EDITOR
    // ═══════════════════════════════════════════════════════════════════════
    private void showCustomJsEditor() {
        LinearLayout v = buildDialogLayout();
        TextView lbl = makeDialogLabel("JavaScript — injected on every page load");
        EditText et = makeDialogInput(profile.customJs,
            "// Example:\n// document.body.style.background='#111'\n// document.querySelectorAll('video').forEach(v=>v.play())",
            true);
        et.setMinLines(8);
        v.addView(lbl);
        v.addView(et);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("📝  Custom JavaScript");
        b.setView(wrapInScroll(v));
        b.setPositiveButton("Save & Run", (d, w) -> {
            profile.customJs = et.getText().toString().trim();
            webView.evaluateJavascript(profile.customJs, null);
            saveProfile(); toast("✅ JS injected");
        });
        b.setNeutralButton("Clear", (d, w) -> { profile.customJs = ""; saveProfile(); toast("Cleared"); });
        b.setNegativeButton("Cancel", null);
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        dlg.show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SITE INFO
    // ═══════════════════════════════════════════════════════════════════════
    private void showSiteInfo() {
        String url = webView.getUrl();
        boolean https = url != null && url.startsWith("https://");

        LinearLayout v = buildDialogLayout();

        // Security card
        LinearLayout secCard = new LinearLayout(this);
        secCard.setOrientation(LinearLayout.HORIZONTAL);
        secCard.setGravity(Gravity.CENTER_VERTICAL);
        secCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        setRoundBg(secCard, https ? C_GREEN_BG : C_RED_BG, dp(10));
        LinearLayout.LayoutParams secP = new LinearLayout.LayoutParams(-1, -2);
        secP.setMargins(0, 0, 0, dp(12));

        TextView secIcon = new TextView(this);
        secIcon.setText(https ? "🔒" : "🔓");
        secIcon.setTextSize(24);
        secIcon.setPadding(0, 0, dp(12), 0);

        LinearLayout secText = new LinearLayout(this);
        secText.setOrientation(LinearLayout.VERTICAL);
        TextView secTitle = new TextView(this);
        secTitle.setText(https ? "Secure Connection" : "Insecure Connection");
        secTitle.setTextColor(https ? C_GREEN : C_RED);
        secTitle.setTextSize(14);
        secTitle.setTypeface(null, Typeface.BOLD);
        TextView secSub = new TextView(this);
        secSub.setText(https ? "HTTPS — Traffic is encrypted" : "HTTP — Traffic is not encrypted");
        secSub.setTextColor(C_TEXT_DIM);
        secSub.setTextSize(11);
        secText.addView(secTitle); secText.addView(secSub);

        secCard.addView(secIcon); secCard.addView(secText);
        v.addView(secCard, secP);

        // Info rows
        addInfoRow(v, "URL",     url != null ? url : "—");
        addInfoRow(v, "Title",   webView.getTitle() != null ? webView.getTitle() : "—");
        v.addView(makeHDivider());
        v.addView(makeSectionHeader("Sandbox State"));
        addInfoRow(v, "Level",       profile.level);
        addInfoRow(v, "Device",      profile.deviceModel);
        addInfoRow(v, "JavaScript",  profile.jsEnabled ? "Enabled" : "Disabled");
        addInfoRow(v, "Cookies",     profile.cookiesEnabled ? "Enabled" : "Disabled");
        addInfoRow(v, "Ad Block",    profile.adBlock ? "Active" : "Off");
        addInfoRow(v, "Do-Not-Track",profile.dnt ? "Sending" : "Off");
        addInfoRow(v, "Location",    profile.locationEnabled ? "Allowed" : "Blocked");
        addInfoRow(v, "Media",       profile.mediaEnabled ? "Allowed" : "Blocked");
        addInfoRow(v, "Incognito",   incognito ? "Active" : "Off");

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("📊  Site Info");
        b.setView(wrapInScroll(v));
        b.setPositiveButton("OK", null);
        b.setNeutralButton("Copy URL", (d, w) -> { copyText(url != null ? url : ""); toast("URL copied"); });
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        dlg.show();
    }

    private void addInfoRow(LinearLayout parent, String key, String val) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        row.setPadding(0, dp(5), 0, dp(5));

        TextView k = new TextView(this);
        k.setText(key);
        k.setTextColor(C_TEXT_DIM);
        k.setTextSize(12);
        k.setMinWidth(dp(90));
        k.setTypeface(null, Typeface.BOLD);

        TextView vt = new TextView(this);
        vt.setText(val);
        vt.setTextColor(C_TEXT);
        vt.setTextSize(12);
        vt.setPadding(dp(8), 0, 0, 0);

        row.addView(k); row.addView(vt, new LinearLayout.LayoutParams(0, -2, 1f));
        parent.addView(row);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FIND IN PAGE
    // ═══════════════════════════════════════════════════════════════════════
    private void showFindInPage() {
        LinearLayout v = buildDialogLayout();
        EditText et = makeDialogInput("", "Search in page...", false);
        v.addView(makeDialogLabel("Find in Page"));
        v.addView(et);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("🔍  Find in Page");
        b.setView(v);
        b.setPositiveButton("Find ↓", (d, w) -> {
            String q = et.getText().toString();
            if (!TextUtils.isEmpty(q)) webView.findAllAsync(q);
        });
        b.setNegativeButton("Clear", (d, w) -> webView.clearMatches());
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        dlg.show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DOWNLOADS MENU
    // ═══════════════════════════════════════════════════════════════════════
    private void showDownloadsMenu() {
        LinearLayout v = buildDialogLayout();

        String curUrl = webView.getUrl();
        String curTitle = webView.getTitle();

        // Quick download current page
        LinearLayout quickRow = makeMenuItemRow("⬇",  "Download Current Page");
        quickRow.setOnClickListener(vv -> {
            if (curUrl != null && !curUrl.startsWith("about:"))
                showDownloadDialog(curUrl, "text/html", (curTitle != null ? curTitle : "page") + ".html");
        });
        v.addView(quickRow);
        v.addView(makeHDivider());

        // Save source
        LinearLayout srcRow = makeMenuItemRow("📄", "View Page Source");
        srcRow.setOnClickListener(vv -> {
            if (curUrl != null) navigate("view-source:" + curUrl);
        });
        v.addView(srcRow);
        v.addView(makeHDivider());

        // Custom URL download
        LinearLayout customRow = makeMenuItemRow("🔗", "Download from URL...");
        customRow.setOnClickListener(vv -> {
            showDownloadDialog("", "*/*", "file");
        });
        v.addView(customRow);
        v.addView(makeHDivider());

        // Open downloads folder
        LinearLayout folderRow = makeMenuItemRow("📁", "Open Downloads Folder");
        folderRow.setOnClickListener(vv -> {
            Intent i = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            try { startActivity(i); } catch (Exception e) { toast("Cannot open downloads"); }
        });
        v.addView(folderRow);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("⬇  Downloads");
        b.setView(wrapInScroll(v));
        b.setNegativeButton("Close", null);
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        dlg.show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SAVE IMAGE
    // ═══════════════════════════════════════════════════════════════════════
    private void saveCurrentPageImage() {
        String curUrl = webView.getUrl();
        if (curUrl == null || curUrl.startsWith("about:")) {
            toast("Nothing to save");
            return;
        }
        // Inject JS to get all image URLs on page
        webView.evaluateJavascript(
            "(function(){var imgs=document.querySelectorAll('img[src]');" +
            "var list=[];imgs.forEach(function(i){if(i.src)list.push(i.src);});" +
            "return JSON.stringify(list);})()",
            value -> {
                if (value == null || value.equals("null") || value.equals("\"[]\"")) {
                    runOnUiThread(() -> toast("No images found on page"));
                    return;
                }
                // Parse the JSON array
                String clean = value.replaceAll("^\"|\"$", "").replace("\\\"", "\"").replace("\\\\", "\\");
                // Simple split approach
                runOnUiThread(() -> showImagePickerDialog(clean, curUrl));
            }
        );
    }

    private void showImagePickerDialog(String jsonImages, String pageUrl) {
        // Very simple JSON array parse
        List<String> urls = new ArrayList<>();
        if (jsonImages.startsWith("[")) {
            jsonImages = jsonImages.substring(1, jsonImages.length() - 1);
            for (String part : jsonImages.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
                String u = part.trim().replaceAll("^\"|\"$", "");
                if (!u.isEmpty()) urls.add(u);
            }
        }

        if (urls.isEmpty()) {
            toast("No images found");
            return;
        }

        String[] arr = urls.toArray(new String[0]);
        String[] display = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            String u = arr[i];
            display[i] = (i + 1) + ".  " + (u.length() > 60 ? "..." + u.substring(u.length() - 57) : u);
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("🖼️  Save Image (" + arr.length + " found)");
        b.setItems(display, (d, which) -> {
            String imageUrl = arr[which];
            String ext = MimeTypeMap.getFileExtensionFromUrl(imageUrl);
            if (TextUtils.isEmpty(ext)) ext = "jpg";
            String fileName = "img_" + System.currentTimeMillis() + "." + ext;
            startDownload(imageUrl, "image/*", fileName);
        });
        b.setNegativeButton("Cancel", null);
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        dlg.show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INCOGNITO / CLEAR
    // ═══════════════════════════════════════════════════════════════════════
    private void toggleIncognito() {
        incognito = !incognito;
        if (incognito) {
            profile.clearOnExit   = true;
            profile.cookiesEnabled= false;
            profile.adBlock       = true;
            profile.dnt           = true;
            profile.level         = "HIGH";
        }
        applyWebSettings(); updateStatusDot(); updateSandboxLabel();
        toast(incognito ? "🕵️ Incognito ON — Session will not be saved" : "Incognito OFF");
    }

    private void confirmClearAllData() {
        showMaterialDialog(
            "⚠️ Clear All Data",
            "This will clear cache, cookies, history, storage, and form data.\nThis cannot be undone.",
            "Clear Everything",
            this::performFullClear,
            "Cancel", null);
    }

    private void performFullClear() {
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();
        CookieManager.getInstance().removeAllCookies(null);
        webView.getSettings().setDomStorageEnabled(false);
        webView.getSettings().setDomStorageEnabled(true);
        prefs.edit().remove("last_url").apply();
        toast("✅ All data cleared");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SETTINGS
    // ═══════════════════════════════════════════════════════════════════════
    private void showSettings() {
        LinearLayout v = buildDialogLayout();

        v.addView(makeSectionHeader("Search Engine"));
        String[] engines = {"Google", "DuckDuckGo", "Bing", "Brave", "Startpage", "Ecosia"};
        String[] engineUrls = {
            "https://www.google.com/search?q=",
            "https://duckduckgo.com/?q=",
            "https://www.bing.com/search?q=",
            "https://search.brave.com/search?q=",
            "https://www.startpage.com/do/search?q=",
            "https://www.ecosia.org/search?q="
        };
        for (int i = 0; i < engines.length; i++) {
            final String eu = engineUrls[i];
            final String en = engines[i];
            boolean active = profile.searchEngine.equals(eu);
            LinearLayout row = makeMenuItemRow(active ? "✓" : "  ", en);
            if (active) setRoundBg(row, C_PRIMARY_BG, dp(8));
            row.setOnClickListener(vv -> {
                profile.searchEngine = eu;
                toast("Search: " + en);
                showSettings(); // refresh (close current implicitly)
            });
            v.addView(row);
            if (i < engines.length - 1) v.addView(makeHDivider());
        }

        v.addView(makeHDivider());
        v.addView(makeSectionHeader("Text Zoom  (" + profile.textZoom + "%)"));
        LinearLayout zoomRow = new LinearLayout(this);
        zoomRow.setOrientation(LinearLayout.HORIZONTAL);
        zoomRow.setGravity(Gravity.CENTER_VERTICAL);
        zoomRow.setPadding(0, dp(6), 0, dp(6));
        for (int z : new int[]{75, 90, 100, 110, 125, 150}) {
            final int fz = z;
            TextView btn = new TextView(this);
            btn.setText(z + "%");
            btn.setTextSize(12);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(dp(8), dp(6), dp(8), dp(6));
            boolean active = profile.textZoom == z;
            btn.setTextColor(active ? C_PRIMARY : C_TEXT_DIM);
            setRoundBg(btn, active ? C_PRIMARY_BG : C_SURFACE2, dp(6));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.setMargins(dp(2), 0, dp(2), 0);
            btn.setOnClickListener(vv -> {
                profile.textZoom = fz;
                webView.getSettings().setTextZoom(fz);
                saveProfile();
            });
            zoomRow.addView(btn, p);
        }
        v.addView(zoomRow);

        v.addView(makeHDivider());
        v.addView(makeSectionHeader("Home Page"));
        EditText homeEt = makeDialogInput(profile.homePage, "https://...", false);
        v.addView(homeEt);

        v.addView(makeHDivider());
        v.addView(makeSectionHeader("About"));
        TextView aboutTv = new TextView(this);
        aboutTv.setText("Braymi Browser  v2.0\nPrivate Sandbox WebView Engine\nBuilt for Sketchware Pro\n© Braymi 2025");
        aboutTv.setTextColor(C_TEXT_DIM);
        aboutTv.setTextSize(12);
        aboutTv.setPadding(0, dp(6), 0, dp(6));
        v.addView(aboutTv);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("⚙️  Settings");
        b.setView(wrapInScroll(v));
        b.setPositiveButton("Save", (d, w) -> {
            profile.homePage = homeEt.getText().toString().trim();
            if (TextUtils.isEmpty(profile.homePage)) profile.homePage = "about:blank";
            saveProfile();
            toast("✅ Settings saved");
        });
        b.setNegativeButton("Close", null);
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        dlg.show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SNACKBAR
    // ═══════════════════════════════════════════════════════════════════════
    private final Handler snackHandler = new Handler(Looper.getMainLooper());
    private Runnable snackHideRunnable;

    private void showSnack(String msg, int color, long durationMs) {
        runOnUiThread(() -> {
            snackText.setText(msg);
            snackText.setTextColor(color);
            setRoundBg(snackBar, C_SURFACE3, dp(12));
            if (!snackVisible) {
                snackBar.setVisibility(View.VISIBLE);
                AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                fadeIn.setDuration(200);
                snackBar.startAnimation(fadeIn);
                snackVisible = true;
            }
            if (snackHideRunnable != null) snackHandler.removeCallbacks(snackHideRunnable);
            snackHideRunnable = this::hideSnack;
            snackHandler.postDelayed(snackHideRunnable, durationMs);
        });
    }

    private void hideSnack() {
        AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(200);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {}
            @Override public void onAnimationRepeat(Animation a) {}
            @Override public void onAnimationEnd(Animation a) {
                snackBar.setVisibility(View.GONE);
                snackVisible = false;
            }
        });
        snackBar.startAnimation(fadeOut);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BLANK PAGE
    // ═══════════════════════════════════════════════════════════════════════
    private String getBlankPage() {
        String incognitoBar = incognito
            ? "<div class='pill incog'>🕵️&nbsp;&nbsp;Incognito — Session not saved</div>"
            : "";
        return "<!DOCTYPE html><html><head>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<style>" +
            "*{margin:0;padding:0;box-sizing:border-box}" +
            "body{background:#0D1117;color:#8B949E;font-family:-apple-system,sans-serif;" +
            "display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;gap:14px;overflow:hidden}" +
            ".logo{color:#79C0FF;font-size:32px;font-weight:700;letter-spacing:3px}" +
            ".sub{font-size:12px;opacity:.6;letter-spacing:1px}" +
            ".pill{font-size:11px;padding:5px 14px;border-radius:20px;background:#161B22;color:#56D364;border:1px solid #21262D}" +
            ".incog{color:#D2A8FF;border-color:#30363D}" +
            ".dot{display:inline-block;width:6px;height:6px;border-radius:50%;background:#56D364;margin-right:6px;animation:pulse 2s infinite}" +
            "@keyframes pulse{0%,100%{opacity:1}50%{opacity:.4}}" +
            "</style></head><body>" +
            "<div class='logo'>⬡ BRAYMI</div>" +
            "<div class='pill'><span class='dot'></span>Sandbox Active · Fingerprints Masked</div>" +
            "<div class='sub'>Type a URL or search above</div>" +
            incognitoBar +
            "</body></html>";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MATERIAL 3 DIALOG HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    private void styleAlertDialog(AlertDialog dlg) {
        // Apply dark background to the dialog window after show
        if (dlg != null) {
            dlg.setOnShowListener(d -> {
                try {
                    View decorView = dlg.getWindow().getDecorView();
                    decorView.setBackgroundColor(Color.TRANSPARENT);
                } catch (Exception ignored) {}
            });
        }
    }

    private void showMaterialDialog(String title, String msg, String posText, Runnable pos, String negText, Runnable neg) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(title);
        b.setMessage(msg);
        if (posText != null) b.setPositiveButton(posText, (d, w) -> { if (pos != null) pos.run(); });
        if (negText != null) b.setNegativeButton(negText, (d, w) -> { if (neg != null) neg.run(); });
        AlertDialog dlg = b.create();
        styleAlertDialog(dlg);
        dlg.show();
    }

    private LinearLayout buildDialogLayout() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(16), dp(8), dp(16), dp(8));
        v.setBackgroundColor(C_SURFACE);
        return v;
    }

    private ScrollView wrapInScroll(View v) {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(C_SURFACE);
        sv.addView(v);
        return sv;
    }

    private TextView makeDialogLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(C_TEXT_DIM);
        tv.setTextSize(11);
        tv.setLetterSpacing(0.05f);
        tv.setAllCaps(true);
        tv.setPadding(0, dp(8), 0, dp(4));
        return tv;
    }

    private EditText makeDialogInput(String def, String hint, boolean multiLine) {
        EditText et = new EditText(this);
        et.setText(def);
        et.setHint(hint);
        et.setTextColor(C_TEXT);
        et.setHintTextColor(C_TEXT_HINT);
        et.setBackgroundColor(Color.TRANSPARENT);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        et.setTextSize(13);
        if (multiLine) {
            et.setMinLines(3);
            et.setGravity(Gravity.TOP);
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        } else {
            et.setSingleLine(true);
        }
        setRoundBg(et, C_SURFACE2, dp(8));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(8));
        et.setLayoutParams(p);
        return et;
    }

    private LinearLayout makeMenuItemRow(String icon, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(11), dp(8), dp(11));
        row.setBackground(getRipple(C_SURFACE3));

        TextView ic = new TextView(this);
        ic.setText(icon);
        ic.setTextSize(15);
        ic.setMinWidth(dp(28));
        ic.setGravity(Gravity.CENTER);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(C_TEXT);
        lbl.setTextSize(14);
        lbl.setPadding(dp(8), 0, 0, 0);

        row.addView(ic);
        row.addView(lbl, new LinearLayout.LayoutParams(0, -2, 1f));
        return row;
    }

    private View makeHDivider() {
        View v = new View(this);
        v.setBackgroundColor(C_DIVIDER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 1);
        p.setMargins(0, dp(2), 0, dp(2));
        v.setLayoutParams(p);
        return v;
    }

    private TextView makeSectionHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(C_PRIMARY);
        tv.setTextSize(11);
        tv.setAllCaps(true);
        tv.setLetterSpacing(0.1f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dp(12), 0, dp(6));
        return tv;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SWITCH ROW  (Material 3 style toggle)
    // ═══════════════════════════════════════════════════════════════════════
    interface BoolCallback { void set(boolean b); }

    private View makeSwitchRow(String label, boolean current, BoolCallback cb) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(C_TEXT);
        lbl.setTextSize(13.5f);
        row.addView(lbl, new LinearLayout.LayoutParams(0, -2, 1f));

        // Custom toggle track
        final boolean[] val = {current};
        FrameLayout track = new FrameLayout(this);
        int trackW = dp(44), trackH = dp(24);
        track.setMinimumWidth(trackW);
        track.setMinimumHeight(trackH);

        // Thumb
        View thumb = new View(this);
        int thumbSz = dp(18);
        FrameLayout.LayoutParams thumbP = new FrameLayout.LayoutParams(thumbSz, thumbSz, Gravity.CENTER_VERTICAL);
        thumbP.setMargins(val[0] ? dp(22) : dp(3), 0, 0, 0);

        GradientDrawable thumbBg = new GradientDrawable();
        thumbBg.setShape(GradientDrawable.OVAL);
        thumbBg.setColor(C_ON_PRI);
        thumb.setBackground(thumbBg);
        track.addView(thumb, thumbP);

        // Track background
        GradientDrawable trackBg = new GradientDrawable();
        trackBg.setCornerRadius(dp(12));
        trackBg.setColor(val[0] ? C_PRIMARY : C_SURFACE3);
        track.setBackground(trackBg);

        Runnable updateToggle = () -> {
            trackBg.setColor(val[0] ? C_PRIMARY : C_SURFACE3);
            thumbBg.setColor(val[0] ? C_ON_PRI : C_TEXT_DIM);
            ((FrameLayout.LayoutParams) thumb.getLayoutParams()).setMargins(val[0] ? dp(23) : dp(3), 0, 0, 0);
            thumb.requestLayout();
        };

        track.setOnClickListener(v -> {
            val[0] = !val[0];
            cb.set(val[0]);
            updateToggle.run();
        });
        lbl.setOnClickListener(v -> { track.performClick(); });

        row.addView(track, new LinearLayout.LayoutParams(trackW, trackH));
        return row;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TOP BAR / NAV HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    private TextView makeTopBarIconBtn(String icon) {
        TextView tv = new TextView(this);
        tv.setText(icon);
        tv.setTextColor(C_TEXT_DIM);
        tv.setTextSize(22);
        tv.setPadding(dp(8), dp(4), dp(4), dp(4));
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private TextView makeNavBtn(String icon, View.OnClickListener l) {
        TextView tv = new TextView(this);
        tv.setText(icon);
        tv.setTextSize(17);
        tv.setTextColor(C_TEXT_DIM);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(6), 0, dp(6));
        tv.setOnClickListener(l);
        tv.setMinHeight(dp(40));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        tv.setLayoutParams(p);
        return tv;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATUS / BADGE UPDATES
    // ═══════════════════════════════════════════════════════════════════════
    private int[] getLevelColors(String level) {
        switch (level) {
            case "HIGH":   return new int[]{C_GREEN_BG,  C_GREEN};
            case "MEDIUM": return new int[]{C_ORANGE_BG, C_ORANGE};
            default:       return new int[]{C_RED_BG,    C_RED};
        }
    }

    private void updateStatusDot() {
        int[] cols = getLevelColors(profile.level);
        int color = incognito ? C_PURPLE : cols[1];
        statusDot.setTextColor(color);
    }

    private void updateSandboxLabel() {
        String text = incognito ? "🕵️  INCOGNITO" : ("🔒  " + profile.level);
        int[] cols = getLevelColors(profile.level);
        int bg  = incognito ? C_PURPLE_BG : cols[0];
        int col = incognito ? C_PURPLE    : cols[1];
        sandboxLabel.setText(text);
        sandboxLabel.setTextColor(col);
        setRoundBg(sandboxLabel, bg, dp(10));
    }

    private void updateSecureIcon(String url) {
        if (url == null || url.isEmpty() || url.startsWith("about:")) {
            secureIcon.setText("  ");
        } else if (url.startsWith("https://")) {
            secureIcon.setText("🔒");
        } else {
            secureIcon.setText("🔓");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DRAWING / STYLE HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    private void setRoundBg(View v, int color, int radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        v.setBackground(gd);
    }

    @SuppressWarnings("deprecation")
    private android.graphics.drawable.Drawable getRipple(int rippleColor) {
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(rippleColor);
        mask.setCornerRadius(dp(8));
        if (Build.VERSION.SDK_INT >= 21) {
            return new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(rippleColor),
                null, mask);
        }
        return mask;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MISC HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    private String shortenUrl(String url) {
        if (url == null) return "";
        if (url.startsWith("https://www.")) return url.substring(12);
        if (url.startsWith("https://"))     return url.substring(8);
        if (url.startsWith("http://www."))  return url.substring(11);
        if (url.startsWith("http://"))      return url.substring(7);
        return url;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null)
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    private void copyUrl() {
        String url = webView.getUrl();
        copyText(url != null ? url : "");
        toast("📋 URL copied");
    }

    private void copyText(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("braymi", text));
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════════════════
    private SandboxProfile loadProfile() {
        SandboxProfile p = new SandboxProfile();
        p.jsEnabled       = prefs.getBoolean("js",        true);
        p.cookiesEnabled  = prefs.getBoolean("cookies",   false);
        p.locationEnabled = prefs.getBoolean("location",  false);
        p.mediaEnabled    = prefs.getBoolean("media",     false);
        p.adBlock         = prefs.getBoolean("adblock",   true);
        p.dnt             = prefs.getBoolean("dnt",       true);
        p.darkMode        = prefs.getBoolean("dark",      true);
        p.clearOnExit     = prefs.getBoolean("clear",     true);
        p.downloadImages  = prefs.getBoolean("dlImages",  true);
        p.downloadFiles   = prefs.getBoolean("dlFiles",   true);
        p.level           = prefs.getString("level",      "HIGH");
        p.deviceModel     = prefs.getString("device",     "Pixel 9");
        p.ua              = prefs.getString("ua",         p.ua);
        p.customJs        = prefs.getString("customjs",   "");
        p.searchEngine    = prefs.getString("engine",     p.searchEngine);
        p.homePage        = prefs.getString("home",       "about:blank");
        p.textZoom        = prefs.getInt("textZoom",      100);
        return p;
    }

    private void saveProfile() {
        prefs.edit()
            .putBoolean("js",       profile.jsEnabled)
            .putBoolean("cookies",  profile.cookiesEnabled)
            .putBoolean("location", profile.locationEnabled)
            .putBoolean("media",    profile.mediaEnabled)
            .putBoolean("adblock",  profile.adBlock)
            .putBoolean("dnt",      profile.dnt)
            .putBoolean("dark",     profile.darkMode)
            .putBoolean("clear",    profile.clearOnExit)
            .putBoolean("dlImages", profile.downloadImages)
            .putBoolean("dlFiles",  profile.downloadFiles)
            .putString("level",     profile.level)
            .putString("device",    profile.deviceModel)
            .putString("ua",        profile.ua)
            .putString("customjs",  profile.customJs)
            .putString("engine",    profile.searchEngine)
            .putString("home",      profile.homePage)
            .putInt("textZoom",     profile.textZoom)
            .apply();
    }

    private void saveLastUrl(String url) {
        if (!incognito && url != null && !url.startsWith("about:"))
            prefs.edit().putString("last_url", url).apply();
    }

    private String getLastUrl() { return prefs.getString("last_url", "about:blank"); }

    // ═══════════════════════════════════════════════════════════════════════
    // SYSTEM UTILS
    // ═══════════════════════════════════════════════════════════════════════
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private int getStatusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    private int getNavBarHeight() {
        int id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }
}
