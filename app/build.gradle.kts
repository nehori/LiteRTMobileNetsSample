plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.litertmobilenetssample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.litertmobilenetssample"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

    }
    // ↓↓↓ ここから追加 (もし buildFeatures の前や後など、androidブロック内であればOK) ↓↓↓
    aaptOptions {
        // noCompressプロパティに直接リストを代入するのではなく、
        // .addAll() メソッドを使って要素を追加する
        noCompress.addAll(listOf("tflite"))
    }
    // ↑↑↑ ここまで追加 ↑↑↑
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // ↓↓↓ ここから追加 ↓↓↓
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4") // 画像分類タスクライブラリ
    // ↑↑↑ ここまで追加 ↑↑↑

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}