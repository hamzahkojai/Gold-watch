plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.hamzah.goldwatch"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hamzah.goldwatch"
        minSdk = 30
        targetSdk = 34
        versionCode = 4
        versionName = "4.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
