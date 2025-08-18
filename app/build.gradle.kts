plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
//    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.organizen.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.organizen.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
//    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}

//dependencies {
//
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.appcompat)
////    implementation(libs.material)
//
//    // Compose
//    implementation(platform(libs.androidx.compose.bom))
//    implementation (libs.google.firebase.auth.ktx)
////    implementation(libs.androidx.activity)
//    implementation(libs.androidx.activity.compose)
//    implementation(libs.androidx.navigation.compose)
//    implementation(libs.androidx.material3)
////    implementation(libs.androidx.constraintlayout) //xml
//    implementation(libs.androidx.core.splashscreen)
//    implementation(libs.kotlinx.coroutines.play.services)
//
//
//    implementation(platform(libs.firebase.bom))
//    implementation(libs.firebaseAuthKtx)
//
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//}

//dependencies {
//    // Android de bază
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.appcompat) // poți renunța dacă nu ai niciun ecran pe Views
//
//    // Compose
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.activity.compose)
//    implementation(libs.androidx.navigation.compose)
//    implementation(libs.androidx.material3)
//
//    // Splash screen API
//    implementation(libs.androidx.core.splashscreen)
//
//    // Firebase Auth + coroutines-play-services (pentru await())
//    implementation(platform(libs.firebase.bom))
//    implementation(libs.firebase.auth.ktx) // <- vezi să folosești numele exact din TOML: libs.firebase-auth-ktx
//    implementation(libs.kotlinx.coroutines.play.services)
//
//    // Teste
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//}

dependencies {
    // Android de bază
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)                 // poți elimina dacă e 100% Compose

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.material)
    implementation("androidx.health.connect:connect-client:1.1.0-alpha08")

    // Splash Screen API
    implementation(libs.androidx.core.splashscreen)

    // Firebase Auth (+ BOM)
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    implementation("com.google.firebase:firebase-auth")
//    implementation(platform(libs.firebase.bom))
//    implementation(libs.firebase.auth.ktx)
//    implementation(platform("com.google.firebase:firebase-bom:32.8.0")) // Or the latest BoM version
//    implementation("com.google.firebase:firebase-auth-ktx")

    // await() pentru Task (Firebase)
    implementation(libs.kotlinx.coroutines.play.services)
//    implementation(libs.google.firebase.auth.ktx)
//    implementation(libs.google.firebase.auth.ktx)

//    // Teste
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}