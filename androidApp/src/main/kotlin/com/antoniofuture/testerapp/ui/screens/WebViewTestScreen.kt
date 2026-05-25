package com.antoniofuture.testerapp.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.antoniofuture.testerapp.common.*
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val LOG_MAX_COUNT = 500

private enum class Overlay { NONE, INJECTION, SETTINGS, HISTORY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewTestScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as? Activity
    var currentUrl by remember { mutableStateOf("https://www.example.com") }
    var urlInput by remember { mutableStateOf(currentUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var logs by remember { mutableStateOf(listOf<LogEntry>()) }
    var jsLogs by remember { mutableStateOf(listOf<LogEntry>()) }
    var activityLogs by remember { mutableStateOf(listOf<LogEntry>()) }
    var showLogDrawer by remember { mutableStateOf(false) }
    var showMenuDrawer by remember { mutableStateOf(false) }
    var showCleanupDrawer by remember { mutableStateOf(false) }
    var logSheetExpanded by remember { mutableStateOf(false) }
    var selectedLogTab by remember { mutableStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var recentUrls by remember { mutableStateOf(listOf(currentUrl)) }
    var historyList by remember { mutableStateOf(listOf<HistoryEntry>()) }
    var showUrlDropdown by remember { mutableStateOf(false) }
    var currentOverlay by remember { mutableStateOf(Overlay.NONE) }
    var interceptedUrl by remember { mutableStateOf<String?>(null) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val prefs = remember { context.getSharedPreferences("testerapp_webview_settings", Context.MODE_PRIVATE) }

    fun loadSettings(): SettingsData {
        val raw = prefs.getString("settings_json", null)
        if (raw != null) {
            try {
                val parts = raw.split("|||")
                if (parts.size == 6) {
                    return SettingsData(
                        initialUrl = parts[0].ifEmpty { "https://www.example.com" },
                        downloadPath = parts[1],
                        userAgent = parts[2],
                        enableJavaScript = parts[3] != "false",
                        enableCookies = parts[4] != "false",
                        enableCache = parts[5] != "false"
                    )
                }
            } catch (_: Exception) {}
        }
        return SettingsData()
    }

    fun saveSettings(data: SettingsData) {
        val raw = listOf(
            data.initialUrl, data.downloadPath, data.userAgent,
            data.enableJavaScript.toString(), data.enableCookies.toString(), data.enableCache.toString()
        ).joinToString("|||")
        prefs.edit().putString("settings_json", raw).apply()
    }

    var settings by remember {
        mutableStateOf(loadSettings())
    }

    var injectionScripts by remember {
        mutableStateOf(listOf(InjectionScript("默认脚本", "console.log('TesterApp: 脚本已注入');", true)))
    }

    fun addLog(level: LogLevel, message: String, source: LogSource) {
        val entry = createLogEntry(level, message, source)
        logs = (logs + entry).takeLast(LOG_MAX_COUNT)
        when (source) {
            LogSource.JAVASCRIPT -> jsLogs = (jsLogs + entry).takeLast(LOG_MAX_COUNT)
            LogSource.WEBVIEW_ACTIVITY -> activityLogs = (activityLogs + entry).takeLast(LOG_MAX_COUNT)
            else -> {}
        }
    }

    fun addHistory(url: String) {
        val entry = HistoryEntry(url = url, timestamp = System.currentTimeMillis())
        historyList = listOf(entry) + historyList.filter { it.url != url }
    }

    fun navigateTo(url: String) {
        val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else if (url.contains("://")) {
            url
        } else {
            "https://$url"
        }
        currentUrl = finalUrl
        urlInput = finalUrl
        recentUrls = (listOf(finalUrl) + recentUrls.filter { it != finalUrl }).take(5)
        addHistory(finalUrl)
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

    fun isNonStandardProtocol(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return !lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://") &&
            !lowerUrl.startsWith("javascript:") && !lowerUrl.startsWith("data:") &&
            !lowerUrl.startsWith("about:") && !lowerUrl.startsWith("blob:") &&
            !lowerUrl.startsWith("file:")
    }

    fun handleBlobDownload(blobUrl: String) {
        val escapedUrl = blobUrl.replace("\\", "\\\\").replace("'", "\\'")
        val js = """
            (function() {
                try {
                    fetch('$escapedUrl').then(function(response) {
                        if (!response.ok) {
                            TesterAppBlob.onError('HTTP ' + response.status + ': ' + response.statusText);
                            return;
                        }
                        return response.blob();
                    }).then(function(blob) {
                        if (!blob) return;
                        var reader = new FileReader();
                        reader.onload = function() {
                            TesterAppBlob.onData(reader.result);
                        };
                        reader.onerror = function() {
                            TesterAppBlob.onError('文件读取失败');
                        };
                        reader.readAsDataURL(blob);
                    }).catch(function(e) {
                        TesterAppBlob.onError(e.message || 'fetch 请求失败');
                    });
                } catch(e) {
                    TesterAppBlob.onError(e.message || '未知错误');
                }
            })();
        """.trimIndent()
        webViewRef?.evaluateJavascript(js, null)
    }

    fun saveBlobToFile(dataUrl: String) {
        try {
            val base64 = dataUrl.substringAfter("base64,")
            if (base64.isEmpty()) {
                addLog(LogLevel.ERROR, "Blob 数据为空", LogSource.WEBVIEW_ACTIVITY)
                return
            }
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "download_$timestamp"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    resolver.openOutputStream(it)?.use { out -> out.write(bytes) }
                    addLog(LogLevel.INFO, "Blob 文件已保存到下载目录: $fileName", LogSource.WEBVIEW_ACTIVITY)
                    Toast.makeText(context, "下载完成: $fileName", Toast.LENGTH_SHORT).show()
                }
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, fileName)
                file.writeBytes(bytes)
                addLog(LogLevel.INFO, "Blob 文件已保存: ${file.absolutePath}", LogSource.WEBVIEW_ACTIVITY)
                Toast.makeText(context, "下载完成: $fileName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, "Blob 保存失败: ${e.message}", LogSource.WEBVIEW_ACTIVITY)
            Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateNavigationState(webView: WebView?) {
        canGoBack = webView?.canGoBack() == true
        canGoForward = webView?.canGoForward() == true
    }

    val qrScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { url -> navigateTo(url) }
    }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallback ?: return@rememberLauncherForActivityResult
        filePathCallback = null
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results: Array<Uri>? = if (data?.clipData != null && data.clipData!!.itemCount > 0) {
                Array(data.clipData!!.itemCount) { i -> data.clipData!!.getItemAt(i).uri }
            } else {
                data?.data?.let { arrayOf(it) }
            }
            callback.onReceiveValue(results)
        } else {
            callback.onReceiveValue(null)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("WebView 测试", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { showMenuDrawer = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "菜单", tint = Color.White)
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
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = { navigateTo(urlInput) }
                            ),
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
                                                Text(url, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = {
                            webViewRef?.goBack()
                            updateNavigationState(webViewRef)
                            addLog(LogLevel.INFO, "后退", LogSource.WEBVIEW_ACTIVITY)
                        },
                        enabled = canGoBack
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "后退")
                    }
                    IconButton(
                        onClick = {
                            webViewRef?.goForward()
                            updateNavigationState(webViewRef)
                            addLog(LogLevel.INFO, "前进", LogSource.WEBVIEW_ACTIVITY)
                        },
                        enabled = canGoForward
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "前进")
                    }
                    IconButton(onClick = {
                        if (isLoading) {
                            webViewRef?.stopLoading()
                        } else {
                            webViewRef?.reload()
                        }
                    }) {
                        Icon(
                            if (isLoading) Icons.Default.Stop else Icons.Default.Refresh,
                            contentDescription = if (isLoading) "停止" else "刷新"
                        )
                    }
                    IconButton(onClick = { currentOverlay = Overlay.HISTORY }) {
                        Icon(Icons.Default.History, contentDescription = "历史记录")
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            val appSettings = settings
                            WebView(ctx).apply {
                                this.settings.javaScriptEnabled = appSettings.enableJavaScript
                                this.settings.domStorageEnabled = true
                                this.settings.loadWithOverviewMode = true
                                this.settings.useWideViewPort = true
                                this.settings.builtInZoomControls = true
                                this.settings.displayZoomControls = false
                                this.settings.allowFileAccess = true
                                this.settings.allowContentAccess = true
                                this.settings.setSupportZoom(true)
                                this.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                if (appSettings.enableCookies) {
                                    CookieManager.getInstance().setAcceptCookie(true)
                                } else {
                                    CookieManager.getInstance().setAcceptCookie(false)
                                }
                                if (appSettings.userAgent.isNotEmpty()) {
                                    this.settings.userAgentString = appSettings.userAgent
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    this.settings.safeBrowsingEnabled = true
                                }

                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onData(dataUrl: String) {
                                        activity?.runOnUiThread { saveBlobToFile(dataUrl) }
                                    }

                                    @JavascriptInterface
                                    fun onError(message: String) {
                                        activity?.runOnUiThread {
                                            addLog(LogLevel.ERROR, "Blob 下载错误: $message", LogSource.WEBVIEW_ACTIVITY)
                                            Toast.makeText(context, "下载失败: $message", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }, "TesterAppBlob")

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        if (isNonStandardProtocol(url)) {
                                            interceptedUrl = url
                                            addLog(LogLevel.WARNING, "拦截自定义协议: $url", LogSource.WEBVIEW_ACTIVITY)
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        isLoading = true
                                        loadingProgress = 0
                                        url?.let {
                                            currentUrl = it
                                            urlInput = it
                                            if (it !in recentUrls) {
                                                recentUrls = (listOf(it) + recentUrls).take(5)
                                            }
                                            addHistory(it)
                                        }
                                        updateNavigationState(view)
                                        addLog(LogLevel.INFO, "页面开始加载: $url", LogSource.WEBVIEW_ACTIVITY)
                                        executeInjectionScripts(view!!)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        loadingProgress = 100
                                        updateNavigationState(view)
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

                                    override fun onShowFileChooser(
                                        webView: WebView?,
                                        callback: ValueCallback<Array<Uri>>?,
                                        params: FileChooserParams?
                                    ): Boolean {
                                        val acceptTypes = params?.acceptTypes?.let { types ->
                                            if (types.isEmpty() || (types.size == 1 && types[0].isEmpty())) {
                                                arrayOf("*/*")
                                            } else {
                                                types
                                            }
                                        } ?: arrayOf("*/*")

                                        filePathCallback = callback
                                        try {
                                            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                                addCategory(Intent.CATEGORY_OPENABLE)
                                                type = acceptTypes[0]
                                                if (acceptTypes.size > 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                                    putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes)
                                                }
                                                if (params?.isCaptureEnabled == true) {
                                                    putExtra("android.intent.extra.ALLOW_MULTIPLE", true)
                                                }
                                            }
                                            fileChooserLauncher.launch(Intent.createChooser(intent, "选择文件"))
                                        } catch (e: Exception) {
                                            filePathCallback = null
                                            callback?.onReceiveValue(null)
                                        }
                                        addLog(LogLevel.INFO, "文件选择器已打开", LogSource.WEBVIEW_ACTIVITY)
                                        return true
                                    }
                                }

                                @Suppress("DEPRECATION")
                                setDownloadListener(object : DownloadListener {
                                    override fun onDownloadStart(
                                        url: String?,
                                        userAgent: String?,
                                        contentDisposition: String?,
                                        mimetype: String?,
                                        contentLength: Long
                                    ) {
                                        addLog(LogLevel.INFO, "下载开始: $url", LogSource.WEBVIEW_ACTIVITY)
                                        if (url == null) return
                                        if (url.startsWith("blob:")) {
                                            handleBlobDownload(url)
                                            return
                                        }
                                        try {
                                            val req = DownloadManager.Request(Uri.parse(url))
                                            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            if (appSettings.downloadPath.isNotEmpty()) {
                                                val dir = File(appSettings.downloadPath)
                                                if (dir.exists() || dir.mkdirs()) {
                                                    req.setDestinationUri(Uri.fromFile(dir))
                                                }
                                            } else {
                                                req.setDestinationInExternalPublicDir(
                                                    Environment.DIRECTORY_DOWNLOADS,
                                                    URLUtil.guessFileName(url, contentDisposition, mimetype)
                                                )
                                            }
                                            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                            dm.enqueue(req)
                                            Toast.makeText(context, "下载已开始", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            addLog(LogLevel.ERROR, "下载失败: ${e.message}", LogSource.WEBVIEW_ACTIVITY)
                                        }
                                    }
                                })

                                if (appSettings.enableCache) {
                                    this.settings.cacheMode = WebSettings.LOAD_DEFAULT
                                } else {
                                    this.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                }

                                loadUrl(appSettings.initialUrl)
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("加载错误", color = MaterialTheme.colorScheme.error)
                                Text(error)
                                TextButton(onClick = { errorMessage = null }) { Text("关闭") }
                            }
                        }
                    }
                }
            }
        }

        // Injection overlay
        AnimatedVisibility(
            visible = currentOverlay == Overlay.INJECTION,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                InjectionScreen(
                    scripts = injectionScripts,
                    onBack = { currentOverlay = Overlay.NONE },
                    onScriptChange = { injectionScripts = it }
                )
            }
        }

        // Settings overlay
        AnimatedVisibility(
            visible = currentOverlay == Overlay.SETTINGS,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                SettingsScreen(
                    settings = settings,
                    onBack = { currentOverlay = Overlay.NONE },
                    onSettingsChange = { newSettings ->
                        settings = newSettings
                        saveSettings(newSettings)
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

        // History overlay
        AnimatedVisibility(
            visible = currentOverlay == Overlay.HISTORY,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                HistoryScreen(
                    history = historyList,
                    onBack = { currentOverlay = Overlay.NONE },
                    onNavigate = { url ->
                        currentOverlay = Overlay.NONE
                        navigateTo(url)
                    },
                    onClear = { historyList = emptyList() }
                )
            }
        }

        // Log bottom drawer — two-stage with animation
        AnimatedVisibility(
            visible = showLogDrawer,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showLogDrawer = false }
                )
                val sheetFraction by animateFloatAsState(
                    targetValue = if (logSheetExpanded) 1f else 0.5f,
                    animationSpec = tween(durationMillis = 300)
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(sheetFraction)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .pointerInput(Unit) {
                                var totalDrag = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onDragEnd = {
                                        if (totalDrag < -150f) {
                                            logSheetExpanded = true
                                        } else if (totalDrag > 150f && logSheetExpanded) {
                                            logSheetExpanded = false
                                        }
                                    },
                                    onVerticalDrag = { _, dragAmount -> totalDrag += dragAmount }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = if (logSheetExpanded) "下拖收起" else "上拖展开",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LogBottomSheetContent(
                        jsLogs = jsLogs,
                        activityLogs = activityLogs,
                        selectedTab = selectedLogTab,
                        context = context,
                        onTabSelected = { selectedLogTab = it },
                        onClearJsLogs = { jsLogs = emptyList() },
                        onClearActivityLogs = { activityLogs = emptyList() },
                        onClose = {
                            showLogDrawer = false
                            logSheetExpanded = false
                        }
                    )
                }
            }
        }

        // Menu bottom drawer
        AnimatedVisibility(
            visible = showMenuDrawer,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showMenuDrawer = false }
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Text("功能", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    MenuDrawerItem(Icons.Default.List, "日志", "查看 JS 日志、Activity 日志和系统信息") {
                        showMenuDrawer = false
                        showLogDrawer = true
                    }
                    MenuDrawerItem(Icons.Default.Code, "脚本注入", "配置和管理 JavaScript 注入脚本") {
                        showMenuDrawer = false
                        currentOverlay = Overlay.INJECTION
                    }
                    MenuDrawerItem(Icons.Default.Settings, "设置", "WebView 配置、下载路径、功能开关") {
                        showMenuDrawer = false
                        currentOverlay = Overlay.SETTINGS
                    }
                    MenuDrawerItem(Icons.Default.QrCodeScanner, "二维码扫描", "扫描二维码获取网页地址并导航") {
                        showMenuDrawer = false
                        try {
                            qrScanLauncher.launch(ScanOptions().apply {
                                setPrompt("将二维码对准扫描框")
                                setBeepEnabled(true)
                                setOrientationLocked(true)
                            })
                        } catch (_: Exception) {
                            addLog(LogLevel.ERROR, "无法启动二维码扫描", LogSource.WEBVIEW_ACTIVITY)
                        }
                    }
                    MenuDrawerItem(Icons.Default.CleaningServices, "清理", "清除站点 Cookie、缓存、权限授权") {
                        showMenuDrawer = false
                        showCleanupDrawer = true
                    }
                }
            }
        }

        // Cleanup drawer
        AnimatedVisibility(
            visible = showCleanupDrawer,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showCleanupDrawer = false }
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Text("清理", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    CleanupDrawerContent(
                        currentUrl = currentUrl,
                        webViewRef = webViewRef,
                        addLog = ::addLog,
                        onClose = { showCleanupDrawer = false }
                    )
                }
            }
        }

        // Protocol interception dialog
        interceptedUrl?.let { url ->
            AlertDialog(
                onDismissRequest = { interceptedUrl = null },
                title = { Text("自定义协议") },
                text = {
                    Text("网页尝试打开自定义协议链接：\n\n$url\n\n是否允许触发该协议？")
                },
                confirmButton = {
                    Button(onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            activity?.startActivity(intent)
                            addLog(LogLevel.INFO, "已触发协议: $url", LogSource.WEBVIEW_ACTIVITY)
                        } catch (e: Exception) {
                            addLog(LogLevel.ERROR, "无法处理协议: ${e.message}", LogSource.WEBVIEW_ACTIVITY)
                        }
                        interceptedUrl = null
                    }) { Text("允许") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        addLog(LogLevel.INFO, "已拒绝协议: $url", LogSource.WEBVIEW_ACTIVITY)
                        interceptedUrl = null
                    }) { Text("拒绝") }
                }
            )
        }
    }
}

