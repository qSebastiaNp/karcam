plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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

        buildConfigField("String", "FTP_HOST", "\"${project.findProperty("FTP_HOST") ?: ""}\"")
        buildConfigField("String", "FTP_PORT", "\"${project.findProperty("FTP_PORT") ?: "21"}\"")
        buildConfigField("String", "FTP_USERNAME", "\"${project.findProperty("FTP_USERNAME") ?: ""}\"")
        buildConfigField("String", "FTP_PASSWORD", "\"${project.findProperty("FTP_PASSWORD") ?: ""}\"")
        buildConfigField("String", "FTP_DESTINATION_PATH", "\"${project.findProperty("FTP_DESTINATION_PATH") ?: "/"}\"")
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
    implementation("commons-net:commons-net:3.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
