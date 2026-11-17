plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.dcam.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dcam.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 4
        versionName = "2.1.0"
    }

    // A fixed key is committed on purpose so every build is signed the same way
    // and new APKs install as updates over old ones. Fine for a personal app;
    // replace with a private key before publishing to the Play Store.
    signingConfigs {
        create("dcam") {
            storeFile = file("dcam-release.jks")
            storePassword = "dcam1234"
            keyAlias = "dcam"
            keyPassword = "dcam1234"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("dcam")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("dcam")
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
        compose = true
    }
}

dependencies {
    val cameraX = "1.5.1"

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")
}
