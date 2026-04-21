plugins {
    id("kotlin-android")

    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.material)

    // https://firebase.google.com/support/release-notes/android
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Lifecycle
    implementation(libs.androidx.lifecycle.process)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    compileOnly(libs.androidx.compose.ui.tooling)
    compileOnly(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended.android)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.zoomable.image.coil)

    // Room for data persisting
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Dagger/Hilt
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.android)

    // for launcher badges
    implementation(libs.shortcutbadger)
}
