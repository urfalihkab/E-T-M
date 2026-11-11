plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val catalogUrl: String = (project.findProperty("catalogUrl") as String?) ?: ""
val versionCodeProp: Int = ((project.findProperty("versionCode") as String?) ?: "1").toInt()

android {
    namespace = "com.seblaziz.egitim"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.seblaziz.egitim"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeProp
        versionName = "1.0.$versionCodeProp"
        buildConfigField("String", "CATALOG_URL", "\"$catalogUrl\"")
    }

    val ks = System.getenv("KEYSTORE_FILE")
    val hasReleaseKey = ks != null && file(ks).exists()
    signingConfigs {
        if (hasReleaseKey) create("release") {
            storeFile = file(ks!!)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Kalıcı anahtar tanımlı değilse Android'in hata ayıklama imzası kullanılır (yine de kurulabilir).
            signingConfig = signingConfigs.getByName(if (hasReleaseKey) "release" else "debug")
        }
    }

    // Kök klasördeki catalog/ uygulamanın içine yedek olarak gömülür (internetsiz açılış için).
    sourceSets["main"].assets.srcDir(rootProject.file("catalog"))

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
}
