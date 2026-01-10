plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.levizack.gemini"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.levizack.gemini"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "GitHub 1.0.2 Release"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true /* changed from false in version 1.0.1 (sorry im tired making that at 4 am) */
            isShrinkResources = true /* changed from false in version 1.0.1 (sorry im tired making that at 4 am) */
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-splashscreen:1.0.1") /* added in version 1.0.1 (sorry im tired making that at 4 am) */
    implementation("androidx.webkit:webkit:1.15.0") /* added in version 1.0.1 (sorry im tired making that at 4 am) */
}

