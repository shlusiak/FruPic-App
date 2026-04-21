buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.1.1")

        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.2")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.10")

        // https://developers.google.com/android/guides/releases
        classpath("com.google.gms:google-services:4.4.4")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.57.2")

        // https://firebase.google.com/support/release-notes/android
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.6")
    }
}
