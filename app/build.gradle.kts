plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.grabpro"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.a2.ayb"; minSdk = 29; targetSdk = 34; versionCode = 3; versionName = "3.0"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { jniLibs { useLegacyPackaging = true } }
}
dependencies {
    // yt-dlp + ffmpeg running on-device. If the build says the version doesn't exist, bump it to the latest on Maven Central.
    implementation("io.github.junkfood02.youtubedl-android:library:0.17.2")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.17.2")
    implementation("androidx.webkit:webkit:1.12.1") // private tabs (separate cookie profile)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.mlkit:text-recognition:16.0.1") // on-device OCR (Latin script)
}
