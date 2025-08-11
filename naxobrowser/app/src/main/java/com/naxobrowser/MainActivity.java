package com.naxobrowser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceError;
import android.webkit.SslErrorHandler;
import android.webkit.JavascriptInterface;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.Button;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import android.webkit.CookieManager;

public class MainActivity extends AppCompatActivity implements VideoDetector.VideoDetectionListener {

    private static final String TAG = "MainActivity";
    
    private LinearLayout tabContainer;
    private HorizontalScrollView tabScrollView;
    private FrameLayout webViewContainer;
    private EditText urlEditText;
    private ImageView backButton, forwardButton, refreshButton, homeButton;
    private ImageView lockIcon, braveShieldIcon, menuIcon, videoDetectionButton;
    private ProgressBar progressBar;
    private Button newTabButton;
    
    private List<BrowserTab> tabs = new ArrayList<>();
    private BrowserTab currentTab;
    private int tabCounter = 1;
    
    private VideoDetector videoDetector;
    private boolean hasDetectedVideos = false;
    private Handler detectionHandler = new Handler();
    
    private ActivityResultLauncher<Intent> cookieActivityLauncher;

    public static class BrowserTab {
        public WebView webView;
        public String title;
        public String url;
        public View tabView;
        public boolean isLoading;
        public VideoDetector videoDetector;
        
        public BrowserTab(WebView webView, String title, String url, View tabView) {
            this.webView = webView;
            this.title = title;
            this.url = url;
            this.tabView = tabView;
            this.isLoading = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);

        initializeViews();
        setupActivityLaunchers();
        setupTabContainer();
        setupButtonListeners();
        setupAddressBar();
        
        // Create first tab
        createNewTab();
    }

    private void initializeViews() {
        tabContainer = findViewById(R.id.tabContainer);
        tabScrollView = findViewById(R.id.tabScrollView);
        webViewContainer = findViewById(R.id.webViewContainer);
        urlEditText = findViewById(R.id.urlEditText);
        progressBar = findViewById(R.id.progressBar);
        lockIcon = findViewById(R.id.lockIcon);
        braveShieldIcon = findViewById(R.id.braveShieldIcon);
        menuIcon = findViewById(R.id.menuIcon);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        refreshButton = findViewById(R.id.refreshButton);
        homeButton = findViewById(R.id.homeButton);
        newTabButton = findViewById(R.id.newTabButton);
        
        videoDetectionButton = findViewById(R.id.videoDetectionButton);
        if (videoDetectionButton != null) {
            videoDetectionButton.setColorFilter(Color.GRAY);
        }
    }

    private void setupTabContainer() {
        if (tabContainer == null) {
            // Create tab container if it doesn't exist
            tabContainer = new LinearLayout(this);
            tabContainer.setOrientation(LinearLayout.HORIZONTAL);
            tabContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            
            if (tabScrollView != null) {
                tabScrollView.addView(tabContainer);
            }
        }
        
        if (newTabButton == null) {
            newTabButton = new Button(this);
            newTabButton.setText("+");
            newTabButton.setOnClickListener(v -> createNewTab());
            tabContainer.addView(newTabButton);
        }
    }

    private void createNewTab() {
        // Create new WebView
        WebView newWebView = new WebView(this);
        newWebView.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        
        // Create tab view
        View tabView = LayoutInflater.from(this).inflate(R.layout.tab_item, null);
        TextView tabTitle = tabView.findViewById(R.id.tabTitle);
        ImageView closeTab = tabView.findViewById(R.id.closeTab);
        
        String tabName = "Tab " + tabCounter++;
        tabTitle.setText(tabName);
        
        // Create BrowserTab
        BrowserTab newTab = new BrowserTab(newWebView, tabName, "https://www.google.com", tabView);
        
        // Set click listeners
        tabView.setOnClickListener(v -> switchToTab(newTab));
        closeTab.setOnClickListener(v -> closeTab(newTab));
        newTab.videoDetector = new VideoDetector(this, newWebView);
        newTab.videoDetector.setVideoDetectionListener(this);
        
        tabs.add(newTab);
        
        // Add tab to container
        tabContainer.addView(tabView, tabContainer.getChildCount() - 1); // Add before new tab button
        
        // Setup WebView
        setupWebView(newWebView, newTab);
        
        // Switch to new tab
        switchToTab(newTab);
        
        // Load initial page
        newWebView.loadUrl("https://www.google.com");
    }

