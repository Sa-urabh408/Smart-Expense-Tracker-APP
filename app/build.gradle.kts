plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.demo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.viewpager2)
    implementation(libs.fragment)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // WorkManager
    implementation("androidx.work:work-runtime:2.9.0")
    implementation("com.google.guava:guava:31.1-android")

    // Room Database
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Charts
    implementation(libs.mpandroidchart)

    // PDF Export
    implementation(libs.itextpdf)

    // Glide - Image loading and circular transformations
    implementation(libs.glide)

    // ML Kit - Offline OCR text recognition (no API key needed)
    implementation(libs.mlkit.text.recognition)

    // Gson - JSON serialization for SharedPreferences
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}