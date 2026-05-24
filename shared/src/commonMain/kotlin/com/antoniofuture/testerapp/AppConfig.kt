package com.antoniofuture.testerapp.common

data class AppInfo(
    val name: String = "Tester App",
    val version: String = "1.0.0-alpha",
    val developer: String = "Antonio Future",
    val email: String = "antonioliang@Foxmail.com",
    val packagePrefix: String = "com.antoniofuture.testerapp",
    val githubRepo: String = "https://github.com/AntoniotheFuture/tester-app"
)

object AppConfig {
    const val PACKAGE_PREFIX = "com.antoniofuture.testerapp"
    const val APP_NAME = "Tester App"
    const val VERSION_NAME = "1.0.0-alpha"
    const val VERSION_CODE = 1
    const val MIN_SDK = 24
    const val TARGET_SDK = 34
    const val COMPILE_SDK = 34

    const val DEVELOPER = "Antonio Future"
    const val DEVELOPER_EMAIL = "antonioliang@Foxmail.com"
    const val GITHUB_REPO = "https://github.com/AntoniotheFuture/tester-app"
}
