package com.browser;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;

import java.io.File;

public class ModernBrowserApp extends Application {
    
    private WebView webView;
    private WebEngine webEngine;
    private TextField urlField;
    private Button backButton, forwardButton, refreshButton, homeButton;
    private ProgressBar progressBar;
    private Label statusLabel;
    private CookieManager cookieManager;
    private TabPane tabPane;
    private Stage primaryStage;
    
    private static final String HOME_URL = "https://www.google.com";
    private static final String DEFAULT_TITLE = "Modern Java Browser";

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle(DEFAULT_TITLE);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        
        // Initialize cookie manager
        cookieManager = new CookieManager();
        
        // Create main layout
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2b2b2b;");
        
        // Create top toolbar
        HBox toolbar = createToolbar();
        root.setTop(toolbar);
        
        // Create tab pane
        tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: #3c3c3c; -fx-border-color: #555;");
        
        // Add initial tab
        addNewTab();
        
        root.setCenter(tabPane);
        
        // Create status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);
        
        // Setup scene
        Scene scene = new Scene(root, 1200, 800);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load styles.css: " + e.getMessage());
        }
        
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Load home page
        webEngine.load(HOME_URL);
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #404040; -fx-border-color: #555; -fx-border-width: 0 0 1 0;");
        
        // Navigation buttons
        backButton = createIconButton("mdi2a-arrow-left", "Back");
        forwardButton = createIconButton("mdi2a-arrow-right", "Forward");
        refreshButton = createIconButton("mdi2r-refresh", "Refresh");
        homeButton = createIconButton("mdi2h-home", "Home");
        
        // URL field
        urlField = new TextField();
        urlField.setPromptText("Enter URL or search term...");
        urlField.setPrefWidth(400);
        urlField.getStyleClass().add("url-field");
        HBox.setHgrow(urlField, Priority.ALWAYS);
        
        // Menu button for cookies
        MenuButton cookieMenu = new MenuButton();
        cookieMenu.setGraphic(new FontIcon("mdi2c-cookie"));
        cookieMenu.getStyleClass().add("menu-button");
        cookieMenu.setTooltip(new Tooltip("Cookie Management"));
        
        MenuItem importCookies = new MenuItem("Import Cookies");
        importCookies.setGraphic(new FontIcon("mdi2i-import"));
        importCookies.setOnAction(e -> cookieManager.importCookies(primaryStage));
        
        MenuItem exportCookies = new MenuItem("Export Cookies");
        exportCookies.setGraphic(new FontIcon("mdi2e-export"));
        exportCookies.setOnAction(e -> cookieManager.exportCookies(primaryStage));
        
        MenuItem clearCookies = new MenuItem("Clear All Cookies");
        clearCookies.setGraphic(new FontIcon("mdi2d-delete"));
        clearCookies.setOnAction(e -> cookieManager.clearAllCookies(primaryStage));
        
        cookieMenu.getItems().addAll(importCookies, exportCookies, new SeparatorMenuItem(), clearCookies);
        
        // New tab button
        Button newTabButton = createIconButton("mdi2p-plus", "New Tab");
        newTabButton.setOnAction(e -> addNewTab());
        
        toolbar.getChildren().addAll(
            backButton, forwardButton, refreshButton, homeButton,
            new Separator(), urlField, cookieMenu, newTabButton
        );
        
        return toolbar;
    }
    
    private Button createIconButton(String icon, String tooltip) {
        Button button = new Button();
        button.setGraphic(new FontIcon(icon));
        button.setTooltip(new Tooltip(tooltip));
        button.getStyleClass().add("nav-button");
        return button;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #353535; -fx-border-color: #555; -fx-border-width: 1 0 0 0;");
        
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #ccc;");
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(150);
        progressBar.setVisible(false);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label cookieLabel = new Label("Cookies: Enabled");
        cookieLabel.setStyle("-fx-text-fill: #ccc;");
        
        statusBar.getChildren().addAll(statusLabel, progressBar, spacer, cookieLabel);
        
        return statusBar;
    }
    
    private void addNewTab() {
        Tab tab = new Tab("New Tab");
        tab.setClosable(true);
        
        // Create web view for this tab
        WebView tabWebView = new WebView();
        WebEngine tabWebEngine = tabWebView.getEngine();
        
        // Set current active web view and engine
        if (tabPane.getTabs().isEmpty()) {
            webView = tabWebView;
            webEngine = tabWebEngine;
            setupWebEngine();
        }
        
        tab.setContent(tabWebView);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        
        // Handle tab selection
        tab.setOnSelectionChanged(e -> {
            if (tab.isSelected()) {
                webView = tabWebView;
                webEngine = tabWebEngine;
                updateUrlField();
            }
        });
        
        // Handle tab closing
        tab.setOnClosed(e -> {
            if (tabPane.getTabs().isEmpty()) {
                addNewTab();
            }
        });
    }
    
    private void setupWebEngine() {
        // Setup navigation button handlers
        backButton.setOnAction(e -> {
            if (webEngine.getHistory().getCurrentIndex() > 0) {
                webEngine.getHistory().go(-1);
            }
        });
        
        forwardButton.setOnAction(e -> {
            if (webEngine.getHistory().getCurrentIndex() < webEngine.getHistory().getEntries().size() - 1) {
                webEngine.getHistory().go(1);
            }
        });
        
        refreshButton.setOnAction(e -> webEngine.reload());
        homeButton.setOnAction(e -> webEngine.load(HOME_URL));
        
        // Setup URL field
        urlField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                navigateToUrl(urlField.getText());
            }
        });
        
        // Setup web engine listeners
        webEngine.locationProperty().addListener((obs, oldLocation, newLocation) -> {
            urlField.setText(newLocation);
            updateNavigationButtons();
        });
        
        webEngine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
            if (currentTab != null) {
                currentTab.setText(newTitle != null && !newTitle.isEmpty() ? newTitle : "New Tab");
            }
        });
        
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            switch (newState) {
                case SCHEDULED:
                    progressBar.setVisible(true);
                    statusLabel.setText("Loading...");
                    break;
                case RUNNING:
                    progressBar.setProgress(-1);
                    break;
                case SUCCEEDED:
                    progressBar.setVisible(false);
                    statusLabel.setText("Page loaded successfully");
                    break;
                case FAILED:
                    progressBar.setVisible(false);
                    statusLabel.setText("Failed to load page");
                    break;
            }
        });
    }
    
    private void navigateToUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        
        url = url.trim();
        
        // Add protocol if missing
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://" + url;
            } else {
                // Treat as search query
                url = "https://www.google.com/search?q=" + url.replace(" ", "+");
            }
        }
        
        webEngine.load(url);
    }
    
    private void updateUrlField() {
        if (webEngine != null) {
            String location = webEngine.getLocation();
            if (location != null) {
                urlField.setText(location);
            }
        }
    }
    
    private void updateNavigationButtons() {
        if (webEngine != null) {
            backButton.setDisable(webEngine.getHistory().getCurrentIndex() <= 0);
            forwardButton.setDisable(webEngine.getHistory().getCurrentIndex() >= webEngine.getHistory().getEntries().size() - 1);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}