# Build Instructions for Cookie Browser Android App

## Prerequisites

1. **Java Development Kit (JDK 8 or higher)**
   - Download from: https://adoptium.net/
   - Ensure JAVA_HOME environment variable is set

2. **Android Studio** (Recommended method)
   - Download from: https://developer.android.com/studio
   - Install Android SDK through Android Studio

3. **Android SDK** (Command line method)
   - Minimum SDK: API 21 (Android 5.0)
   - Target SDK: API 34 (Android 14)
   - Build Tools: 34.0.0 or higher

## Method 1: Building with Android Studio (Recommended)

1. **Install Android Studio**
   - Download and install Android Studio
   - Open Android Studio and complete the setup wizard
   - Install the Android SDK and build tools

2. **Open the Project**
   - Open Android Studio
   - Click "Open an existing Android Studio project"
   - Navigate to and select the `BrowserApp` folder
   - Wait for Gradle sync to complete

3. **Configure SDK Path**
   - Android Studio will automatically detect your SDK path
   - If prompted, update the `local.properties` file with your SDK location

4. **Build the APK**
   - Menu: Build → Generate Signed Bundle/APK
   - Choose "APK" → Next
   - Create a new keystore or use existing one
   - Fill in the keystore details
   - Choose "release" build variant
   - Click "Finish"

5. **Locate the APK**
   - APK will be generated in: `app/build/outputs/apk/release/`
   - Install using: `adb install app-release.apk`

## Method 2: Command Line Build (Linux/Mac/Windows)

### Setup Android SDK

1. **Download Command Line Tools**
   ```bash
   wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
   unzip commandlinetools-linux-9477386_latest.zip
   mkdir -p ~/Android/Sdk/cmdline-tools/latest
   mv cmdline-tools/* ~/Android/Sdk/cmdline-tools/latest/
   ```

2. **Set Environment Variables**
   ```bash
   export ANDROID_HOME=~/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   ```

3. **Install SDK Components**
   ```bash
   sdkmanager "platforms;android-34"
   sdkmanager "build-tools;34.0.0"
   sdkmanager "platform-tools"
   ```

### Build Process

1. **Navigate to Project Directory**
   ```bash
   cd BrowserApp
   ```

2. **Update local.properties**
   ```bash
   echo "sdk.dir=$ANDROID_HOME" > local.properties
   ```

3. **Build Debug APK**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Build Release APK**
   ```bash
   ./gradlew assembleRelease
   ```

5. **Locate APK Files**
   - Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
   - Release APK: `app/build/outputs/apk/release/app-release.apk`

## Method 3: Windows Specific Instructions

### Prerequisites for Windows

1. **Install Java JDK**
   - Download from Oracle or OpenJDK
   - Add to PATH: `C:\Program Files\Java\jdk-11\bin`
   - Set JAVA_HOME: `C:\Program Files\Java\jdk-11`

2. **Install Android Studio**
   - Download from: https://developer.android.com/studio
   - Default installation path: `C:\Users\%USERNAME%\AppData\Local\Android\Sdk`

### Build Commands (Windows)

```cmd
cd BrowserApp
gradlew.bat assembleDebug
```

## Signing the APK (For Distribution)

### Create Keystore
```bash
keytool -genkey -v -keystore browser-app-key.keystore -alias browser-app -keyalg RSA -keysize 2048 -validity 10000
```

### Sign APK
```bash
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore browser-app-key.keystore app-release-unsigned.apk browser-app
zipalign -v 4 app-release-unsigned.apk BrowserApp-signed.apk
```

## Installation

### Using ADB
```bash
adb install app-debug.apk
```

### Manual Installation
1. Copy APK to Android device
2. Enable "Unknown Sources" in device settings
3. Tap the APK file to install

## Troubleshooting

### Common Issues

1. **Gradle Sync Failed**
   - Check internet connection
   - Update Gradle version in `gradle/wrapper/gradle-wrapper.properties`
   - Clean project: `./gradlew clean`

2. **SDK Not Found**
   - Verify `local.properties` file has correct `sdk.dir` path
   - Ensure Android SDK is properly installed

3. **Build Tools Missing**
   ```bash
   sdkmanager "build-tools;34.0.0"
   ```

4. **Java Version Issues**
   - Ensure Java 8 or higher is installed
   - Check JAVA_HOME environment variable

### Build Variants

- **Debug**: Contains debugging information, larger size
- **Release**: Optimized for distribution, smaller size

### Performance Tips

1. **Increase Gradle Memory**
   ```bash
   export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=512m"
   ```

2. **Enable Parallel Builds**
   - Add to `gradle.properties`: `org.gradle.parallel=true`

3. **Use Gradle Daemon**
   - Add to `gradle.properties`: `org.gradle.daemon=true`

## APK Information

- **Package Name**: `com.browserapp.cookiemanager`
- **Minimum Android Version**: 5.0 (API 21)
- **Target Android Version**: 14 (API 34)
- **App Name**: Cookie Browser
- **Permissions**: Internet, Storage, Network State

## Features Included

- Full WebView browser functionality
- Cookie export to JSON files
- Cookie import from JSON files
- Clear all cookies option
- Modern Material Design UI
- Navigation controls (Home, Back, Refresh)
- URL address bar with search functionality

## Final APK Size
- Debug APK: ~5-8 MB
- Release APK: ~3-5 MB (after optimization)

---

**Note**: Make sure to test the APK on different Android versions and devices before distribution.