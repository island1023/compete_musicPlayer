plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android) // 必须保留，否则后续 kotlin 代码会报错
}

android {
    namespace = "com.example.musicplayer"

    // 按照你需要的配置，改为 34
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.musicplayer"
        // 按照你需要的配置修改
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // 建议保留 ViewBinding
    buildFeatures {
        viewBinding = true
    }

    // 强烈建议保持 Java 11，改为 Java 8 极易在现代 AS 中引发编译错误
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // 🌟 必须显式添加这一行，让系统支持 Android 14 的广播安全标志底层修复
    implementation("androidx.core:core:1.13.1")
    // === 基础 UI ===
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // === 网络请求 ===
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:3.14.9") // 你额外需要的 okhttp

    // === 图片加载 ===
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // === 音乐播放 (旧版 ExoPlayer) ===
    // 按你需要的配置写入了 ExoPlayer 2.19.1
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")

    // === 生命周期与协程 (建议保留，避免旧代码报错) ===
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
}