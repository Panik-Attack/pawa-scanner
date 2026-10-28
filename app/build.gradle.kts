import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pawa.scanner"
    compileSdk = 34

    // Leer propiedades desde local.properties (no se sube a git)
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(FileInputStream(localPropertiesFile))
    }

    defaultConfig {
        applicationId = "com.pawa.scanner"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PAWA_API_KEY", "\"${localProperties.getProperty("pawa.api.key", "TU_API_KEY_AQUI")}\"")
        buildConfigField("String", "PAWA_DEVICE_ID", "\"${localProperties.getProperty("pawa.device.id", "PDA-001")}\"")
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
    buildFeatures {
        buildConfig = true
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
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.24")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    constraints {
        implementation("androidx.lifecycle:lifecycle-common:2.8.3")
        implementation("androidx.lifecycle:lifecycle-livedata:2.8.3")
        implementation("androidx.lifecycle:lifecycle-livedata-core:2.8.3")
        implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.3")
        implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.3")
        implementation("androidx.lifecycle:lifecycle-process:2.8.3")
        implementation("androidx.lifecycle:lifecycle-runtime:2.8.3")
        implementation("androidx.lifecycle:lifecycle-common-java8:2.8.3")
        implementation("androidx.annotation:annotation-jvm:1.8.0")
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        implementation("androidx.activity:activity:1.9.0")
        implementation("androidx.fragment:fragment:1.8.1")
        implementation("androidx.loader:loader:1.0.0")
        implementation("androidx.savedstate:savedstate:1.2.1")
        implementation("androidx.emoji2:emoji2:1.3.0")
    }
}
