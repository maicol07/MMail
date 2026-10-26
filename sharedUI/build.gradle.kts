import io.github.kingsword09.symbolcraft.model.SymbolVariant
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.symbolCraft)
}

kotlin {
    android {
        namespace = providers.gradleProperty("appPackageName").get() + ".shared"
        compileSdk {
            version = release(providers.gradleProperty("androidCompileSdk").get().toInt()) {
                minorApiLevel = providers.gradleProperty("androidCompileSdkMinor").get().toInt()
            }
        }
        minSdk = providers.gradleProperty("androidMinSdk").get().toInt()
        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
        withHostTest {}
    }

    sourceSets {
        commonMain { kotlin.srcDir("build/generated/source/symbolcraft/commonMain/kotlin") }
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            implementation(libs.compose.animation)
            api(libs.compose.material3)
            implementation(libs.compose.resources)
            implementation(libs.materialKolor)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        androidMain.dependencies {
            implementation(libs.androidx.activityCompose)
            implementation(libs.msal)
            implementation(libs.okhttp)
            implementation(libs.jsoup)
            implementation(libs.compose.webview)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.json)
            implementation(libs.okhttp.mockwebserver)
        }
    }
}

tasks.matching { it.name.startsWith("compile") }.configureEach {
    dependsOn(tasks.named("generateSymbolCraftIcons"))
}

compose.resources { packageOfResClass = "it.maicol07.mmail.resources" }

symbolCraft {
    outputDirectory = "build/generated/source/symbolcraft/commonMain/kotlin"
    cacheEnabled = true
    generatePreview = false
    materialSymbols(
        "mail", "inbox", "star", "send", "archive", "delete", "edit", "search",
        "close", "arrow_back", "reply", "menu", "filter_list", "attach_file", "settings",
    ) { bothFills(500, SymbolVariant.ROUNDED) }
}
