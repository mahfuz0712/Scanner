plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Bump ONLY this when you release (tag the GitHub release with the same number, e.g. v1.0.1)
val appVersionName = "1.0.0"

android {
    namespace = "com.mux.android.scanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mux.android.scanner"
        minSdk = 29          // Android 10
        targetSdk = 34
        versionName = appVersionName
        versionCode = appVersionName.split(".").let { it[0].toInt() * 10000 + it[1].toInt() * 100 + it[2].toInt() }
    }

    // Same debug key on every CI build, so a new APK installs over the old one as an update.
    // app/debug.keystore (same one as Dialer) is used when present, so updates install over old builds.
    val ks = file("debug.keystore")
    if (ks.exists()) {
        signingConfigs {
            getByName("debug") {
                storeFile = ks
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.material3:material3")

    // Camera
    val camerax = "1.3.4"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")

    // QR / barcode (model is bundled in the app, works offline)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Document scanner (edge detection, crop, filters, PDF) - runs through Google Play services
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
}
