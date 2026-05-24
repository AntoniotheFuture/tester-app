pluginManagement {
    val useAliyun = System.getenv("CI") != "true"
    repositories {
        if (useAliyun) {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/central") }
        }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0" apply false
}

dependencyResolutionManagement {
    val useAliyun = System.getenv("CI") != "true"
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        if (useAliyun) {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/central") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "tester-app"
include(":shared")
include(":androidApp")
