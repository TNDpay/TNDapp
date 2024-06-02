buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        val hiltVersion = "2.43.2"
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hiltVersion")
    }
}

plugins {
    id("com.google.dagger.hilt.android") version "2.43.2" apply false
    id("com.android.application") version "8.3.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.0" apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
