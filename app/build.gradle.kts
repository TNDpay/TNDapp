import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
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

    packagingOptions {
        resources {
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/DEPENDENCIES")
            pickFirsts.add("META-INF/*")
        }
    }
    defaultConfig {
        applicationId = "com.tnd.tnd"
        minSdk = 28
        targetSdk = 34
        versionCode = 3
        versionName = "1.3"
        resConfigs("en")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "HELIUS_API_KEY", "\"${localProperties.getProperty("HELIUS_API_KEY", "")}\"")

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

    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")
    implementation("com.google.firebase:firebase-firestore-ktx:25.0.0")
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
    implementation("com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.3")
    implementation("com.portto.solana:web3:0.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.bitcoinj:bitcoinj-core:0.16.2")

    //Sol SDK
    implementation("com.github.metaplex-foundation:SolanaKT:2.1.1")
    //UI
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("com.github.cachapa:ExpandableLayout:2.9.2")
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

    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    //implementation("com.google.firebase:firebase-analytics")


}