plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

repositories {
    mavenCentral()
    google()
}

android {
    compileSdk = 36

    defaultConfig {
        applicationId = "de.saschahlusiak.frupic2"

        minSdk = 31
        targetSdk = 36

        versionCode = 41
        versionName = "2.0.1"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
    namespace = "de.saschahlusiak.frupic"
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.android.material:material:1.13.0")

    // https://firebase.google.com/support/release-notes/android
    implementation("com.google.firebase:firebase-analytics:23.0.0")
    implementation("com.google.firebase:firebase-crashlytics:20.0.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-process:2.9.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    compileOnly("androidx.compose.ui:ui-tooling:1.9.0")
    compileOnly("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("io.coil-kt:coil:2.7.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("me.saket.telephoto:zoomable-image-coil:0.16.0")

    // Room for data persisting
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    // Dagger/Hilt
    ksp("com.google.dagger:hilt-compiler:2.57.1")
    implementation("com.google.dagger:hilt-android:2.57.1")

    // for launcher badges
    implementation("me.leolin:ShortcutBadger:1.1.22@aar")
}