    private void setupWebView(WebView webView, BrowserTab tab) {
        WebSettings webSettings = webView.getSettings();
        
        // Basic settings
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // Cookie settings - IMPORTANT for cookie functionality
        webSettings.setSaveFormData(true);
        webSettings.setSavePassword(true);
        
        // Security and access settings
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        
        // Media settings - IMPORTANT for video detection
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        
        // Zoom settings
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        
        // Enhanced User Agent for better compatibility
        String userAgent = "Mozilla/5.0 (Linux; Android 12; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 NaxoBrowser/1.0";
        webSettings.setUserAgentString(userAgent);

        // Enable cookies explicitly
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        setupWebViewClient(webView, tab);
        setupWebChromeClient(webView, tab);
    }

    private void setupWebViewClient(WebView webView, BrowserTab tab) {
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "📄 Page started loading: " + url);
                
                tab.isLoading = true;
                tab.url = url;
                updateTabTitle(tab, "Loading...");
                
                if (currentTab == tab) {
                    urlEditText.setText(url);
                    updateLockIcon(url);
                    progressBar.setVisibility(View.VISIBLE);
                }
                
                // Clear previous videos and reset detection
                VideoManager.getInstance().clearAllVideos();
                resetVideoDetection();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "✅ Page finished loading: " + url);
                
                tab.isLoading = false;
                tab.url = url;
                
                // Get page title
                String title = view.getTitle();
                if (title != null && !title.isEmpty()) {
                    tab.title = title;
                    updateTabTitle(tab, title);
                }
                
                if (currentTab == tab) {
                    updateNavigationButtons();
                    progressBar.setVisibility(View.GONE);
                }
                
                // Multiple injection attempts with different delays
                injectVideoDetectionScript(tab);
                
                // Facebook specific detection
                if (url.contains("facebook.com") || url.contains("fb.com")) {
                    injectFacebookSpecificDetection(tab);
                }
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                Log.d(TAG, "👁️ Page visible: " + url);
                // Additional detection when page becomes visible
                detectionHandler.postDelayed(() -> injectVideoDetectionScript(tab), 1500);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Log all requests to find video URLs
                if (isPotentialVideoUrl(url)) {
                    Log.d(TAG, "🌐 Intercepted potential video: " + url);
                    
                    runOnUiThread(() -> {
                        VideoDetector.VideoInfo videoInfo = new VideoDetector.VideoInfo();
                        videoInfo.url = url;
                        videoInfo.format = detectFormat(url);
                        videoInfo.title = "Intercepted Video";
                        videoInfo.pageUrl = view.getUrl();
                        onVideoDetected(videoInfo);
                    });
                }
                
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // Continue despite SSL errors
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Log.e(TAG, "WebView error: " + error.getDescription());
                super.onReceivedError(view, request, error);
            }
        });
    }

    private void setupWebChromeClient(WebView webView, BrowserTab tab) {
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (currentTab == tab) {
                    progressBar.setProgress(newProgress);
                    if (newProgress == 100) {
                        progressBar.setVisibility(View.GONE);
                        // Final detection attempt when page is fully loaded
                        detectionHandler.postDelayed(() -> {
                            injectVideoDetectionScript(tab);
                            performManualVideoScan(tab);
                        }, 2000);
                    } else {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                Log.d(TAG, "🖥️ Console: " + consoleMessage.message());
                return super.onConsoleMessage(consoleMessage);
            }
        });
    }

    private void switchToTab(BrowserTab tab) {
        if (currentTab != null) {
            // Hide current tab's WebView
            currentTab.webView.setVisibility(View.GONE);
        }
        
        currentTab = tab;
        
        // Show new tab's WebView
        if (webViewContainer.getChildCount() > 0) {
            webViewContainer.removeAllViews();
        }
        webViewContainer.addView(tab.webView);
        tab.webView.setVisibility(View.VISIBLE);
        
        // Update UI
        urlEditText.setText(tab.url);
        updateLockIcon(tab.url);
        updateNavigationButtons();
        
        // Update tab selection
        updateTabSelection();
    }

    private void updateTabSelection() {
        for (BrowserTab tab : tabs) {
            View tabView = tab.tabView;
            if (tab == currentTab) {
                tabView.setBackgroundColor(Color.parseColor("#2196F3"));
            } else {
                tabView.setBackgroundColor(Color.parseColor("#F5F5F5"));
            }
        }
    }

    private void updateTabTitle(BrowserTab tab, String title) {
        TextView tabTitle = tab.tabView.findViewById(R.id.tabTitle);
        tabTitle.setText(title);
    }

    private void closeTab(BrowserTab tab) {
        if (tabs.size() <= 1) {
            // Don't close the last tab
            return;
        }
        
        int tabIndex = tabs.indexOf(tab);
        tabs.remove(tab);
        tabContainer.removeView(tab.tabView);
        
        // Switch to another tab
        if (currentTab == tab) {
            if (tabIndex >= tabs.size()) {
                tabIndex = tabs.size() - 1;
            }
            switchToTab(tabs.get(tabIndex));
        }
    }

    private void setupActivityLaunchers() {
        cookieActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String reloadUrl = result.getData().getStringExtra("reload_url");
                        if (reloadUrl != null && !reloadUrl.isEmpty() && currentTab != null) {
                            // Verify cookies before reloading
                            verifyCookiesForUrl(reloadUrl);
                            
                            // Clear cache and reload
                            currentTab.webView.clearCache(true);
                            currentTab.webView.loadUrl(reloadUrl);
                            showToast("Cookies imported. Reloading page...");
                        }
                    }
                });
    }
    
    private void verifyCookiesForUrl(String url) {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            String cookies = cookieManager.getCookie(url);
            Log.d(TAG, "=== COOKIE VERIFICATION ===");
            Log.d(TAG, "URL: " + url);
            Log.d(TAG, "Cookies: " + (cookies != null ? cookies : "null"));
            
            if (cookies != null && !cookies.isEmpty()) {
                String[] cookieArray = cookies.split("; ");
                Log.d(TAG, "Total cookies for " + url + ": " + cookieArray.length);
                for (String cookie : cookieArray) {
                    Log.d(TAG, "  - " + cookie);
                }
            } else {
                Log.w(TAG, "No cookies found for " + url);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verifying cookies: " + e.getMessage());
        }
    }

    private void setupButtonListeners() {
        backButton.setOnClickListener(v -> { 
            if (currentTab != null && currentTab.webView.canGoBack()) {
                currentTab.webView.goBack(); 
            }
        });
        forwardButton.setOnClickListener(v -> { 
            if (currentTab != null && currentTab.webView.canGoForward()) {
                currentTab.webView.goForward(); 
            }
        });
        refreshButton.setOnClickListener(v -> { 
            if (currentTab != null) {
                VideoManager.getInstance().clearAllVideos();
                resetVideoDetection();
                currentTab.webView.reload(); 
            }
        });
        homeButton.setOnClickListener(v -> {
            if (currentTab != null) {
                loadUrl("https://www.google.com");
            }
        });
        braveShieldIcon.setOnClickListener(v -> showToast("Shield Protection Active"));
        menuIcon.setOnClickListener(this::showMenu);
        
        if (videoDetectionButton != null) {
            videoDetectionButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, VideoListActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupAddressBar() {
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String input = urlEditText.getText().toString().trim();
                if (!input.isEmpty() && currentTab != null) {
                    hideKeyboard();
                    urlEditText.clearFocus();
                    handleUserInput(input);
                }
                return true;
            }
            return false;
        });
    }

    private void handleUserInput(String input) {
        if (Patterns.WEB_URL.matcher(input).matches()) {
            loadUrl(normalizeUrl(input));
        } else {
            performSearch(input);
        }
    }

    private String normalizeUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }

    private void loadUrl(String url) {
        if (!isNetworkAvailable()) {
            showToast("No internet connection");
            return;
        }
        if (currentTab != null) {
            Log.d(TAG, "🌐 Loading URL: " + url);
            currentTab.webView.loadUrl(url);
        }
    }

    private void performSearch(String query) {
        try {
            String searchUrl = "https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8");
            loadUrl(searchUrl);
        } catch (Exception e) {
            showToast("Error in search");
            Log.e(TAG, "Search error", e);
        }
    }

    private void injectVideoDetectionScript(BrowserTab tab) {
        if (tab.videoDetector != null) {
            Log.d(TAG, "💉 Injecting video detection script...");
            
            // Multiple injection attempts with different delays
            detectionHandler.post(() -> tab.videoDetector.setupVideoDetection());
            detectionHandler.postDelayed(() -> tab.videoDetector.setupVideoDetection(), 1000);
            detectionHandler.postDelayed(() -> tab.videoDetector.setupVideoDetection(), 3000);
            detectionHandler.postDelayed(() -> tab.videoDetector.setupVideoDetection(), 5000);
        }
    }

    private void injectFacebookSpecificDetection(BrowserTab tab) {
        Log.d(TAG, "📘 Injecting Facebook-specific detection...");
        
        String facebookScript = "javascript:(function() {" +
            "console.log('📘 Facebook video detection started');" +
            
            // Monitor Facebook's video player
            "const checkFbVideos = () => {" +
            "  const videoSelectors = [" +
            "    'video[src]'," +
            "    'video[data-video-url]'," +
            "    '[data-video-id] video'," +
            "    '.fbStoryAttachmentImage video'," +
            "    '[role=\"presentation\"] video'," +
            "    '.scaledImageFitWidth video'" +
            "  ];" +
            "  " +
            "  videoSelectors.forEach(selector => {" +
            "    try {" +
            "      const videos = document.querySelectorAll(selector);" +
            "      videos.forEach(video => {" +
            "        if (video.src && video.src !== '') {" +
            "          console.log('📱 FB Video found:', video.src);" +
            "          VideoDetectorAndroid.onVideoUrlDetected(video.src, 'FB_VIDEO', video.src);" +
            "        }" +
            "        if (video.currentSrc && video.currentSrc !== '') {" +
            "          console.log('📱 FB Video currentSrc found:', video.currentSrc);" +
            "          VideoDetectorAndroid.onVideoUrlDetected(video.currentSrc, 'FB_VIDEO', video.currentSrc);" +
            "        }" +
            "      });" +
            "    } catch (e) {" +
            "      console.log('Error checking selector:', selector, e);" +
            "    }" +
            "  });" +
            "};" +
            "checkFbVideos();" +
            "setInterval(checkFbVideos, 2000);" +
            "})();";
        
        tab.webView.evaluateJavascript(facebookScript, null);
    }

    private void performManualVideoScan(BrowserTab tab) {
        if (tab.videoDetector != null) {
            // Manual scan functionality removed for compatibility
        }
    }

    private void resetVideoDetection() {
        hasDetectedVideos = false;
        if (videoDetectionButton != null) {
            videoDetectionButton.setColorFilter(Color.GRAY);
        }
    }

    private boolean isPotentialVideoUrl(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains(".mp4") || lowerUrl.contains(".webm") || 
               lowerUrl.contains(".m3u8") || lowerUrl.contains(".ts") ||
               lowerUrl.contains("video") || lowerUrl.contains("stream");
    }

    private String detectFormat(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains(".mp4")) return "MP4";
        if (lowerUrl.contains(".webm")) return "WEBM";
        if (lowerUrl.contains(".m3u8")) return "HLS";
        if (lowerUrl.contains(".ts")) return "TS";
        return "UNKNOWN";
    }

    private void updateNavigationButtons() {
        if (currentTab != null) {
            backButton.setEnabled(currentTab.webView.canGoBack());
            forwardButton.setEnabled(currentTab.webView.canGoForward());
        }
    }

    private void updateLockIcon(String url) {
        if (url.startsWith("https://")) {
            lockIcon.setImageResource(R.drawable.ic_lock);
            lockIcon.setColorFilter(Color.GREEN);
        } else {
            lockIcon.setImageResource(R.drawable.ic_lock_open);
            lockIcon.setColorFilter(Color.RED);
        }
    }

    private void showMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.browser_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            // Menu items removed for compatibility
            return false;
        });
        
        popup.show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVideoDetected(VideoDetector.VideoInfo videoInfo) {
        hasDetectedVideos = true;
        if (videoDetectionButton != null) {
            videoDetectionButton.setColorFilter(Color.RED);
        }
        
        VideoManager.getInstance().addVideo(videoInfo);
        Log.d(TAG, "🎥 Video detected: " + videoInfo.url);
    }
    
    @Override
    public void onDrmDetected(String url, String type) {
        // DRM detection callback
        Log.d(TAG, "🔒 DRM detected: " + url + " type: " + type);
    }

    @Override
    public void onBackPressed() {
        if (currentTab != null && currentTab.webView.canGoBack()) {
            currentTab.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}