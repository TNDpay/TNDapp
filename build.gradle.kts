buildscript {
    val hilt_version = "2.43.2"
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hilt_version")
    }
}
plugins {
    id("com.google.dagger.hilt.android") version "2.43.2" apply false
    id("com.android.application") version "8.3.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.0" apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
}
