plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.georacing.georacing"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.georacing.georacing"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Soporte para dispositivos con páginas de 16 KB (requerido desde Nov 2025)
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}





kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

// Usar un build dir separado para evitar bloqueos de Windows en build/outputs/apk/debug
layout.buildDirectory.set(file("build_unlocked"))

// Copiar los APK generados al path solicitado
val copyDebugApk by tasks.registering {
    dependsOn("assembleDebug")
    doLast {
        val srcDir = layout.buildDirectory.dir("outputs/apk/debug").get().asFile
        val targetDir = layout.projectDirectory.dir("build/outputs/apk/debug").asFile
        targetDir.mkdirs()
        copy {
            from(srcDir) {
                include("*.apk")
                include("output-metadata.json")
            }
            into(targetDir)
        }
    }
}

afterEvaluate {
    tasks.named("assembleDebug").configure {
        finalizedBy(copyDebugApk)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Retrofit & Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)

    // Room Database (Offline-First with Coroutines & Flow support)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    
    // Firebase products (using BoM, no need to specify versions)
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    
    // Google Sign-In with Credential Manager (nuevo método recomendado)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    
    // Google Sign-In legacy (fallback para compatibilidad con emuladores)
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    
    // Google Play Services Location (para ubicación en tiempo real)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    
    // Nearby Connections API (para P2P entre dispositivos cercanos)
    implementation("com.google.android.gms:play-services-nearby:19.3.0")
    
    // Google Pay
    implementation("com.google.android.gms:play-services-wallet:19.4.0")
    
    // Google Play Billing
    implementation("com.android.billingclient:billing:7.0.0")

    // Health Connect
    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")
    
    // Kyant Backdrop (Removed)
    // implementation("io.github.kyant0:backdrop-android:2.0.0-alpha01")
    
    // Haze (Used for existing GlassSurface components, alongside Backdrop)
    implementation("dev.chrisbanes.haze:haze:1.1.0")
    
    // Kyant Backdrop (Liquid Glass Effects)
    implementation("io.github.kyant0:backdrop:1.0.5")
    implementation("io.github.kyant0:shapes:0.3.1")
    androidTestImplementation("io.github.kyant0:shapes:0.3.1")
    
    // MLKit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    
    // Jetpack Glance (For Live Activity UI)
    implementation("androidx.glance:glance:1.1.0")
    implementation("androidx.glance:glance-appwidget:1.1.0")

    // MapLibre Native Android SDK (sin Google Maps, sin API keys)
    implementation("org.maplibre.gl:android-sdk:11.5.0")
    
    // ZXing para generar y escanear códigos QR
    implementation("com.google.zxing:core:3.5.3")
    
    // CameraX para escanear QR (compatible con 16 KB page size)
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    
    // Accompanist para permisos
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // WorkManager Kotlin + Coroutines
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4") // Required for LifecycleService
    
    // Coroutines support for Firebase
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Android Auto Car App Library
    implementation("androidx.car.app:app:1.4.0")
    implementation("androidx.car.app:app-projected:1.4.0")
}
