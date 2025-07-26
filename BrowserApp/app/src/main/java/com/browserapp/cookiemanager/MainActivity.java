package com.browserapp.cookiemanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.CookieManager;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlEditText;
    private Button goButton, homeButton;
    private CookieManager cookieManager;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupWebView();
        setupButtons();
        
        gson = new Gson();
        cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
    }

    private void initializeViews() {
        webView = findViewById(R.id.webView);
        urlEditText = findViewById(R.id.urlEditText);
        goButton = findViewById(R.id.goButton);
        homeButton = findViewById(R.id.homeButton);
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                urlEditText.setText(url);
            }
        });

        // Load homepage
        webView.loadUrl("https://www.google.com");
    }

    private void setupButtons() {
        goButton.setOnClickListener(v -> {
            String url = urlEditText.getText().toString().trim();
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                webView.loadUrl(url);
            }
        });

        homeButton.setOnClickListener(v -> {
            webView.loadUrl("https://www.google.com");
            urlEditText.setText("https://www.google.com");
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_export_cookies) {
            exportCookies();
            return true;
        } else if (id == R.id.action_import_cookies) {
            importCookies();
            return true;
        } else if (id == R.id.action_clear_cookies) {
            clearAllCookies();
            return true;
        } else if (id == R.id.action_refresh) {
            webView.reload();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void exportCookies() {
        try {
            Map<String, String> cookieMap = new HashMap<>();
            String cookieString = cookieManager.getCookie(webView.getUrl());
            
            if (cookieString != null && !cookieString.isEmpty()) {
                String[] cookies = cookieString.split(";");
                for (String cookie : cookies) {
                    String[] parts = cookie.trim().split("=", 2);
                    if (parts.length == 2) {
                        cookieMap.put(parts[0], parts[1]);
                    }
                }
            }

            String jsonCookies = gson.toJson(cookieMap);
            String fileName = "cookies_export_" + System.currentTimeMillis() + ".json";
            
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);
            
            FileWriter writer = new FileWriter(file);
            writer.write(jsonCookies);
            writer.close();
            
            Toast.makeText(this, "Cookies exported to Downloads/" + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error exporting cookies: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void importCookies() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Import Cookies");
        builder.setMessage("Enter the filename from Downloads folder (e.g., cookies_export_123456.json):");
        
        final EditText input = new EditText(this);
        input.setHint("filename.json");
        builder.setView(input);
        
        builder.setPositiveButton("Import", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = input.getText().toString().trim();
                if (!fileName.isEmpty()) {
                    importCookiesFromFile(fileName);
                }
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void importCookiesFromFile(String fileName) {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);
            
            if (!file.exists()) {
                Toast.makeText(this, "File not found in Downloads folder", Toast.LENGTH_SHORT).show();
                return;
            }
            
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();
            
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> cookieMap = gson.fromJson(jsonBuilder.toString(), type);
            
            String currentUrl = webView.getUrl();
            if (currentUrl != null) {
                for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
                    String cookieValue = entry.getKey() + "=" + entry.getValue();
                    cookieManager.setCookie(currentUrl, cookieValue);
                }
                cookieManager.flush();
                
                Toast.makeText(this, "Cookies imported successfully! Refreshing page...", Toast.LENGTH_SHORT).show();
                webView.reload();
            } else {
                Toast.makeText(this, "Please navigate to a website first", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Toast.makeText(this, "Error importing cookies: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void clearAllCookies() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Cookies");
        builder.setMessage("Are you sure you want to clear all cookies?");
        
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cookieManager.removeAllCookies(null);
                cookieManager.flush();
                Toast.makeText(MainActivity.this, "All cookies cleared", Toast.LENGTH_SHORT).show();
                webView.reload();
            }
        });
        
        builder.setNegativeButton("No", null);
        builder.show();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}