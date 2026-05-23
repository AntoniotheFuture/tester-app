package com.apptester.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.webkit.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.apptester.common.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewTestScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    var currentUrl by remember { mutableStateOf("https://www.example.com") }
    var urlInput by remember { mutableStateOf(currentUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var logs by remember { mutableStateOf(listOf<LogEntry>()) }
    var jsLogs by remember { mutableStateOf(listOf<LogEntry>()) }
    var activityLogs by remember { mutableStateOf(listOf<LogEntry>()) }
    var showLogDrawer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showInjection by remember { mutableStateOf(false) }
    var selectedLogTab by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var recentUrls by remember { mutableStateOf(listOf(currentUrl)) }
    var showUrlDropdown by remember { mutableStateOf(false) }

    var settings by remember {
        mutableStateOf(
            SettingsData(
                initialUrl = "https://www.example.com",
                downloadPath = "/storage/emulated/0/Download",
                userAgent = "",
                enableJavaScript = true,
                enableCookies = true,
                enableCache = true
            )
        )
    }

    var injectionScripts by remember {
        mutableStateOf(listOf(InjectionScript("默认脚本", "console.log('App Tester: 脚本已注入');", true)))
    }

    fun addLog(level: LogLevel, message: String, source: LogSource) {
        val entry = createLogEntry(level, message, source)
        logs = logs + entry
        when (source) {
            LogSource.JAVASCRIPT -> jsLogs = jsLogs + entry
            LogSource.WEBVIEW_ACTIVITY -> activityLogs = activityLogs + entry
            else -> {}
        }
    }

    fun navigateTo(url: String) {
        val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        currentUrl = finalUrl
        urlInput = finalUrl
        recentUrls = (listOf(finalUrl) + recentUrls.filter { it != finalUrl }).take(5)
        webViewRef?.loadUrl(finalUrl)
        addLog(LogLevel.INFO, "导航到: $finalUrl", LogSource.WEBVIEW_ACTIVITY)
    }

    fun executeInjectionScripts(webView: WebView) {
        injectionScripts.filter { it.enabled }.forEach { script ->
            webView.evaluateJavascript(script.code) { result ->
                addLog(LogLevel.INFO, "注入脚本 [${script.name}]: $result", LogSource.JAVASCRIPT)
            }
        }
    }

    val drawerState = rememberBottomDrawerState(initialValue = BottomDrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BottomDrawer(
        drawerState = drawerState,
        drawerContent = {
            LogDrawerContent(
                logs = logs,
                jsLogs = jsLogs,
                activityLogs = activityLogs,
                selectedTab = selectedLogTab,
                onTabSelected = { selectedLogTab = it },
                onClearLogs = {
                    logs = emptyList()
                    jsLogs = emptyList()
                    activityLogs = emptyList()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("WebView 测试", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    actions = {
                        IconButton(onClick = {
                            showLogDrawer = true
                            kotlinx.coroutines.MainScope().launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(Icons.Default.List, contentDescription = "日志", tint = Color.White)
                        }
                        IconButton(onClick = { showInjection = true }) {
                            Icon(Icons.Default.Code, contentDescription = "注入", tint = Color.White)
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "设置", tint = Color.White)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 地址栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("地址") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showUrlDropdown = !showUrlDropdown }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "历史")
                                }
                                DropdownMenu(
                                    expanded = showUrlDropdown,
                                    onDismissRequest = { showUrlDropdown = false }
                                ) {
                                    recentUrls.forEach { url ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    url,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            onClick = {
                                                urlInput = url
                                                navigateTo(url)
                                                showUrlDropdown = false
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.History, contentDescription = null)
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                    IconButton(onClick = { navigateTo(urlInput) }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "前往")
                    }
                }

                // 导航按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = {
                            webViewRef?.goBack()
                            addLog(LogLevel.INFO, "后退", LogSource.WEBVIEW_ACTIVITY)
                        },
                        enabled = canGoBack
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "后退")
                    }
                    IconButton(
                        onClick = {
                            webViewRef?.goForward()
                            addLog(LogLevel.INFO, "前进", LogSource.WEBVIEW_ACTIVITY)
                        },
                        enabled = canGoForward
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "前进")
                    }
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = { webViewRef?.stopLoading() }) {
                        Icon(Icons.Default.Stop, contentDescription = "停止")
                    }
                }

                // 进度条
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // WebView
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = settings.enableJavaScript
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.allowFileAccess = true
                                settings.setSupportZoom(true)
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                settings.cookieAcceptPolicy = if (settings.enableCookies) {
                                    CookieManager.getInstance().setAcceptCookie(true)
                                    WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                } else {
                                    CookieManager.getInstance().setAcceptCookie(false)
                                    WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                }

                                if (settings.userAgent.isNotEmpty()) {
                                    settings.userAgentString = settings.userAgent
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    settings.safeBrowsingEnabled = true
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        isLoading = true
                                        loadingProgress = 0
                                        url?.let {
                                            currentUrl = it
                                            urlInput = it
                                            if (it !in recentUrls) {
                                                recentUrls = (listOf(it) + recentUrls).take(5)
                                            }
                                        }
                                        addLog(LogLevel.INFO, "页面开始加载: $url", LogSource.WEBVIEW_ACTIVITY)
                                        executeInjectionScripts(view!!)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        loadingProgress = 100
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                        addLog(LogLevel.INFO, "页面加载完成: $url", LogSource.WEBVIEW_ACTIVITY)
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            errorMessage = error?.description?.toString() ?: "未知错误"
                                            addLog(LogLevel.ERROR, "错误: $errorMessage", LogSource.WEBVIEW_ACTIVITY)
                                        }
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        loadingProgress = newProgress
                                    }

                                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                        val level = when (consoleMessage?.messageLevel()) {
                                            ConsoleMessage.MessageLevel.ERROR -> LogLevel.ERROR
                                            ConsoleMessage.MessageLevel.WARNING -> LogLevel.WARNING
                                            else -> LogLevel.INFO
                                        }
                                        addLog(level, "[JS] ${consoleMessage?.message()}", LogSource.JAVASCRIPT)
                                        return super.onConsoleMessage(consoleMessage)
                                    }
                                }

                                downloadListener = object : DownloadListener {
                                    override fun onDownloadStart(
                                        url: String?,
                                        userAgent: String?,
                                        contentDisposition: String?,
                                        mimetype: String?,
                                        contentLength: Long
                                    ) {
                                        addLog(LogLevel.INFO, "下载开始: $url", LogSource.WEBVIEW_ACTIVITY)
                                        url?.let {
                                            try {
                                                val request = android.webkit.DownloadManager.Request(Uri.parse(it))
                                                request.setNotificationVisibility(
                                                    android.webkit.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                                )
                                                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.webkit.DownloadManager
                                                downloadManager.enqueue(request)
                                            } catch (e: Exception) {
                                                addLog(LogLevel.ERROR, "下载失败: ${e.message}", LogSource.WEBVIEW_ACTIVITY)
                                            }
                                        }
                                    }
                                }

                                if (settings.enableCache) {
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                                } else {
                                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                }

                                loadUrl(settings.initialUrl)
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 错误显示
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("加载错误", color = MaterialTheme.colorScheme.error)
                                Text(error)
                                TextButton(onClick = { errorMessage = null }) {
                                    Text("关闭")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 注入脚本对话框
    if (showInjection) {
        InjectionDialog(
            scripts = injectionScripts,
            onDismiss = { showInjection = false },
            onScriptChange = { injectionScripts = it }
        )
    }

    // 设置对话框
    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onDismiss = { showSettings = false },
            onSettingsChange = { newSettings ->
                settings = newSettings
                webViewRef?.let { webView ->
                    webView.settings.javaScriptEnabled = newSettings.enableJavaScript
                    if (newSettings.userAgent.isNotEmpty()) {
                        webView.settings.userAgentString = newSettings.userAgent
                    }
                    webView.settings.cacheMode = if (newSettings.enableCache) {
                        WebSettings.LOAD_DEFAULT
                    } else {
                        WebSettings.LOAD_NO_CACHE
                    }
                }
            }
        )
    }
}

