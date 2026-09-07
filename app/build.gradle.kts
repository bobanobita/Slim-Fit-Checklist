plugins {
    id("com.android.application")
}

android {
    namespace = "vn.englishlogic.app"
    compileSdk = 35

    androidResources {
        // AAPT normally drops directories beginning with "_"; Next.js uses _next.
        ignoreAssetsPattern = ""
    }

    defaultConfig {
        applicationId = "vn.englishlogic.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
