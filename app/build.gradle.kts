import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.compose.compiler)
}

val properties = gradleLocalProperties(rootDir, providers)
val myProp = properties["propName"]

android {
    namespace = "com.organizen.app"
    compileSdk = 35

    defaultConfig {
        buildConfigField ("String", "default_account_iccid", properties.getProperty("default.account.iccid", ""))
        applicationId = "com.organizen.app"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }

    packaging {
        resources {
            excludes.add(
                "META-INF/INDEX.LIST"
            )
            excludes.add(
                "META-INF/io.netty.versions.properties"
            )
        }
    }
}


dependencies {
    // Android de bază
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.material)

    // Splash Screen API
    implementation(libs.androidx.core.splashscreen)

    // Firebase Auth (+ BOM)
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    implementation("com.google.firebase:firebase-auth")

    implementation(libs.koog.agents)
    implementation(libs.kotlinx.coroutines.play.services)
    // Add a dependency of Health Connect SDK
    implementation (libs.androidx.connect.client)

    // Glance for home-screen widgets
    implementation("androidx.glance:glance-appwidget:1.0.0")

    // Teste
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}