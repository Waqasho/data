# Modern Java Browser

یہ ایک modern Java browser application ہے جو JavaFX کے ساتھ بنایا گیا ہے۔ اس میں cookies import/export کی facility ہے اور modern UI design ہے۔

## Features / خصوصیات

### ✨ Browser Features
- **Modern UI**: Dark theme کے ساتھ material design
- **Tab Support**: Multiple tabs کا support
- **Navigation**: Back, Forward, Refresh, Home buttons
- **Smart URL Bar**: URL یا search terms enter کر سکتے ہیں
- **Status Bar**: Page loading status اور cookie information

### 🍪 Cookie Management
- **Import Cookies**: JSON files سے cookies import کریں
- **Export Cookies**: Cookies کو JSON format میں export کریں  
- **Chrome Format**: Chrome browser کے compatible format میں export
- **Clear Cookies**: تمام cookies کو clear کرنے کا option
- **Real-time Count**: Current cookie count display

### 🎨 Modern UI Features
- **Dark Theme**: Professional dark color scheme
- **Material Icons**: Beautiful material design icons
- **Hover Effects**: Interactive button animations
- **Progress Indicators**: Loading progress display
- **Responsive Design**: Window resizing support

## Requirements / ضروریات

- **Java 17** یا اس سے اوپر
- **Maven 3.6+** (dependency management کے لیے)
- **JavaFX 19** (automatically downloaded via Maven)

## Installation / تنصیب

### 1. Repository Clone کریں
```bash
git clone <repository-url>
cd modern-java-browser
```

### 2. Dependencies Install کریں
```bash
mvn clean install
```

### 3. Application Run کریں
```bash
mvn javafx:run
```

یا compiled JAR file run کریں:
```bash
mvn clean package
java -jar target/modern-java-browser-1.0.0.jar
```

## Usage Guide / استعمال کی رہنمائی

### Basic Navigation / بنیادی ہدایات

1. **URL Enter کریں**: Address bar میں website URL یا search term type کریں
2. **Navigation**: Back/Forward buttons استعمال کریں
3. **New Tab**: Plus (+) button دبا کر نیا tab کھولیں
4. **Refresh**: Current page کو reload کرنے کے لیے refresh button استعمال کریں

### Cookie Management / کوکی کا انتظام

#### Import Cookies
1. Cookie menu (🍪 icon) پر click کریں
2. "Import Cookies" select کریں
3. JSON file choose کریں
4. Cookies automatically import ہو جائیں گے

#### Export Cookies
1. Cookie menu پر click کریں
2. "Export Cookies" select کریں
3. Save location choose کریں
4. Cookies JSON format میں save ہو جائیں گے

#### Clear Cookies
1. Cookie menu پر click کریں
2. "Clear All Cookies" select کریں
3. Confirmation dialog میں OK کریں

### Supported Cookie Formats / سپورٹ شدہ کوکی فارمیٹس

#### Export Format
```json
{
  "exportVersion": "1.0",
  "exportDate": "Date",
  "browserName": "Modern Java Browser",
  "cookieCount": 5,
  "cookies": [
    {
      "name": "cookie_name",
      "value": "cookie_value",
      "domain": "example.com",
      "path": "/",
      "secure": false,
      "httpOnly": true,
      "maxAge": 3600,
      "version": 0,
      "exportTimestamp": 1234567890
    }
  ]
}
```

#### Chrome Compatible Format
Chrome browser سے export کی گئی cookies بھی import کر سکتے ہیں۔

## Keyboard Shortcuts / کی بورڈ شارٹ کٹس

- **Enter** (in URL bar): Navigate to URL
- **Ctrl+T**: New Tab (planned feature)
- **Ctrl+W**: Close Tab (planned feature)
- **F5**: Refresh Page (planned feature)

## Technical Details / تکنیکی تفصیلات

### Architecture / آرکیٹیکچر
- **JavaFX**: UI framework
- **Maven**: Build tool اور dependency management
- **Jackson**: JSON processing
- **Ikonli**: Material design icons

### Dependencies / انحصارات
```xml
<!-- JavaFX -->
org.openjfx:javafx-controls:19
org.openjfx:javafx-web:19
org.openjfx:javafx-fxml:19

<!-- JSON Processing -->
com.fasterxml.jackson.core:jackson-databind:2.15.2

<!-- Icons -->
org.kordamp.ikonli:ikonli-materialdesign2-pack:12.3.1
```

### File Structure / فائل کی ساخت
```
src/
├── main/
│   ├── java/com/browser/
│   │   ├── ModernBrowserApp.java      # Main application class
│   │   └── CookieManager.java         # Cookie management logic
│   └── resources/
│       └── styles.css                 # UI styling
├── pom.xml                           # Maven configuration
└── README.md                         # Documentation
```

## Troubleshooting / مسائل کا حل

### Common Issues / عام مسائل

1. **JavaFX Module Path Error**
   ```bash
   # Solution: Use Maven JavaFX plugin
   mvn javafx:run
   ```

2. **Styles not loading**
   - Check if `styles.css` exists in `src/main/resources/`
   - CSS errors will be printed to console

3. **Cookie Import/Export Issues**
   - Ensure JSON file format is correct
   - Check file permissions
   - Verify JSON syntax

### Performance Tips / کارکردگی کی تجاویز

- Use SSD for better application startup time
- Allocate more memory for large cookie files:
  ```bash
  java -Xmx512m -jar target/modern-java-browser-1.0.0.jar
  ```

## Future Enhancements / مستقبل کی بہتری

- [ ] Bookmarks management
- [ ] Download manager
- [ ] History tracking
- [ ] Extensions support
- [ ] Private browsing mode
- [ ] Cookie editor
- [ ] Password manager integration

## Contributing / تعاون

1. Fork the repository
2. Create feature branch
3. Make changes
4. Test thoroughly
5. Submit pull request

## License / لائسنس

This project is open source and available under the MIT License.

## Support / سپورٹ

اگر کوئی مسئلہ ہو تو issue create کریں یا documentation چیک کریں۔

---

**Note**: یہ browser basic functionality provide کرتا ہے اور production use کے لیے additional security features کی ضرورت ہو سکتی ہے۔