import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.tnd"
    compileSdk = 34
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.tnd.tnd"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        resConfigs("en")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val ironForgeApiKey = localProperties["IronForge_API_KEY"] as? String
        if (ironForgeApiKey != null) {
            buildConfigField("String", "IRONFORGE_API_KEY", "\"$ironForgeApiKey\"")
        } else {
            throw GradleException("IronForge API Key not found in local.properties")
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    val hilt_version = "2.43.2"
    //implementation("io.metamask.androidsdk:metamask-android-sdk:0.1.2")

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // dagger-hilt
    implementation("com.google.dagger:hilt-android:$hilt_version")
    kapt("com.google.dagger:hilt-compiler:$hilt_version")

    // viewmodel-related
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.activity:activity-ktx:1.3.1")

    //MWA
    implementation("com.solanamobile:mobile-wallet-adapter-clientlib-ktx:1.1.0")
    implementation("com.portto.solana:web3:0.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0") // Use the latest version
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.bitcoinj:bitcoinj-core:0.16.2")

    //Sol SDK
    implementation("com.github.metaplex-foundation:SolanaKT:2.1.1")
    //UI
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    //HTTP REQUEST
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")

    //browser
    implementation("androidx.browser:browser:1.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.google.code.gson:gson:2.8.6")

    //MAP
    //implementation("com.google.android.gms:play-services-location:21.2.0")
    //implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("org.osmdroid:osmdroid-android:6.1.14")
    implementation("org.osmdroid:osmdroid-wms:6.1.14")
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.14")
    implementation("org.osmdroid:osmdroid-geopackage:6.1.14")

    //EVM (Metamask)
    implementation("io.metamask.androidsdk:metamask-android-sdk:0.2.1")
    implementation("org.web3j:core:4.8.7") {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    }
    implementation("org.web3j:crypto:4.8.7"){
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    }

}