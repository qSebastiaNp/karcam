# KarCam

A simple Android application for employees to capture and upload photos via FTP.

## Features

- Opens directly to the camera interface
- Photo preview with **Confirm** and **Retake** options
- Automatic FTP upload on confirmation
- Success/error feedback before auto-closing

## Building the APK on Windows 11

### Prerequisites

1. **Java Development Kit (JDK) 17**
   ```powershell
   # Install via Chocolatey
   choco install temurin17 -y
   ```

2. **Android SDK**
   ```powershell
   # Download Android command-line tools
   Invoke-WebRequest -Uri "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" -OutFile "$env:USERPROFILE\cmdline-tools.zip"

   # Extract and set up
   $sdkRoot = "$env:USERPROFILE\Android\Sdk"
   mkdir -Force "$sdkRoot\cmdline-tools"
   Expand-Archive "$env:USERPROFILE\cmdline-tools.zip" "$sdkRoot\cmdline-tools-temp"
   Move-Item "$sdkRoot\cmdline-tools-temp\cmdline-tools" "$sdkRoot\cmdline-tools\latest"

   # Set environment variables
   [Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdkRoot, "User")
   $env:ANDROID_HOME = $sdkRoot
   $env:PATH = "$sdkRoot\cmdline-tools\latest\bin;$sdkRoot\platform-tools;$env:PATH"

   # Accept licenses and install required packages
   "y`ny`ny`ny`ny`ny`ny`ny`ny`ny`ny`n" | sdkmanager --licenses
   sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
   ```

### Build Steps

1. **Clone the repository**
   ```powershell
   git clone https://github.com/qSebastiaNp/karcam.git
   cd karcam
   ```

2. **Create `local.properties`** (point to your Android SDK)
   ```powershell
   Set-Content -Path local.properties -Value "sdk.dir=$($env:ANDROID_HOME -replace '\\', '/')"
   ```

3. **Build the debug APK**
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   The APK will be at: `app\build\outputs\apk\debug\app-debug.apk`

4. **Build a release APK** (optional — unsigned)
   ```powershell
   .\gradlew.bat assembleRelease
   ```

### Installing on Devices

Transfer the APK to each device and install:
```powershell
# Via ADB (if device is connected via USB with USB debugging enabled)
adb install app\build\outputs\apk\debug\app-debug.apk
```

Or copy the APK file to the device via USB/email/file share and open it to install (enable "Install from unknown sources" in device settings).

## FTP Configuration

FTP credentials are injected at build time via Gradle properties. Create or edit `gradle.properties` in the project root (or in `~/.gradle/gradle.properties` for global config):

```properties
FTP_HOST=your.ftp.server.com
FTP_PORT=21
FTP_USERNAME=your_username
FTP_PASSWORD=your_password
FTP_DESTINATION_PATH=/upload/photos
```

These values are compiled into `BuildConfig` fields and are **not** stored in the repository.

## Project Structure

```
karcam/
├── app/
│   ├── src/main/
│   │   ├── java/com/karcam/
│   │   │   ├── MainActivity.kt      # Camera + preview UI
│   │   │   └── FtpUploader.kt       # FTP upload logic
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── values/              # strings, colors, themes
│   │   │   ├── drawable/            # icons
│   │   │   └── xml/file_paths.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## Requirements

- **Min SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)
- **Build tools**: JDK 17, Gradle 8.5, AGP 8.2.2
