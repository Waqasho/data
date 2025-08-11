package com.naxobrowser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class CookieManagerActivity extends AppCompatActivity {

    private static final int PICK_JSON_FILE = 1;
    private static final int PICK_NETSCAPE_FILE = 2;
    private String currentUrl;
    private TextView currentUrlTextView;
    private LinearLayout cookieListLayout;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cookie_manager);

        try {
            currentUrl = getIntent().getStringExtra("current_url");
            currentUrlTextView = findViewById(R.id.currentUrlTextView);
            cookieListLayout = findViewById(R.id.cookieListLayout);
            scrollView = findViewById(R.id.scrollView);

            if (currentUrl != null && !currentUrl.isEmpty()) {
                currentUrlTextView.setText("Cookies for: " + currentUrl);
                loadAndDisplayCookies();
            } else {
                currentUrlTextView.setText("No URL provided. Please visit a website first.");
                // Show a default message in the cookie list
                showNoCookiesMessage("Please visit a website first to view cookies.");
            }

            Button importJsonButton = findViewById(R.id.importJsonButton);
            Button exportJsonButton = findViewById(R.id.exportJsonButton);
            Button importNetscapeButton = findViewById(R.id.importNetscapeButton);
            Button exportNetscapeButton = findViewById(R.id.exportNetscapeButton);
            Button refreshButton = findViewById(R.id.refreshButton);

            if (importJsonButton != null) importJsonButton.setOnClickListener(v -> openFilePicker(PICK_JSON_FILE));
            if (exportJsonButton != null) exportJsonButton.setOnClickListener(v -> exportCookiesToJson());
            if (importNetscapeButton != null) importNetscapeButton.setOnClickListener(v -> openFilePicker(PICK_NETSCAPE_FILE));
            if (exportNetscapeButton != null) exportNetscapeButton.setOnClickListener(v -> exportCookiesToNetscape());
            if (refreshButton != null) refreshButton.setOnClickListener(v -> loadAndDisplayCookies());
            
            // Add clear cookies button functionality
            Button clearCookiesButton = findViewById(R.id.clearCookiesButton);
            if (clearCookiesButton != null) {
                clearCookiesButton.setOnClickListener(v -> clearAllCookies());
            }
            
            // Add close button functionality
            Button closeButton = findViewById(R.id.closeButton);
            if (closeButton != null) {
                closeButton.setOnClickListener(v -> finish());
            }
            
        } catch (Exception e) {
            Log.e("CookieManager", "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing Cookie Manager: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadAndDisplayCookies() {
        try {
            if (currentUrl == null || currentUrl.isEmpty()) {
                showNoCookiesMessage("No URL available");
                return;
            }

            String domain = getDomainFromUrl(currentUrl);
            if (domain == null) {
                showNoCookiesMessage("Invalid URL");
                return;
            }

            CookieManager cookieManager = CookieManager.getInstance();
            String cookies = cookieManager.getCookie(currentUrl);

            if (cookieListLayout != null) {
                cookieListLayout.removeAllViews();
            }

            if (cookies == null || cookies.isEmpty()) {
                showNoCookiesMessage("No cookies found for this domain");
                return;
            }

            // Parse and display individual cookies
            String[] cookiePairs = cookies.split("; ");
            for (String cookiePair : cookiePairs) {
                if (!cookiePair.trim().isEmpty()) {
                    String[] parts = cookiePair.split("=", 2);
                    if (parts.length == 2) {
                        addCookieView(parts[0], parts[1]);
                    } else if (parts.length == 1) {
                        addCookieView(parts[0], "");
                    }
                }
            }
        } catch (Exception e) {
            Log.e("CookieManager", "Error loading cookies: " + e.getMessage(), e);
            showNoCookiesMessage("Error loading cookies: " + e.getMessage());
        }
    }

    private void addCookieView(String name, String value) {
        try {
            if (cookieListLayout == null) {
                Log.e("CookieManager", "cookieListLayout is null");
                return;
            }
            
            TextView cookieText = new TextView(this);
            cookieText.setText("Name: " + name + "\nValue: " + value);
            cookieText.setPadding(20, 10, 20, 10);
            cookieText.setBackgroundResource(android.R.color.darker_gray);
            cookieText.setTextColor(android.R.color.white);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(10, 5, 10, 5);
            cookieText.setLayoutParams(params);
            
            cookieListLayout.addView(cookieText);
        } catch (Exception e) {
            Log.e("CookieManager", "Error adding cookie view: " + e.getMessage(), e);
        }
    }

    private void openFilePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                if (requestCode == PICK_JSON_FILE) {
                    importCookiesFromJson(uri);
                } else if (requestCode == PICK_NETSCAPE_FILE) {
                    importCookiesFromNetscape(uri);
                }
            }
        }
    }

    private String getDomainFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getHost();
        } catch (Exception e) {
            Log.e("CookieManager", "Error parsing URL: " + urlString, e);
            return null;
        }
    }

    private void importCookiesFromJson(Uri uri) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            reader.close();

            // Launch CookieImportActivity with the cookie data
            Intent intent = new Intent(this, CookieImportActivity.class);
            intent.putExtra("cookie_data", stringBuilder.toString());
            intent.putExtra("cookie_format", "json");
            startActivity(intent);

        } catch (IOException e) {
            Log.e("CookieManager", "Error reading JSON cookies", e);
            Toast.makeText(this, "Failed to read cookies from file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportCookiesToJson() {
        try {
            if (currentUrl == null || currentUrl.isEmpty()) {
                Toast.makeText(this, "No URL available for export", Toast.LENGTH_SHORT).show();
                return;
            }

            CookieManager cookieManager = CookieManager.getInstance();
            String cookies = cookieManager.getCookie(currentUrl);

            if (cookies == null || cookies.isEmpty()) {
                Toast.makeText(this, "No cookies to export", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject jsonObject = new JSONObject();
            String[] cookiePairs = cookies.split("; ");
            for (String cookiePair : cookiePairs) {
                String[] parts = cookiePair.split("=", 2);
                if (parts.length == 2) {
                    jsonObject.put(parts[0], parts[1]);
                }
            }

            String domain = getDomainFromUrl(currentUrl);
            if (domain == null) {
                Toast.makeText(this, "Invalid domain for export", Toast.LENGTH_SHORT).show();
                return;
            }

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!path.exists() && !path.mkdirs()) {
                Toast.makeText(this, "Cannot create Downloads directory", Toast.LENGTH_SHORT).show();
                return;
            }

            File file = new File(path, domain.replace(".", "_") + "_cookies.json");
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(jsonObject.toString(4));
            writer.close();
            fos.close();
            Toast.makeText(this, "Cookies exported to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException | JSONException e) {
            Log.e("CookieManager", "Error exporting JSON cookies", e);
            Toast.makeText(this, "Failed to export cookies: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("CookieManager", "Unexpected error exporting JSON cookies", e);
            Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void importCookiesFromNetscape(Uri uri) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            reader.close();

            Intent intent = new Intent(this, CookieImportActivity.class);
            intent.putExtra("cookie_data", stringBuilder.toString());
            intent.putExtra("cookie_format", "netscape");
            startActivity(intent);

        } catch (IOException e) {
            Log.e("CookieManager", "Error reading Netscape cookies", e);
            Toast.makeText(this, "Failed to read cookies from file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportCookiesToNetscape() {
        try {
            if (currentUrl == null || currentUrl.isEmpty()) {
                Toast.makeText(this, "No URL available for export", Toast.LENGTH_SHORT).show();
                return;
            }

            CookieManager cookieManager = CookieManager.getInstance();
            String cookies = cookieManager.getCookie(currentUrl);

            if (cookies == null || cookies.isEmpty()) {
                Toast.makeText(this, "No cookies to export", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder netscapeFormat = new StringBuilder();
            netscapeFormat.append("# Netscape HTTP Cookie File\n");
            netscapeFormat.append("# This is a generated file! Do not edit.\n\n");

            String domain = getDomainFromUrl(currentUrl);
            if (domain == null) {
                Toast.makeText(this, "Invalid domain for export", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] cookiePairs = cookies.split("; ");
            for (String cookiePair : cookiePairs) {
                String[] parts = cookiePair.split("=", 2);
                if (parts.length == 2) {
                    netscapeFormat.append(domain).append("\tTRUE\t/\tFALSE\t0\t").append(parts[0]).append("\t").append(parts[1]).append("\n");
                }
            }

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!path.exists() && !path.mkdirs()) {
                Toast.makeText(this, "Cannot create Downloads directory", Toast.LENGTH_SHORT).show();
                return;
            }

            File file = new File(path, domain.replace(".", "_") + "_cookies.txt");
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(netscapeFormat.toString());
            writer.close();
            fos.close();
            Toast.makeText(this, "Cookies exported to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("CookieManager", "Error exporting Netscape cookies", e);
            Toast.makeText(this, "Failed to export cookies: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("CookieManager", "Unexpected error exporting Netscape cookies", e);
            Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showNoCookiesMessage(String message) {
        try {
            if (cookieListLayout != null) {
                TextView noCookiesText = new TextView(this);
                noCookiesText.setText(message);
                noCookiesText.setPadding(20, 20, 20, 20);
                cookieListLayout.addView(noCookiesText);
            }
        } catch (Exception e) {
            Log.e("CookieManager", "Error showing no cookies message: " + e.getMessage(), e);
        }
    }
    
    private void clearAllCookies() {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
            
            Toast.makeText(this, "All cookies cleared successfully", Toast.LENGTH_SHORT).show();
            loadAndDisplayCookies(); // Refresh the display
        } catch (Exception e) {
            Log.e("CookieManager", "Error clearing cookies: " + e.getMessage(), e);
            Toast.makeText(this, "Error clearing cookies: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

