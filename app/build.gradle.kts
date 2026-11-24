import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// Firma de release: keystore.properties (gitignored) o variables VELOCIMETRO_*. Sin ninguna se
// firma con el debug.keystore, que vale para probar pero no para publicar.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

fun datoFirma(clave: String, entorno: String): String? =
    (keystoreProps[clave] as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(entorno)?.takeIf { it.isNotBlank() }

val storeFileRelease = datoFirma("storeFile", "VELOCIMETRO_STORE_FILE")

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // iosX64 para el simulador de los Mac con Intel.
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.activity.compose)
            implementation(libs.coroutines.android)
            implementation(project(":actualizador"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.resources {
    packageOfResClass = "com.marcmayol.velocimetro.res"
}

android {
    namespace = "com.marcmayol.velocimetro"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.marcmayol.velocimetro"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = providers.gradleProperty("appVersionCode").get().toInt()
        versionName = providers.gradleProperty("appVersionName").get()
    }

    signingConfigs {
        create("release") {
            if (storeFileRelease != null) {
                storeFile = file(storeFileRelease)
                storePassword = datoFirma("storePassword", "VELOCIMETRO_STORE_PASSWORD")
                keyAlias = datoFirma("keyAlias", "VELOCIMETRO_KEY_ALIAS")
                keyPassword = datoFirma("keyPassword", "VELOCIMETRO_KEY_PASSWORD")
            } else {
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}
