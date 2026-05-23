package com.apptester.common

data class AppInfo(
    val name: String = "App Tester",
    val version: String = "1.0.0-alpha",
    val developer: String = "TODO",
    val email: String = "TODO",
    val packagePrefix: String = "com.apptester"
)

object AppConfig {
    const val PACKAGE_PREFIX = "com.apptester"
    const val APP_NAME = "App Tester"
    const val VERSION_NAME = "1.0.0-alpha"
    const val VERSION_CODE = 1
    const val MIN_SDK = 24
    const val TARGET_SDK = 34
    const val COMPILE_SDK = 34
}
