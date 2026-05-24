plugins {
    kotlin("multiplatform") version "1.8.22" apply false
    kotlin("android") version "1.8.22" apply false
    kotlin("plugin.serialization") version "1.8.22" apply false
    id("com.android.application") version "8.1.4" apply false
    id("com.android.library") version "8.1.4" apply false
    id("org.jetbrains.compose") version "1.4.3" apply false
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
