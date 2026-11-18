import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val servidor = providers.gradleProperty("recordatoriosUrl").orElse("http://10.0.2.2:8765/").get()
val uriServidor = URI(servidor)
require(uriServidor.scheme in setOf("http", "https") && uriServidor.host != null && uriServidor.userInfo == null &&
    uriServidor.query == null && uriServidor.fragment == null && servidor.endsWith("/")) { "Revisá recordatoriosUrl." }

android {
    namespace = "com.example.primeraclaseprogramacionmobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.primeraclaseprogramacionmobile"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "3.0"
        buildConfigField("String", "URL_SERVICIO", "\"$servidor\"")

        testInstrumentationRunner = "com.example.primeraclaseprogramacionmobile.RunnerMobile"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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

ksp { arg("room.schemaLocation", file("schemas").absolutePath) }

dependencies {
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.datastore)
    implementation(libs.retrofit)
    implementation(libs.converter.json)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation(libs.okhttp)
    androidTestImplementation(libs.mockwebserver)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
