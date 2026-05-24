package com.antoniofuture.testerapp.common

import kotlinx.serialization.Serializable

enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}

@Serializable
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val message: String,
    val source: LogSource
)

enum class LogSource {
    JAVASCRIPT,
    WEBVIEW_ACTIVITY,
    SYSTEM
}

@Serializable
data class LogState(
    val logs: List<LogEntry> = emptyList(),
    val filterLevel: LogLevel? = null,
    val filterSource: LogSource? = null
)

fun createLogEntry(
    level: LogLevel,
    message: String,
    source: LogSource
): LogEntry = LogEntry(
    timestamp = System.currentTimeMillis(),
    level = level,
    message = message,
    source = source
)
