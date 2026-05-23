package com.apptester.common

import kotlinx.serialization.Serializable

@Serializable
data class WebViewState(
    val currentUrl: String = "",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val loadingProgress: Int = 0,
    val errorMessage: String? = null
)

@Serializable
data class JsInjectionConfig(
    val script: String = "",
    val injectBeforeLoad: Boolean = false
)

@Serializable
data class WebViewInfo(
    val webViewVersion: String = "",
    val safeBrowsingVersion: String = "",
    val supportsFeature: List<String> = emptyList()
)

enum class FileOperation {
    DOWNLOAD,
    SELECT_FILE
}

@Serializable
data class FileEvent(
    val type: FileOperation,
    val fileName: String,
    val mimeType: String,
    val timestamp: Long
)
