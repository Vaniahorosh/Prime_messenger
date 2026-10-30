plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.messenger.prime"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.messenger.prime"
        minSdk = 26
        targetSdk = 37
        versionCode = 64
        versionName = "0.64"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.slidableactivity)
    implementation(libs.glide)
    implementation(libs.glide.transformations)
    implementation(libs.jsoup)

    // High-Performance Kotlin Coroutines & Lifecycle Architecture
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // High-Speed Networking & Image Loading
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.coil)
    implementation(libs.coil.gif)

    // Startup & Compilation Optimization
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.serialization.json)

    // Compose & BlurView
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.viewbinding)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.blurview)
    implementation(libs.androidx.graphics.shapes)

    // Media3 & Playback
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource)
    implementation(libs.media3.extractor)
    implementation(libs.androidx.media)
    implementation(libs.androidx.viewpager2)

    // Window Manager
    implementation(libs.androidx.window)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
