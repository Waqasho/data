package com.naxobrowser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView; // Import WebView
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CookieImportActivity extends AppCompatActivity implements CookieAdapter.OnCookieCheckedChangeListener {

    private static final String TAG = "CookieImportActivity";
    private EditText searchEditText;
    private CheckBox selectAllCheckBox;
    private RecyclerView cookieRecyclerView;
    private Button importButton;
    private CookieAdapter cookieAdapter;
    private List<Cookie> cookieList;
    private WebView webView; // Declare WebView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cookie_import);

        // Initialize WebView (it doesn't need to be visible)
        webView = new WebView(this);

        initViews();
        setupRecyclerView();
        setupListeners();

        String cookieData = getIntent().getStringExtra("cookie_data");
        if (cookieData != null && !cookieData.isEmpty()) {
            parseCookies(cookieData);
        } else {
            Toast.makeText(this, "No cookie data received.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        TextView titleTextView = findViewById(R.id.titleTextView);
        searchEditText = findViewById(R.id.searchEditText);
        selectAllCheckBox = findViewById(R.id.selectAllCheckBox);
        cookieRecyclerView = findViewById(R.id.cookieRecyclerView);
        Button closeButton = findViewById(R.id.closeButton);
        importButton = findViewById(R.id.importButton);
    }

    private void setupRecyclerView() {
        cookieList = new ArrayList<>();
        cookieAdapter = new CookieAdapter(cookieList, this);
        cookieRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cookieRecyclerView.setAdapter(cookieAdapter);
    }

    private void setupListeners() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cookieAdapter.filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        selectAllCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                cookieAdapter.selectAll(isChecked);
            }
        });

        findViewById(R.id.closeButton).setOnClickListener(v -> finish());

        importButton.setOnClickListener(v -> {
            List<Cookie> selectedCookies = cookieAdapter.getSelectedCookies();
            if (selectedCookies.isEmpty()) {
                Toast.makeText(this, "Please select at least one cookie to import.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show current cookies before import
            showCurrentCookies();
            
            // Check if there might be conflicts with existing cookies
            checkForCookieConflicts(selectedCookies);
            
            importSelectedCookies(selectedCookies);
        });
    }
    
    private void showCurrentCookies() {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            // Test with a common domain
            String testUrl = "https://google.com";
            String cookies = cookieManager.getCookie(testUrl);
            if (cookies != null && !cookies.isEmpty()) {
                Log.d(TAG, "Current cookies for " + testUrl + ": " + cookies);
            } else {
                Log.d(TAG, "No current cookies found for " + testUrl);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing current cookies: " + e.getMessage());
        }
    }
    
    private void checkForCookieConflicts(List<Cookie> selectedCookies) {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            int conflictCount = 0;
            
            for (Cookie cookie : selectedCookies) {
                if (cookie.domain != null && !cookie.domain.isEmpty()) {
                    String domain = cookie.domain.startsWith(".") ? cookie.domain.substring(1) : cookie.domain;
                    String url = "https://" + domain;
                    String existingCookies = cookieManager.getCookie(url);
                    
                    if (existingCookies != null && existingCookies.contains(cookie.name + "=")) {
                        conflictCount++;
                    }
                }
            }
            
            if (conflictCount > 0) {
                Toast.makeText(this, "Warning: " + conflictCount + " cookies may conflict with existing ones", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for cookie conflicts: " + e.getMessage());
        }
    }

    public CookieImportActivity() {
        cookieList = new ArrayList<>();
    }

    private void parseCookies(String cookieData) {
        cookieList.clear();
        
        if (cookieData == null || cookieData.trim().isEmpty()) {
            Toast.makeText(this, "No cookie data provided.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Auto-detect format
        String trimmedData = cookieData.trim();
        if (trimmedData.startsWith("[") || trimmedData.startsWith("{")) {
            parseJsonCookies(trimmedData);
        } else {
            parseNetscapeCookies(trimmedData);
        }

        Log.d(TAG, "Total cookies parsed: " + cookieList.size());
        ((TextView) findViewById(R.id.titleTextView)).setText("Import cookies (" + cookieList.size() + ")");
        cookieAdapter.updateCookieList(cookieList);
        cookieRecyclerView.scrollToPosition(0);
        
        if (cookieList.isEmpty()) {
            Toast.makeText(this, "No valid cookies found in the provided data.", Toast.LENGTH_LONG).show();
        }
    }

    private void parseJsonCookies(String cookieData) {
        try {
            // Handle case where data is a JSON array
            if (cookieData.trim().startsWith("[")) {
                JSONArray cookieArray = new JSONArray(cookieData);
                parseJsonCookieArray(cookieArray);
            } else { // Handle case where data is a JSON object
                JSONObject jsonObject = new JSONObject(cookieData);
                if (jsonObject.has("cookies") && jsonObject.get("cookies") instanceof JSONArray) {
                    parseJsonCookieArray(jsonObject.getJSONArray("cookies"));
                } else if (jsonObject.has("data") && jsonObject.get("data") instanceof JSONArray) {
                    parseJsonCookieArray(jsonObject.getJSONArray("data"));
                } else {
                    // Try to find any array in the object
                    Iterator<String> keys = jsonObject.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        if (jsonObject.get(key) instanceof JSONArray) {
                            parseJsonCookieArray(jsonObject.getJSONArray(key));
                            break;
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON cookies", e);
            Toast.makeText(this, "Error parsing JSON cookies: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error parsing JSON cookies", e);
            Toast.makeText(this, "Unexpected error parsing cookies: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void parseJsonCookieArray(JSONArray cookieArray) throws JSONException {
        for (int i = 0; i < cookieArray.length(); i++) {
            try {
                JSONObject cookieObj = cookieArray.getJSONObject(i);
                String name = cookieObj.optString("name", "");
                String value = cookieObj.optString("value", "");
                String domain = cookieObj.optString("domain", "");
                String path = cookieObj.optString("path", "/");
                boolean secure = cookieObj.optBoolean("secure", false);
                boolean httpOnly = cookieObj.optBoolean("httpOnly", false);
                
                // Cookie expiration can be in seconds (unix timestamp) or milliseconds
                long expiration = cookieObj.optLong("expirationDate", -1);
                if (expiration == -1) {
                    expiration = cookieObj.optLong("expires", -1);
                }
                
                // Convert milliseconds to seconds if needed
                if (expiration > 0 && expiration > 9999999999L) {
                    expiration = expiration / 1000;
                }

                // Validate required fields
                if (!name.isEmpty() && !domain.isEmpty() && !value.isEmpty()) {
                    // Clean domain - keep original domain format for proper cookie setting
                    String cleanDomain = domain;
                    
                    // Validate domain format
                    if (isValidDomain(cleanDomain)) {
                        Cookie cookie = new Cookie(name, value, cleanDomain, path, expiration, secure, httpOnly);
                        cookieList.add(cookie);
                        Log.d(TAG, "Added cookie: " + name + " for domain: " + cleanDomain);
                    } else {
                        Log.w(TAG, "Skipping cookie with invalid domain: " + domain);
                    }
                } else {
                    Log.w(TAG, "Skipping cookie with missing required fields: name=" + name + ", domain=" + domain + ", value=" + value);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing cookie at index " + i, e);
            }
        }
    }

    private void parseNetscapeCookies(String cookieData) {
        String[] lines = cookieData.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("\t");
            if (parts.length >= 7) {
                try {
                    String domain = parts[0];
                    String path = parts[2];
                    boolean secure = Boolean.parseBoolean(parts[3]);
                    long expiration = Long.parseLong(parts[4]);
                    String name = parts[5];
                    String value = parts[6];

                    // Validate required fields
                    if (!name.isEmpty() && !domain.isEmpty() && !value.isEmpty()) {
                        // Validate domain format
                        if (isValidDomain(domain)) {
                            Cookie cookie = new Cookie(name, value, domain, path, expiration, secure, false);
                            cookieList.add(cookie);
                            Log.d(TAG, "Added Netscape cookie: " + name + " for domain: " + domain);
                        } else {
                            Log.w(TAG, "Skipping Netscape cookie with invalid domain: " + domain);
                        }
                    } else {
                        Log.w(TAG, "Skipping Netscape cookie with missing required fields: name=" + name + ", domain=" + domain + ", value=" + value);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Skipping malformed Netscape line: " + line, e);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Netscape cookie line: " + line, e);
                }
            } else {
                Log.w(TAG, "Skipping Netscape line with insufficient parts: " + line);
            }
        }
    }

    private boolean isValidDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }
        
        // Remove leading dot if present
        String cleanDomain = domain.startsWith(".") ? domain.substring(1) : domain;
        
        // Basic domain validation
        return cleanDomain.length() > 0 && 
               cleanDomain.contains(".") && 
               !cleanDomain.startsWith(".") && 
               !cleanDomain.endsWith(".") &&
               cleanDomain.matches("^[a-zA-Z0-9.-]+$");
    }

    // FIXED COOKIE IMPORT FUNCTION
    private void importSelectedCookies(List<Cookie> selectedCookies) {
        CookieManager cookieManager = CookieManager.getInstance();
        
        // Enable cookies and third-party cookies
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true); // Pass webView instance
        
        int successCount = 0;
        int errorCount = 0;
        final String[] firstValidDomain = {null}; // Changed to final array
        
        Log.d(TAG, "=== STARTING ENHANCED COOKIE IMPORT ===");
        Log.d(TAG, "Total cookies to import: " + selectedCookies.size());
        
        for (Cookie cookie : selectedCookies) {
            try {
                String domain = cookie.domain;
                if (domain == null || domain.isEmpty()) {
                    Log.w(TAG, "Skipping cookie with null/empty domain: " + cookie.name);
                    errorCount++;
                    continue;
                }

                // Create proper URL for cookie setting
                String cleanDomain = domain.startsWith(".") ? domain.substring(1) : domain;
                String url = "https://" + cleanDomain;
                
                // Build cookie string with proper format
                StringBuilder cookieString = new StringBuilder();
                cookieString.append(cookie.name).append("=").append(cookie.value);

                // Add path
                if (cookie.path != null && !cookie.path.isEmpty()) {
                    cookieString.append("; Path=").append(cookie.path);
                } else {
                    cookieString.append("; Path=/");
                }

                // Add domain (keep original format with dot if present)
                cookieString.append("; Domain=").append(cookie.domain);

                // Add expiration
                if (cookie.expires > 0) {
                    // Ensure expiration is in seconds
                    long expirationSeconds = cookie.expires;
                    if (expirationSeconds > 9999999999L) {
                        expirationSeconds = expirationSeconds / 1000;
                    }
                    
                    Date expiryDate = new Date(expirationSeconds * 1000);
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                    cookieString.append("; Expires=").append(sdf.format(expiryDate));
                }

                // Add security flags
                if (cookie.secure) {
                    cookieString.append("; Secure");
                }

                if (cookie.httpOnly) {
                    cookieString.append("; HttpOnly");
                }

                // Set the cookie
                cookieManager.setCookie(url, cookieString.toString());
                Log.d(TAG, "✓ Setting cookie for URL \'" + url + "\': " + cookieString.toString());
                successCount++;

                // Also try setting for HTTP version if HTTPS fails
                if (!url.startsWith("http://")) {
                    String httpUrl = "http://" + cleanDomain;
                    cookieManager.setCookie(httpUrl, cookieString.toString());
                    Log.d(TAG, "✓ Also setting cookie for HTTP URL \'" + httpUrl + "\': " + cookieString.toString());
                }
                
                if (firstValidDomain[0] == null) {
                    firstValidDomain[0] = url;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error setting cookie: " + cookie.name + ", Domain: " + cookie.domain + ", Error: " + e.getMessage());
                errorCount++;
            }
        }
        
        cookieManager.flush(); // Ensure cookies are written to storage
        
        Log.d(TAG, "=== COOKIE IMPORT SUMMARY ===");
        Log.d(TAG, "Successfully imported: " + successCount + " cookies.");
        Log.d(TAG, "Failed to import: " + errorCount + " cookies.");
        
        if (errorCount > 0) {
            Toast.makeText(this, "Import completed with " + errorCount + " errors. Check logs for details.", Toast.LENGTH_LONG).show();
        }
        
        if (successCount > 0) {
            Toast.makeText(this, "Cookies imported successfully! Reloading page...", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(() -> {
                Intent resultIntent = new Intent();
                if (firstValidDomain[0] != null) {
                    resultIntent.putExtra("reload_url", firstValidDomain[0]);
                }
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }, 2000);
        } else {
            Toast.makeText(this, "No cookies were successfully imported.", Toast.LENGTH_LONG).show();
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private void verifyCookiesForUrl(String url) {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            String cookies = cookieManager.getCookie(url);
            if (cookies != null && !cookies.isEmpty()) {
                Log.d(TAG, "Verified cookies for " + url + ": " + cookies);
            } else {
                Log.d(TAG, "No cookies found for " + url);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verifying cookies: " + e.getMessage());
        }
    }

    @Override
    public void onCookieCheckedChanged(Cookie cookie, boolean isChecked) {
        // This method is called when a checkbox in the RecyclerView is changed.
        // We don\'t need to do anything specific here for the import logic,
        // as the import button will gather all selected cookies directly from the adapter.
        List<Cookie> filtered = cookieAdapter.getFilteredCookies();
        if (filtered.isEmpty()) {
            selectAllCheckBox.setChecked(false);
        } else {
            boolean allSelected = true;
            for (Cookie c : filtered) {
                if (!c.isSelected()) {
                    allSelected = false;
                    break;
                }
            }
            selectAllCheckBox.setChecked(allSelected);
        }
    }
}







