plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("org.jetbrains.compose")
}

kotlin {
    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.7.2")
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
                implementation("androidx.core:core-ktx:1.10.1")
            }
        }
    }
}

android {
    namespace = "com.antoniofuture.testerapp.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.8"
    }
}
