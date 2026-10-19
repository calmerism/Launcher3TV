plugins {
    id("com.android.application")
}

android {
    namespace = "dev.home.tvlauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.home.tvlauncher"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "2026.10"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Personal sideload build: signed with the local debug key.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation("androidx.leanback:leanback-grid:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
}
