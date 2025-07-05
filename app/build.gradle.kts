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
        applicationId = "de.saschahlusiak.frupic"

        minSdk = 23
        targetSdk = 36

        versionCode = 40
        versionName = "1.5.0"
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
        viewBinding = true
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
    implementation("androidx.appcompat:appcompat:1.7.1")
    // https://mvnrepository.com/artifact/androidx.swiperefreshlayout/swiperefreshlayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // https://mvnrepository.com/artifact/androidx.viewpager/viewpager
    implementation("androidx.viewpager:viewpager:1.1.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.android.material:material:1.12.0")
    // https://mvnrepository.com/artifact/androidx.annotation/annotation
    implementation("androidx.annotation:annotation:1.9.1")
    // https://mvnrepository.com/artifact/androidx.recyclerview/recyclerview
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // https://firebase.google.com/support/release-notes/android
    implementation("com.google.firebase:firebase-analytics:22.5.0")
    implementation("com.google.firebase:firebase-crashlytics:19.4.4")

    // Fragment
    implementation("androidx.fragment:fragment-ktx:1.8.8")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-process:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation:1.8.3")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")

    // Dagger/Hilt
    ksp("com.google.dagger:hilt-compiler:2.56.2")
    implementation("com.google.dagger:hilt-android:2.56.2")

    // for launcher badges
    implementation("me.leolin:ShortcutBadger:1.1.22@aar")

    // Picasso for image downloading
    implementation("com.squareup.picasso:picasso:2.71828")
}
