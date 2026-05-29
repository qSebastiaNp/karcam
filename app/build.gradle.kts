import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.karcam"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.karcam"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "FTP_HOST", "\"${localProperties.getProperty("FTP_HOST") ?: ""}\"")
        buildConfigField("String", "FTP_PORT", "\"${localProperties.getProperty("FTP_PORT") ?: "21"}\"")
        buildConfigField("String", "FTP_USERNAME", "\"${localProperties.getProperty("FTP_USERNAME") ?: ""}\"")
        buildConfigField("String", "FTP_PASSWORD", "\"${localProperties.getProperty("FTP_PASSWORD") ?: ""}\"")
        buildConfigField("String", "FTP_DESTINATION_PATH", "\"${localProperties.getProperty("FTP_DESTINATION_PATH") ?: "/"}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("commons-net:commons-net:3.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
