plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

android {
    namespace = providers.gradleProperty("appPackageName").get()
    compileSdk {
        version = release(providers.gradleProperty("androidCompileSdk").get().toInt()) {
            minorApiLevel = providers.gradleProperty("androidCompileSdkMinor").get().toInt()
        }
    }
    defaultConfig {
        applicationId = providers.gradleProperty("appPackageName").get()
        minSdk = providers.gradleProperty("androidMinSdk").get().toInt()
        targetSdk = providers.gradleProperty("androidTargetSdk").get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":sharedUI"))
    implementation(libs.androidx.activityCompose)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
