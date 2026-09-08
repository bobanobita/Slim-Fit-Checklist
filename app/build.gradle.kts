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
        versionCode = 4
        versionName = "1.3.0"
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

dependencies {
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
