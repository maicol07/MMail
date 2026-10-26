import java.util.Properties

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

val localConfiguration = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val outlookClientId = localConfiguration.getProperty("outlook.clientId", "")
val outlookSignatureHash = localConfiguration.getProperty("outlook.signatureHash", "")

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
        buildConfigField("String", "OUTLOOK_CLIENT_ID", "\"${outlookClientId.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "OUTLOOK_SIGNATURE_HASH", "\"${outlookSignatureHash.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        manifestPlaceholders["outlookSignatureHash"] = outlookSignatureHash.ifBlank { "unconfigured" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    implementation(project(":sharedUI"))
    implementation(libs.androidx.activityCompose)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
