plugins {
    id("com.google.gms.google-services") version "4.4.4"
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val signingStore = providers.environmentVariable("TECLAB_STORE_FILE")
val signingPassword = providers.environmentVariable("TECLAB_STORE_PASSWORD")

android {
    namespace = "com.example.primeraclaseprogramacionmobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.primeraclaseprogramacionmobile"
        minSdk = 24
        targetSdk = 37
        versionCode = 4
        versionName = "4.0-rc1"

        testInstrumentationRunner = "com.example.primeraclaseprogramacionmobile.RunnerMobile"
    }

    signingConfigs {
        if (signingStore.isPresent && signingPassword.isPresent) {
            create("teclab") {
                storeFile = file(signingStore.get())
                storePassword = signingPassword.get()
                keyAlias = "teclab-mobile"
                keyPassword = signingPassword.get()
            }
        }
    }

    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            signingConfig = signingConfigs.findByName("teclab")
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
    constraints {
        implementation("androidx.fragment:fragment:1.8.9") { because("Activity Result necesita una versión compatible del fragmento usado por Firebase Auth") }
    }
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.datastore)
    androidTestImplementation(libs.retrofit)
    androidTestImplementation(libs.converter.json)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    androidTestImplementation(libs.okhttp)
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

val firmaConfigurada = signingStore.isPresent && signingPassword.isPresent
val validarFirmaTeclab = tasks.register("validarFirmaTeclab") {
    inputs.property("configurada", firmaConfigurada)
    doLast {
        check(inputs.properties["configurada"] == true) {
            "Para empaquetar release, configurá TECLAB_STORE_FILE y TECLAB_STORE_PASSWORD."
        }
    }
}
tasks.matching { it.name in setOf("packageRelease", "packageReleaseBundle", "signReleaseBundle") }.configureEach {
    dependsOn(validarFirmaTeclab)
}