@Composable
private fun MenuDrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Divider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogBottomSheetContent(
    jsLogs: List<LogEntry>,
    activityLogs: List<LogEntry>,
    selectedTab: Int,
    context: Context,
    onTabSelected: (Int) -> Unit,
    onClearJsLogs: () -> Unit,
    onClearActivityLogs: () -> Unit,
    onClose: () -> Unit
) {
    val activity = context as? Activity
    var filterJs by remember { mutableStateOf("") }
    var filterActivity by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            try {
                val selectedLogs = when (selectedTab) {
                    0 -> jsLogs.filter { filterJs.isEmpty() || it.message.contains(filterJs, ignoreCase = true) }
                    1 -> activityLogs.filter { filterActivity.isEmpty() || it.message.contains(filterActivity, ignoreCase = true) }
                    else -> emptyList()
                }
                val content = formatLogsForExport(selectedLogs)
                context.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(content.toByteArray())
                }
            } catch (_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("日志面板", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "关闭")
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { onTabSelected(0) }, text = { Text("JS") })
            Tab(selected = selectedTab == 1, onClick = { onTabSelected(1) }, text = { Text("Activity") })
            Tab(selected = selectedTab == 2, onClick = { onTabSelected(2) }, text = { Text("信息") })
        }

        if (selectedTab != 2) {
            val currentFilter = if (selectedTab == 0) filterJs else filterActivity
            val onFilterChange: (String) -> Unit = if (selectedTab == 0) { { filterJs = it } } else { { filterActivity = it } }

            OutlinedTextField(
                value = currentFilter,
                onValueChange = onFilterChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                placeholder = { Text("关键字过滤...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索", modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (currentFilter.isNotEmpty()) {
                        IconButton(onClick = { onFilterChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rawLogs = if (selectedTab == 0) jsLogs else activityLogs
                val filteredCount = rawLogs.count { currentFilter.isEmpty() || it.message.contains(currentFilter, ignoreCase = true) }
                Text(
                    "$filteredCount 条",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = {
                    val selectedLogs = rawLogs.filter { currentFilter.isEmpty() || it.message.contains(currentFilter, ignoreCase = true) }
                    val content = formatLogsForExport(selectedLogs)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, content)
                        putExtra(Intent.EXTRA_SUBJECT, "TesterApp 日志导出")
                    }
                    activity?.startActivity(Intent.createChooser(shareIntent, "分享日志"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "分享", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {
                    exportLauncher.launch("testerapp_logs_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt")
                }) {
                    Icon(Icons.Default.FileDownload, contentDescription = "导出", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {
                    when (selectedTab) {
                        0 -> onClearJsLogs()
                        1 -> onClearActivityLogs()
                    }
                }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "清除", modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (selectedTab == 2) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                ProgramInfoContent()
            }
        } else {
            val rawLogs = if (selectedTab == 0) jsLogs else activityLogs
            val currentFilter = if (selectedTab == 0) filterJs else filterActivity
            val displayLogs = rawLogs.filter { currentFilter.isEmpty() || it.message.contains(currentFilter, ignoreCase = true) }

            if (displayLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    items(displayLogs.reversed()) { log ->
                        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        val time = timeFormat.format(Date(log.timestamp))
                        val color = when (log.level) {
                            LogLevel.ERROR -> Color.Red
                            LogLevel.WARNING -> Color(0xFFFFA500)
                            else -> Color.Unspecified
                        }
                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                            Row {
                                Text("[$time] ", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("[${log.source.name}] ", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            Text(log.message, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = color)
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    history: List<HistoryEntry>,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onClear: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(history, searchQuery) {
        if (searchQuery.isEmpty()) history
        else history.filter { it.url.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = onClear) {
                            Text("清空", color = Color.White)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("搜索历史记录...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                }
            )

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无历史记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    items(filtered) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate(entry.url) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val host = try { Uri.parse(entry.url).host ?: entry.url } catch (_: Exception) { entry.url }
                                Text(host, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    entry.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanupDrawerContent(
    currentUrl: String,
    webViewRef: WebView?,
    addLog: (LogLevel, String, LogSource) -> Unit,
    onClose: () -> Unit
) {
    var clearCurrentCookies by remember { mutableStateOf(false) }
    var clearCurrentCache by remember { mutableStateOf(false) }
    var clearCurrentPermissions by remember { mutableStateOf(false) }
    var clearAllCookies by remember { mutableStateOf(false) }
    var clearAllCache by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("选择清理项", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(12.dp))

        Text("当前站点:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        CleanupCheckRow("清理本站 Cookie", clearCurrentCookies) { clearCurrentCookies = it }
        CleanupCheckRow("清理本站缓存", clearCurrentCache) { clearCurrentCache = it }
        CleanupCheckRow("清理本站权限授权", clearCurrentPermissions) { clearCurrentPermissions = it }

        Spacer(modifier = Modifier.height(8.dp))
        Divider()
        Spacer(modifier = Modifier.height(8.dp))

        Text("全局:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        CleanupCheckRow("清理全部 Cookie", clearAllCookies) { clearAllCookies = it }
        CleanupCheckRow("清理全部缓存", clearAllCache) { clearAllCache = it }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val results = mutableListOf<String>()
                try {
                    val host = try { Uri.parse(currentUrl).host } catch (_: Exception) { null }

                    if (clearCurrentCookies && host != null) {
                        val cookieManager = CookieManager.getInstance()
                        val allCookies = cookieManager.getCookie(host) ?: ""
                        val cookies = allCookies.split(";").map { it.trim() }
                        for (cookie in cookies) {
                            val name = cookie.substringBefore("=")
                            cookieManager.setCookie(host, "$name=; Max-Age=-1; Path=/")
                        }
                        cookieManager.flush()
                        results.add("已清理本站 Cookie")
                    }
                    if (clearAllCookies) {
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        results.add("已清理全部 Cookie")
                    }
                    if (clearCurrentCache && host != null) {
                        webViewRef?.clearHistory()
                        results.add("已清理本站缓存（历史）")
                    }
                    if (clearAllCache) {
                        webViewRef?.clearCache(true)
                        webViewRef?.clearHistory()
                        results.add("已清理全部缓存")
                    }
                    if (clearCurrentPermissions) {
                        webViewRef?.clearSslPreferences()
                        results.add("已清理本站权限授权")
                    }

                    clearCurrentCookies = false
                    clearCurrentCache = false
                    clearCurrentPermissions = false
                    clearAllCookies = false
                    clearAllCache = false
                    resultMessage = if (results.isEmpty()) "未选择任何清理项" else results.joinToString("\n")
                    showResultDialog = true
                    addLog(LogLevel.INFO, "清理完成: ${results.joinToString("; ")}", LogSource.WEBVIEW_ACTIVITY)
                } catch (e: Exception) {
                    resultMessage = "清理失败: ${e.message}"
                    showResultDialog = true
                    addLog(LogLevel.ERROR, "清理失败: ${e.message}", LogSource.WEBVIEW_ACTIVITY)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("执行清理")
        }
    }

    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = {
                showResultDialog = false
                onClose()
            },
            title = { Text("清理结果") },
            text = { Text(resultMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showResultDialog = false
                    onClose()
                }) { Text("确定") }
            }
        )
    }
}

@Composable
private fun CleanupCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatLogsForExport(logs: List<LogEntry>): String {
    val sb = StringBuilder()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    sb.appendLine("========== TesterApp 日志导出 ==========")
    sb.appendLine("导出时间: ${dateFormat.format(Date())}")
    sb.appendLine("日志数量: ${logs.size}")
    sb.appendLine("========================================\n")
    for (log in logs) {
        val time = dateFormat.format(Date(log.timestamp))
        val level = when (log.level) {
            LogLevel.DEBUG -> "DEBUG"
            LogLevel.INFO -> "INFO"
            LogLevel.WARNING -> "WARN"
            LogLevel.ERROR -> "ERROR"
        }
        sb.appendLine("[$time] [$level] [${log.source.name}] ${log.message}")
    }
    return sb.toString()
}

@Composable
fun ProgramInfoContent() {
    Column(modifier = Modifier.fillMaxWidth()) {
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun FeatureItem(name: String, supported: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
private fun InjectionScreen(
    scripts: List<InjectionScript>,
    onBack: () -> Unit,
    onScriptChange: (List<InjectionScript>) -> Unit
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editName by remember { mutableStateOf("") }
    var editCode by remember { mutableStateOf("") }
    var newScriptName by remember { mutableStateOf("") }
    var newScriptCode by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }

    fun startEditing(index: Int) {
        val script = scripts[index]
        editingIndex = index
        editName = script.name
        editCode = script.code
        showAddForm = false
        newScriptName = ""
        newScriptCode = ""
    }

    fun cancelEditing() { editingIndex = null; editName = ""; editCode = "" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("脚本注入配置", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text("已配置的脚本 (${scripts.size}):", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            scripts.forEachIndexed { index, script ->
                if (editingIndex == index) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("编辑脚本", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("脚本名称") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = editCode, onValueChange = { editCode = it }, label = { Text("JavaScript 代码") }, modifier = Modifier.fillMaxWidth().height(120.dp), textStyle = TextStyle(fontFamily = FontFamily.Monospace))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { cancelEditing() }) { Text("取消") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    if (editName.isNotEmpty() && editCode.isNotEmpty()) {
                                        val newList = scripts.toMutableList()
                                        newList[index] = InjectionScript(editName, editCode, scripts[index].enabled)
                                        onScriptChange(newList)
                                        cancelEditing()
                                    }
                                }) { Text("保存") }
                            }
                        }
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = script.enabled, onCheckedChange = { enabled ->
                                val newList = scripts.toMutableList()
                                newList[index] = script.copy(enabled = enabled)
                                onScriptChange(newList)
                            })
                            Column(modifier = Modifier.weight(1f)) {
                                Text(script.name, style = MaterialTheme.typography.bodyMedium)
                                Text(script.code.take(50) + if (script.code.length > 50) "..." else "", style = MaterialTheme.typography.bodySmall, maxLines = 2, fontFamily = FontFamily.Monospace)
                            }
                            IconButton(onClick = { startEditing(index) }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                val newList = scripts.toMutableList()
                                newList.removeAt(index)
                                onScriptChange(newList)
                                if (editingIndex == index) cancelEditing()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (!showAddForm) {
                OutlinedButton(onClick = { showAddForm = true; cancelEditing() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加新脚本")
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("添加新脚本", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = newScriptName, onValueChange = { newScriptName = it }, label = { Text("脚本名称") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = newScriptCode, onValueChange = { newScriptCode = it }, label = { Text("JavaScript 代码") }, modifier = Modifier.fillMaxWidth().height(120.dp), textStyle = TextStyle(fontFamily = FontFamily.Monospace))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { showAddForm = false; newScriptName = ""; newScriptCode = "" }) { Text("取消") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                if (newScriptName.isNotEmpty() && newScriptCode.isNotEmpty()) {
                                    onScriptChange(scripts + InjectionScript(newScriptName, newScriptCode, true))
                                    newScriptName = ""; newScriptCode = ""; showAddForm = false
                                }
                            }) { Text("添加") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    settings: SettingsData,
    onBack: () -> Unit,
    onSettingsChange: (SettingsData) -> Unit
) {
    var localSettings by remember(settings) { mutableStateOf(settings) }

    LaunchedEffect(settings) {
        localSettings = settings
    }

    val dirPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            localSettings = localSettings.copy(downloadPath = it.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    TextButton(onClick = {
                        onSettingsChange(localSettings)
                        onBack()
                    }) { Text("保存", color = Color.White) }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            OutlinedTextField(
                value = localSettings.initialUrl,
                onValueChange = { localSettings = localSettings.copy(initialUrl = it) },
                label = { Text("初始页面地址") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = localSettings.userAgent,
                onValueChange = { localSettings = localSettings.copy(userAgent = it) },
                label = { Text("User-Agent (留空使用默认)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("下载文件保存位置", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = localSettings.downloadPath.ifEmpty { "未设置 (默认 Download/TesterApp)" },
                    onValueChange = {},
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    label = { Text("保存路径") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { try { dirPickerLauncher.launch(null) } catch (_: Exception) {} }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "选择目录")
                }
            }
            if (localSettings.downloadPath.isNotEmpty()) {
                TextButton(onClick = { localSettings = localSettings.copy(downloadPath = "") }) { Text("重置为默认路径") }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("功能开关", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            SwitchRow("启用 JavaScript", localSettings.enableJavaScript) { localSettings = localSettings.copy(enableJavaScript = it) }
            SwitchRow("启用 Cookie", localSettings.enableCookies) { localSettings = localSettings.copy(enableCookies = it) }
            SwitchRow("启用缓存", localSettings.enableCache) { localSettings = localSettings.copy(enableCache = it) }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { onSettingsChange(localSettings); onBack() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存设置") }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

data class InjectionScript(
    val name: String,
    val code: String,
    val enabled: Boolean = true
)

data class SettingsData(
    val initialUrl: String = "https://www.example.com",
    val downloadPath: String = "",
    val userAgent: String = "",
    val enableJavaScript: Boolean = true,
    val enableCookies: Boolean = true,
    val enableCache: Boolean = true
)

data class HistoryEntry(
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)
