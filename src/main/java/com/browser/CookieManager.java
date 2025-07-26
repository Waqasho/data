package com.browser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.CookieHandler;
import java.net.CookieStore;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CookieManager {
    
    private final ObjectMapper objectMapper;
    private final java.net.CookieManager systemCookieManager;
    
    public CookieManager() {
        this.objectMapper = new ObjectMapper();
        this.systemCookieManager = new java.net.CookieManager();
        CookieHandler.setDefault(systemCookieManager);
    }
    
    /**
     * Export cookies to a JSON file
     */
    public void exportCookies(Stage parentStage) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Cookies");
            fileChooser.setInitialFileName("browser_cookies.json");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );
            
            File file = fileChooser.showSaveDialog(parentStage);
            if (file != null) {
                ArrayNode cookiesArray = objectMapper.createArrayNode();
                CookieStore cookieStore = systemCookieManager.getCookieStore();
                
                // Get all cookies from the cookie store
                List<HttpCookie> cookies = cookieStore.getCookies();
                
                for (HttpCookie cookie : cookies) {
                    ObjectNode cookieJson = objectMapper.createObjectNode();
                    cookieJson.put("name", cookie.getName());
                    cookieJson.put("value", cookie.getValue());
                    cookieJson.put("domain", cookie.getDomain() != null ? cookie.getDomain() : "");
                    cookieJson.put("path", cookie.getPath() != null ? cookie.getPath() : "/");
                    cookieJson.put("secure", cookie.getSecure());
                    cookieJson.put("httpOnly", cookie.isHttpOnly());
                    cookieJson.put("maxAge", cookie.getMaxAge());
                    cookieJson.put("version", cookie.getVersion());
                    
                    // Add creation timestamp
                    cookieJson.put("exportTimestamp", System.currentTimeMillis());
                    
                    cookiesArray.add(cookieJson);
                }
                
                // Create metadata
                ObjectNode exportData = objectMapper.createObjectNode();
                exportData.put("exportVersion", "1.0");
                exportData.put("exportDate", new Date().toString());
                exportData.put("browserName", "Modern Java Browser");
                exportData.put("cookieCount", cookies.size());
                exportData.set("cookies", cookiesArray);
                
                // Write to file
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, exportData);
                
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", 
                    "Successfully exported " + cookies.size() + " cookies to:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Failed", 
                "Failed to export cookies: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Import cookies from a JSON file
     */
    public void importCookies(Stage parentStage) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Import Cookies");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            
            File file = fileChooser.showOpenDialog(parentStage);
            if (file != null) {
                String jsonContent = new String(Files.readAllBytes(file.toPath()));
                JsonNode rootNode = objectMapper.readTree(jsonContent);
                
                int importedCount = 0;
                int failedCount = 0;
                
                // Check if it's our export format
                if (rootNode.has("cookies")) {
                    ArrayNode cookiesArray = (ArrayNode) rootNode.get("cookies");
                    
                    for (JsonNode cookieNode : cookiesArray) {
                        try {
                            HttpCookie cookie = createCookieFromJson(cookieNode);
                            if (cookie != null) {
                                // Add cookie to store
                                URI uri = URI.create("http://" + 
                                    (cookie.getDomain() != null ? cookie.getDomain() : "localhost"));
                                systemCookieManager.getCookieStore().add(uri, cookie);
                                importedCount++;
                            }
                        } catch (Exception e) {
                            failedCount++;
                            System.err.println("Failed to import cookie: " + e.getMessage());
                        }
                    }
                } else {
                    // Try to parse as simple cookie array
                    if (rootNode.isArray()) {
                        for (JsonNode cookieNode : rootNode) {
                            try {
                                HttpCookie cookie = createCookieFromJson(cookieNode);
                                if (cookie != null) {
                                    URI uri = URI.create("http://" + 
                                        (cookie.getDomain() != null ? cookie.getDomain() : "localhost"));
                                    systemCookieManager.getCookieStore().add(uri, cookie);
                                    importedCount++;
                                }
                            } catch (Exception e) {
                                failedCount++;
                                System.err.println("Failed to import cookie: " + e.getMessage());
                            }
                        }
                    }
                }
                
                String message = "Import completed!\n" +
                    "Successfully imported: " + importedCount + " cookies\n";
                if (failedCount > 0) {
                    message += "Failed to import: " + failedCount + " cookies";
                }
                
                showAlert(Alert.AlertType.INFORMATION, "Import Completed", message);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Import Failed", 
                "Failed to import cookies: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clear all cookies
     */
    public void clearAllCookies(Stage parentStage) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Clear All Cookies");
        confirmAlert.setHeaderText("Are you sure?");
        confirmAlert.setContentText("This will permanently delete all stored cookies. This action cannot be undone.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                CookieStore cookieStore = systemCookieManager.getCookieStore();
                int cookieCount = cookieStore.getCookies().size();
                cookieStore.removeAll();
                
                showAlert(Alert.AlertType.INFORMATION, "Cookies Cleared", 
                    "Successfully cleared " + cookieCount + " cookies.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Clear Failed", 
                    "Failed to clear cookies: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get current cookie count
     */
    public int getCookieCount() {
        return systemCookieManager.getCookieStore().getCookies().size();
    }
    
    /**
     * Get cookies for a specific domain
     */
    public List<HttpCookie> getCookiesForDomain(String domain) {
        List<HttpCookie> result = new ArrayList<>();
        CookieStore cookieStore = systemCookieManager.getCookieStore();
        
        for (HttpCookie cookie : cookieStore.getCookies()) {
            if (cookie.getDomain() != null && 
                (cookie.getDomain().equals(domain) || cookie.getDomain().endsWith("." + domain))) {
                result.add(cookie);
            }
        }
        
        return result;
    }
    
    private HttpCookie createCookieFromJson(JsonNode cookieNode) {
        try {
            String name = cookieNode.get("name").asText();
            String value = cookieNode.get("value").asText();
            
            HttpCookie cookie = new HttpCookie(name, value);
            
            if (cookieNode.has("domain") && !cookieNode.get("domain").asText().isEmpty()) {
                cookie.setDomain(cookieNode.get("domain").asText());
            }
            
            if (cookieNode.has("path")) {
                cookie.setPath(cookieNode.get("path").asText());
            }
            
            if (cookieNode.has("secure")) {
                cookie.setSecure(cookieNode.get("secure").asBoolean());
            }
            
            if (cookieNode.has("httpOnly")) {
                cookie.setHttpOnly(cookieNode.get("httpOnly").asBoolean());
            }
            
            if (cookieNode.has("maxAge")) {
                cookie.setMaxAge(cookieNode.get("maxAge").asLong());
            }
            
            if (cookieNode.has("version")) {
                cookie.setVersion(cookieNode.get("version").asInt());
            }
            
            return cookie;
        } catch (Exception e) {
            System.err.println("Error creating cookie from JSON: " + e.getMessage());
            return null;
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Export cookies in Chrome format (for compatibility)
     */
    public void exportCookiesChrome(Stage parentStage) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Cookies (Chrome Format)");
            fileChooser.setInitialFileName("cookies_chrome_format.json");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );
            
            File file = fileChooser.showSaveDialog(parentStage);
            if (file != null) {
                ArrayNode cookiesArray = objectMapper.createArrayNode();
                CookieStore cookieStore = systemCookieManager.getCookieStore();
                
                for (HttpCookie cookie : cookieStore.getCookies()) {
                    ObjectNode cookieJson = objectMapper.createObjectNode();
                    cookieJson.put("domain", cookie.getDomain() != null ? cookie.getDomain() : "");
                    cookieJson.put("expirationDate", System.currentTimeMillis() / 1000 + cookie.getMaxAge());
                    cookieJson.put("hostOnly", false);
                    cookieJson.put("httpOnly", cookie.isHttpOnly());
                    cookieJson.put("name", cookie.getName());
                    cookieJson.put("path", cookie.getPath() != null ? cookie.getPath() : "/");
                    cookieJson.put("secure", cookie.getSecure());
                    cookieJson.put("session", cookie.getMaxAge() == -1);
                    cookieJson.put("storeId", "0");
                    cookieJson.put("value", cookie.getValue());
                    
                    cookiesArray.add(cookieJson);
                }
                
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, cookiesArray);
                
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", 
                    "Successfully exported " + cookiesArray.size() + " cookies in Chrome format to:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Failed", 
                "Failed to export cookies: " + e.getMessage());
            e.printStackTrace();
        }
    }
}