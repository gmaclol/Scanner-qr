import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Cerca il file keystore.properties nella root del progetto o nella cartella app
var keystorePropertiesFile = rootProject.file("keystore.properties")
if (!keystorePropertiesFile.exists()) {
    keystorePropertiesFile = project.file("keystore.properties")
}

val keystoreProperties = Properties()
val hasKeystore = keystorePropertiesFile.exists()
if (hasKeystore) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.apple.quickscan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.apple.quickscan"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            if (hasKeystore) {
                val alias = keystoreProperties["keyAlias"] as? String
                val keyPass = keystoreProperties["keyPassword"] as? String
                val storePass = keystoreProperties["storePassword"] as? String
                val storeFileStr = keystoreProperties["storeFile"] as? String

                if (alias != null && keyPass != null && storePass != null && storeFileStr != null) {
                    keyAlias = alias
                    keyPassword = keyPass
                    storePassword = storePass
                    
                    val jksFile = keystorePropertiesFile.parentFile.resolve(storeFileStr)
                    if (jksFile.exists()) {
                        storeFile = jksFile
                    }
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                signingConfig = releaseSigning
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit Barcode Scanning
    implementation(libs.google.mlkit.barcode)

    // ZXing Core
    implementation(libs.zxing.core)

    // Gson & OkHttp (per UpdateManager)
    implementation(libs.gson)
    implementation(libs.okhttp)
}