@Composable
fun LogDrawerContent(
    logs: List<LogEntry>,
    jsLogs: List<LogEntry>,
    activityLogs: List<LogEntry>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "日志面板",
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = onClearLogs) {
                Text("清除")
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                text = { Text("全部日志") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                text = { Text("JS 日志") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                text = { Text("Activity") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) },
                text = { Text("程序信息") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val displayLogs = when (selectedTab) {
            0 -> logs
            1 -> jsLogs
            2 -> activityLogs
            else -> emptyList()
        }

        if (selectedTab == 3) {
            ProgramInfoContent()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                items(displayLogs.takeLast(100)) { log ->
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val time = timeFormat.format(Date(log.timestamp))
                    val color = when (log.level) {
                        LogLevel.ERROR -> Color.Red
                        LogLevel.WARNING -> Color.Yellow
                        else -> Color.Unspecified
                    }
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Row {
                            Text("[$time] ", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("[${log.source.name}] ", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text(
                            log.message,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = color
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun ProgramInfoContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("WebView 版本信息", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        InfoRow("Android 版本", Build.VERSION.RELEASE)
        InfoRow("SDK 版本", Build.VERSION.SDK_INT.toString())
        InfoRow("WebView 版本", Build.VERSION.SDK_INT.toString())
        InfoRow("设备型号", Build.MODEL)
        InfoRow("厂商", Build.MANUFACTURER)

        Spacer(modifier = Modifier.height(16.dp))
        Text("支持的功能", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        FeatureItem("JavaScript", true)
        FeatureItem("DOM Storage", true)
        FeatureItem("File Access", true)
        FeatureItem("Wide ViewPort", true)
        FeatureItem("Zoom Controls", true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            FeatureItem("Safe Browsing", true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FeatureItem("Force Dark", true)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun FeatureItem(name: String, supported: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (supported) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (supported) Color.Green else Color.Red,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InjectionDialog(
    scripts: List<InjectionScript>,
    onDismiss: () -> Unit,
    onScriptChange: (List<InjectionScript>) -> Unit
) {
    var editingScript by remember { mutableStateOf<InjectionScript?>(null) }
    var newScriptName by remember { mutableStateOf("") }
    var newScriptCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("注入脚本配置") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("已配置的脚本:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                scripts.forEachIndexed { index, script ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = script.enabled,
                                onCheckedChange = { enabled ->
                                    val newList = scripts.toMutableList()
                                    newList[index] = script.copy(enabled = enabled)
                                    onScriptChange(newList)
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(script.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    script.code.take(30) + "...",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = {
                                val newList = scripts.toMutableList()
                                newList.removeAt(index)
                                onScriptChange(newList)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("添加新脚本:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newScriptName,
                    onValueChange = { newScriptName = it },
                    label = { Text("脚本名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newScriptCode,
                    onValueChange = { newScriptCode = it },
                    label = { Text("JavaScript 代码") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (newScriptName.isNotEmpty() && newScriptCode.isNotEmpty()) {
                            val newList = scripts + InjectionScript(newScriptName, newScriptCode, true)
                            onScriptChange(newList)
                            newScriptName = ""
                            newScriptCode = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("添加脚本")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: SettingsData,
    onDismiss: () -> Unit,
    onSettingsChange: (SettingsData) -> Unit
) {
    var localSettings by remember { mutableStateOf(settings) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = localSettings.initialUrl,
                    onValueChange = { localSettings = localSettings.copy(initialUrl = it) },
                    label = { Text("初始页面地址") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = localSettings.downloadPath,
                    onValueChange = { localSettings = localSettings.copy(downloadPath = it) },
                    label = { Text("下载文件保存位置") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = localSettings.userAgent,
                    onValueChange = { localSettings = localSettings.copy(userAgent = it) },
                    label = { Text("User-Agent (留空使用默认)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用 JavaScript")
                    Switch(
                        checked = localSettings.enableJavaScript,
                        onCheckedChange = { localSettings = localSettings.copy(enableJavaScript = it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用 Cookie")
                    Switch(
                        checked = localSettings.enableCookies,
                        onCheckedChange = { localSettings = localSettings.copy(enableCookies = it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用缓存")
                    Switch(
                        checked = localSettings.enableCache,
                        onCheckedChange = { localSettings = localSettings.copy(enableCache = it) }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSettingsChange(localSettings)
                onDismiss()
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

data class InjectionScript(
    val name: String,
    val code: String,
    val enabled: Boolean = true
)

data class SettingsData(
    val initialUrl: String = "https://www.example.com",
    val downloadPath: String = "/storage/emulated/0/Download",
    val userAgent: String = "",
    val enableJavaScript: Boolean = true,
    val enableCookies: Boolean = true,
    val enableCache: Boolean = true
)
