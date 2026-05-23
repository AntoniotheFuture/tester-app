package com.apptester.app.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.apptester.common.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled", "RequiresFeature")
@Composable
fun WebViewScreen() {
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
    var showLogs by remember { mutableStateOf(false) }
    var showJsInjection by remember { mutableStateOf(false) }
    var showWebViewInfo by remember { mutableStateOf(false) }
    var jsCode by remember { mutableStateOf("") }
    var injectBeforeLoad by remember { mutableStateOf(false) }
    var webViewInfo by remember { mutableStateOf<WebViewInfo?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var pendingFilePath by remember { mutableStateOf<Uri?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadFileName by remember { mutableStateOf("") }

    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    fun addLog(level: LogLevel, message: String, source: LogSource) {
        logs = logs + createLogEntry(level, message, source)
    }

    fun clearLogs() {
        logs = emptyList()
    }

    fun navigateTo(url: String) {
        val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        currentUrl = finalUrl
        urlInput = finalUrl
        webViewRef.value?.loadUrl(finalUrl)
        addLog(LogLevel.INFO, "导航到: $finalUrl", LogSource.WEBVIEW_ACTIVITY)
    }

    fun injectJs(code: String) {
        webViewRef.value?.evaluateJavascript(code) { result ->
            addLog(LogLevel.INFO, "JS 执行结果: $result", LogSource.JAVASCRIPT)
        }
    }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            addLog(LogLevel.INFO, "文件已选择: ${it.lastPathSegment}", LogSource.WEBVIEW_ACTIVITY)
        }
    }

    val downloadLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    pendingFilePath?.let { sourceUri ->
                        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                            inputStream.copyTo(outputStream)
                            addLog(LogLevel.INFO, "文件下载完成: ${downloadFileName}", LogSource.WEBVIEW_ACTIVITY)
                        }
                    }
                }
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, "文件下载失败: ${e.message}", LogSource.WEBVIEW_ACTIVITY)
            }
            pendingFilePath = null
            downloadFileName = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Tester - WebView 测试") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showJsInjection = true }) {
                        Icon(Icons.Default.Code, contentDescription = "JS注入", tint = Color.White)
                    }
                    IconButton(onClick = { showLogs = !showLogs }) {
                        Icon(Icons.Default.List, contentDescription = "日志", tint = Color.White)
                    }
                    IconButton(onClick = { showWebViewInfo = true }) {
                        Icon(Icons.Default.Info, contentDescription = "WebView信息", tint = Color.White)
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
            // URL 输入栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { navigateTo(urlInput) }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { navigateTo(urlInput) }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "导航")
                        }
                    }
                )
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
                        if (webViewRef.value?.canGoBack() == true) {
                            webViewRef.value?.goBack()
                            addLog(LogLevel.INFO, "后退", LogSource.WEBVIEW_ACTIVITY)
                        }
                    },
                    enabled = canGoBack
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "后退")
                }
                IconButton(
                    onClick = {
                        if (webViewRef.value?.canGoForward() == true) {
                            webViewRef.value?.goForward()
                            addLog(LogLevel.INFO, "前进", LogSource.WEBVIEW_ACTIVITY)
                        }
                    },
                    enabled = canGoForward
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "前进")
                }
                IconButton(
                    onClick = { webViewRef.value?.reload() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
                IconButton(
                    onClick = { webViewRef.value?.stopLoading() }
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "停止")
                }
                IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "文件选择")
                }
            }

            // 进度条
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { loadingProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Tab 选择
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("WebView") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("功能面板") }
                )
            }

            // 内容区
            when (selectedTab) {
                0 -> {
                    // WebView
                    Box(modifier = Modifier.weight(1f)) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    settings.allowFileAccess = true
                                    settings.setSupportZoom(true)
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        settings.safeBrowsingEnabled = true
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                            isLoading = true
                                            loadingProgress = 0
                                            url?.let { currentUrl = it }
                                            addLog(LogLevel.INFO, "页面开始加载: $url", LogSource.WEBVIEW_ACTIVITY)
                                            if (injectBeforeLoad && jsCode.isNotEmpty()) {
                                                view?.evaluateJavascript(jsCode, null)
                                            }
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isLoading = false
                                            loadingProgress = 100
                                            canGoBack = view?.canGoBack() == true
                                            canGoForward = view?.canGoForward() == true
                                            addLog(LogLevel.INFO, "页面加载完成: $url", LogSource.WEBVIEW_ACTIVITY)
                                            if (!injectBeforeLoad && jsCode.isNotEmpty()) {
                                                view?.evaluateJavascript(jsCode, null)
                                            }
                                        }

                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            addLog(LogLevel.INFO, "加载 URL: ${request?.url}", LogSource.WEBVIEW_ACTIVITY)
                                            return false
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?
                                        ) {
                                            if (request?.isForMainFrame == true) {
                                                errorMessage = error?.description?.toString() ?: "未知错误"
                                                addLog(LogLevel.ERROR, "加载错误: $errorMessage", LogSource.WEBVIEW_ACTIVITY)
                                            }
                                        }
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            loadingProgress = newProgress
                                        }

                                        override fun onReceivedTitle(view: WebView?, title: String?) {
                                            title?.let {
                                                addLog(LogLevel.INFO, "页面标题: $it", LogSource.WEBVIEW_ACTIVITY)
                                            }
                                        }

                                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                            val level = when (consoleMessage?.messageLevel()) {
                                                ConsoleMessage.MessageLevel.ERROR -> LogLevel.ERROR
                                                ConsoleMessage.MessageLevel.WARNING -> LogLevel.WARNING
                                                else -> LogLevel.INFO
                                            }
                                            addLog(
                                                level,
                                                "[JS Console] ${consoleMessage?.message()}",
                                                LogSource.JAVASCRIPT
                                            )
                                            return super.onConsoleMessage(consoleMessage)
                                        }

                                        override fun onShowFileChooser(
                                            webView: WebView?,
                                            filePathCallback: ValueCallback<Array<Uri>>?,
                                            fileChooserParams: FileChooserParams?
                                        ): Boolean {
                                            addLog(LogLevel.INFO, "文件选择请求", LogSource.WEBVIEW_ACTIVITY)
                                            return true
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
                                            downloadFileName = URLConnection.guessContentTypeFromName(url) ?: "download"
                                            addLog(LogLevel.INFO, "下载请求: $url", LogSource.WEBVIEW_ACTIVITY)
                                            url?.let {
                                                try {
                                                    val request = android.webkit.DownloadManager.Request(Uri.parse(it))
                                                    request.setNotificationVisibility(
                                                        android.webkit.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                                    )
                                                    val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.webkit.DownloadManager
                                                    downloadManager.enqueue(request)
                                                    addLog(LogLevel.INFO, "下载开始: $downloadFileName", LogSource.WEBVIEW_ACTIVITY)
                                                } catch (e: Exception) {
                                                    addLog(LogLevel.ERROR, "下载失败: ${e.message}", LogSource.WEBVIEW_ACTIVITY)
                                                }
                                            }
                                        }
                                    }

                                    loadUrl(currentUrl)
                                    webViewRef.value = this
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { view ->
                                webViewRef.value = view
                            }
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

                1 -> {
                    // 功能面板
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "快速操作",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { showJsInjection = true },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Code, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("JS注入")
                                        }
                                        Button(
                                            onClick = { showLogs = true },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.List, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("查看日志")
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "预设脚本",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            jsCode = """
                                                console.log('=== 页面信息 ===');
                                                console.log('标题: ' + document.title);
                                                console.log('URL: ' + window.location.href);
                                                console.log('域: ' + document.domain);
                                            """.trimIndent()
                                            injectJs(jsCode)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("获取页面信息")
                                    }
                                    Button(
                                        onClick = {
                                            jsCode = """
                                                console.log('=== DOM 结构 ===');
                                                console.log('Body HTML:');
                                                console.log(document.body.innerHTML.substring(0, 500));
                                            """.trimIndent()
                                            injectJs(jsCode)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("获取 DOM")
                                    }
                                    Button(
                                        onClick = {
                                            jsCode = """
                                                console.log('=== 性能信息 ===');
                                                console.log('导航时间: ' + window.performance.timing.navigationStart);
                                                console.log('加载完成: ' + window.performance.timing.loadEventEnd);
                                            """.trimIndent()
                                            injectJs(jsCode)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("获取性能信息")
                                    }
                                }
                            }
                        }

                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "WebView 版本",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("WebView 版本: ${Build.VERSION.SDK_INT}")
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        Text("Safe Browsing: 支持")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // JS 注入对话框
    if (showJsInjection) {
        AlertDialog(
            onDismissRequest = { showJsInjection = false },
            title = { Text("JavaScript 注入") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = injectBeforeLoad,
                            onCheckedChange = { injectBeforeLoad = it }
                        )
                        Text("页面加载前注入")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsCode,
                        onValueChange = { jsCode = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        label = { Text("JavaScript 代码") },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { showJsInjection = false }) {
                        Text("关闭")
                    }
                    Button(
                        onClick = {
                            if (jsCode.isNotEmpty()) {
                                injectJs(jsCode)
                                showJsInjection = false
                            }
                        }
                    ) {
                        Text("注入")
                    }
                }
            }
        )
    }

    // 日志对话框
    if (showLogs) {
        AlertDialog(
            onDismissRequest = { showLogs = false },
            title = { Text("日志查看") },
            text = {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("共 ${logs.size} 条日志")
                        Row {
                            TextButton(onClick = { clearLogs() }) {
                                Text("清除")
                            }
                        }
                    }
                    Divider()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(logs.takeLast(50)) { log ->
                            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            val time = timeFormat.format(Date(log.timestamp))
                            val color = when (log.level) {
                                LogLevel.ERROR -> Color.Red
                                LogLevel.WARNING -> Color.Yellow
                                else -> Color.Unspecified
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row {
                                    Text(
                                        "[$time] ",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        "[${log.source.name}] ",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    log.message,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = color
                                )
                            }
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogs = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // WebView 信息对话框
    if (showWebViewInfo) {
        AlertDialog(
            onDismissRequest = { showWebViewInfo = false },
            title = { Text("WebView 版本信息") },
            text = {
                Column {
                    Text("Android WebView 版本: ${Build.VERSION.SDK_INT}")
                    Text("内核版本: WebView")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("支持的功能:")
                    Text("- JavaScript: 支持")
                    Text("- DOM Storage: 支持")
                    Text("- File Access: 支持")
                    Text("- Wide ViewPort: 支持")
                    Text("- Zoom Controls: 支持")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Text("- Safe Browsing: 支持")
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Text("- Force Dark: 支持")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWebViewInfo = false }) {
                    Text("关闭")
                }
            }
        )
    }
}
