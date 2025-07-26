# Cookie Browser - Android Browser App with Cookie Management

A full-featured Android browser application built with Java that includes comprehensive cookie management features.

## Features

### Browser Features
- **Full WebView Browser**: Complete web browsing experience
- **Navigation Controls**: Home button, URL bar, Go button
- **Back Navigation**: Use device back button to navigate browser history
- **JavaScript Support**: Full JavaScript and DOM storage enabled
- **Responsive UI**: Modern Material Design interface

### Cookie Management Features
- **Export Cookies**: Export current website cookies to JSON file in Downloads folder
- **Import Cookies**: Import cookies from JSON file in Downloads folder
- **Clear Cookies**: Clear all cookies with confirmation dialog
- **Auto Cookie Handling**: Automatic cookie acceptance and management

## How to Use

### Basic Browsing
1. Open the app (starts with Google homepage)
2. Enter URL in the address bar or search directly
3. Use Home button (🏠) to return to Google
4. Use device back button to go back in browser history

### Cookie Management
1. **Export Cookies**:
   - Navigate to any website
   - Tap the menu (three dots) → "Export Cookies"
   - Cookies will be saved as JSON file in Downloads folder
   - Filename format: `cookies_export_[timestamp].json`

2. **Import Cookies**:
   - Tap menu → "Import Cookies"
   - Enter the filename from Downloads folder
   - Cookies will be imported and page will refresh

3. **Clear Cookies**:
   - Tap menu → "Clear Cookies"
   - Confirm to clear all cookies

4. **Refresh Page**:
   - Tap refresh icon in menu to reload current page

## Build Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 21+ (Android 5.0+)
- Java 8+

### Building the APK

#### Method 1: Using Android Studio
1. Open Android Studio
2. File → Open → Select the `BrowserApp` folder
3. Wait for Gradle sync to complete
4. Build → Generate Signed Bundle/APK
5. Choose APK and follow the signing process
6. APK will be generated in `app/build/outputs/apk/`

#### Method 2: Using Command Line
```bash
cd BrowserApp
./gradlew assembleDebug
```
APK location: `app/build/outputs/apk/debug/app-debug.apk`

#### Method 3: Release Build
```bash
cd BrowserApp
./gradlew assembleRelease
```

### Installation
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Technical Details

### Architecture
- **Language**: Java
- **UI Framework**: Android Views (not Flutter)
- **WebView**: Android WebView with WebKit
- **Cookie Storage**: Android CookieManager
- **JSON Processing**: Gson library
- **File Storage**: External storage (Downloads folder)

### Permissions
- `INTERNET`: Web browsing
- `ACCESS_NETWORK_STATE`: Network status
- `WRITE_EXTERNAL_STORAGE`: Save cookie files
- `READ_EXTERNAL_STORAGE`: Read cookie files

### Dependencies
- AndroidX AppCompat
- Material Design Components
- WebKit
- Gson for JSON processing

## File Structure
```
BrowserApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/browserapp/cookiemanager/
│   │   │   └── MainActivity.java
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── menu/main_menu.xml
│   │   │   ├── values/
│   │   │   └── drawable/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Troubleshooting

### Common Issues
1. **Build Errors**: Ensure Android SDK is properly installed
2. **Permission Denied**: Grant storage permissions in Android settings
3. **Cookie Import Fails**: Check file exists in Downloads folder
4. **WebView Issues**: Ensure device has WebView system component

### Debug Mode
Enable debug mode in `build.gradle` for detailed logging.

## License
MIT License - feel free to modify and distribute.

## Support
For issues and feature requests, please create an issue in the project repository.