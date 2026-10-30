package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.PermissionStatusState
import com.example.model.QuickMacro
import com.example.service.ZaiDeviceController
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ZaiSessionScreen(
    sessionUrl: String,
    isLoading: Boolean,
    loadProgress: Int,
    isAuthenticated: Boolean,
    cookieSummary: String,
    permissionState: PermissionStatusState,
    desktopMode: Boolean,
    pendingScript: String?,
    quickMacros: List<QuickMacro>,
    deviceController: ZaiDeviceController,
    onUrlSubmitted: (String) -> Unit,
    onPageLoadStateChanged: (Boolean, Int) -> Unit,
    onCookiesInspected: (String?) -> Unit,
    onToggleDesktopMode: () -> Unit,
    onScriptConsumed: () -> Unit,
    onExecuteMacro: (QuickMacro) -> Unit,
    onOpenPermissionsTab: () -> Unit
) {
    var urlInput by remember(sessionUrl) { mutableStateOf(sessionUrl) }
    var showUrlBarEditor by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webViewInstanceKey by remember { mutableIntStateOf(0) }
    var rendererRecoveredNotice by remember { mutableStateOf(false) }

    // Execute any pending JS injection from ViewModel safely
    LaunchedEffect(pendingScript, webViewRef) {
        val wv = webViewRef
        if (pendingScript != null && wv != null) {
            try {
                wv.evaluateJavascript(pendingScript, null)
            } catch (_: Exception) {
            }
            onScriptConsumed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .testTag("zai_session_screen")
    ) {
        // Top Cyber Session Header Bar
        Surface(
            color = ObsidianSurface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Session Security & URL Pill
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianElevated)
                            .border(
                                width = 1.dp,
                                color = if (isAuthenticated) EmeraldActive.copy(alpha = 0.45f) else CyanElectric.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { showUrlBarEditor = !showUrlBarEditor }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("session_url_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (isAuthenticated) EmeraldActive else CyanElectric)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "اتصال مشفر",
                            tint = CyanElectric,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sessionUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = cookieSummary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Readiness Badge Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(CyanElectric.copy(alpha = 0.18f), VioletPulse.copy(alpha = 0.22f))
                                )
                            )
                            .border(1.dp, CyanElectric.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable { onOpenPermissionsTab() }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("readiness_badge_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (permissionState.isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Security,
                            contentDescription = "جاهزية الصلاحيات",
                            tint = if (permissionState.isAccessibilityEnabled) EmeraldActive else AmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${permissionState.readinessScore}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Desktop/Mobile Switch
                    IconButton(
                        onClick = {
                            onToggleDesktopMode()
                            webViewRef?.settings?.apply {
                                userAgentString = buildUserAgent(!desktopMode)
                                useWideViewPort = !desktopMode
                                loadWithOverviewMode = !desktopMode
                            }
                            webViewRef?.reload()
                        },
                        modifier = Modifier.testTag("toggle_desktop_mode_button")
                    ) {
                        Icon(
                            imageVector = if (desktopMode) Icons.Default.Computer else Icons.Default.PhoneAndroid,
                            contentDescription = "تبديل وضع العرض",
                            tint = if (desktopMode) CyanElectric else TextSecondary
                        )
                    }

                    // Open in External Browser fallback
                    IconButton(
                        onClick = { deviceController.searchWebOrOpenUrl(sessionUrl) },
                        modifier = Modifier.testTag("open_external_browser_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "فتح في المتصفح الخارجي",
                            tint = EmeraldActive
                        )
                    }

                    // Reload Session
                    IconButton(
                        onClick = {
                            rendererRecoveredNotice = false
                            webViewRef?.reload()
                        },
                        modifier = Modifier.testTag("reload_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث جلسة Z.ai",
                            tint = CyanElectric
                        )
                    }
                }

                // Expandable URL & Endpoint Switcher
                AnimatedVisibility(visible = showUrlBarEditor) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_url_input"),
                                singleLine = true,
                                label = { Text("رابط جلسة Z.ai أو بوابة الوكيل") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        onUrlSubmitted(urlInput)
                                        webViewRef?.loadUrl(urlInput)
                                        showUrlBarEditor = false
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanElectric,
                                    unfocusedBorderColor = ObsidianGlass
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    onUrlSubmitted(urlInput)
                                    webViewRef?.loadUrl(urlInput)
                                    showUrlBarEditor = false
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CyanElectric)
                                    .testTag("go_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "انتقال",
                                    tint = ObsidianDeep
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "بوابة المحادثة chat.z.ai" to "https://chat.z.ai/",
                                "المنصة الرئيسية z.ai" to "https://z.ai/",
                                "تسجيل الدخول السريع" to "https://chat.z.ai/auth"
                            ).forEach { (title, targetUrl) ->
                                val cleanUrl = targetUrl.trim()
                                ElevatedFilterChip(
                                    selected = sessionUrl.startsWith(cleanUrl),
                                    onClick = {
                                        urlInput = cleanUrl
                                        onUrlSubmitted(cleanUrl)
                                        webViewRef?.loadUrl(cleanUrl)
                                        showUrlBarEditor = false
                                    },
                                    label = { Text(title) }
                                )
                            }
                        }
                    }
                }

                // Quick Agent Bridge Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickMacros.forEach { macro ->
                        ElevatedFilterChip(
                            selected = false,
                            onClick = { onExecuteMacro(macro) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (macro.actionCode.startsWith("INJECT")) Icons.Default.Terminal else Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = CyanElectric,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = macro.titleAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                containerColor = ObsidianElevated,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("macro_chip_${macro.id}")
                        )
                    }
                }

                // Notice banner if Chromium WebView renderer recovered from emulator GPU crash
                AnimatedVisibility(visible = rendererRecoveredNotice) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianElevated)
                            .border(1.dp, CyanElectric.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تمت استعادة معالج العرض تلقائياً بوضع الاستقرار العالي",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldActive,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                rendererRecoveredNotice = false
                                webViewInstanceKey++
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanElectric,
                                contentColor = ObsidianDeep
                            )
                        ) {
                            Text("إعادة تحميل", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                progress = { (loadProgress / 100f).coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = CyanElectric,
                trackColor = ObsidianSurface
            )
        }

        // Persistent Z.ai WebView with Crash-Resilient onRenderProcessGone Handler
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            key(webViewInstanceKey) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("zai_webview_container"),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            // Use Software Layer rendering to avoid Emulator GPU/WebGL renderer crashes (aw_browser_terminator.cc)
                            setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
                            }

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                javaScriptCanOpenWindowsAutomatically = false
                                loadsImagesAutomatically = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                useWideViewPort = desktopMode
                                loadWithOverviewMode = desktopMode
                                userAgentString = buildUserAgent(desktopMode)
                                cacheMode = WebSettings.LOAD_DEFAULT
                            }

                            // Attach Real Native Phone Controller Bridge
                            addJavascriptInterface(deviceController, "ZaiPhoneAgent")

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    onPageLoadStateChanged(true, 15)
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val target = request?.url?.toString() ?: return false
                                    if (target.startsWith("http://") || target.startsWith("https://")) {
                                        return false
                                    }
                                    return true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    onPageLoadStateChanged(false, 100)
                                    try {
                                        CookieManager.getInstance().flush()
                                        val cookies = url?.let { CookieManager.getInstance().getCookie(it) }
                                        onCookiesInspected(cookies)

                                        if (permissionState.isAutoBridgeEnabled) {
                                            view?.evaluateJavascript(buildBridgeObserverScript(), null)
                                        }
                                    } catch (_: Exception) {
                                    }
                                }

                                /**
                                 * CRITICAL: Intercepts Chromium Renderer process crashes (aw_browser_terminator.cc)
                                 * Return `true` so the Android OS does NOT kill the host application!
                                 */
                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    try {
                                        view?.stopLoading()
                                        view?.removeJavascriptInterface("ZaiPhoneAgent")
                                        view?.destroy()
                                    } catch (_: Exception) {
                                    }
                                    webViewRef = null
                                    onPageLoadStateChanged(false, 100)
                                    rendererRecoveredNotice = true
                                    webViewInstanceKey++
                                    return true
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    onPageLoadStateChanged(newProgress < 100, newProgress)
                                }
                            }

                            loadUrl(sessionUrl)
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        webViewRef = view
                    }
                )
            }
        }
    }

    DisposableEffect(webViewInstanceKey) {
        onDispose {
            try {
                CookieManager.getInstance().flush()
                webViewRef?.stopLoading()
            } catch (_: Exception) {
            }
        }
    }
}

private fun buildUserAgent(desktopMode: Boolean): String {
    return if (desktopMode) {
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 ZaiAgentOS/3.0"
    } else {
        "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36 ZaiAgentOS/3.0"
    }
}

/**
 * Lightweight, event-driven bridge helper injected into the Z.ai page without heavy polling.
 */
private fun buildBridgeObserverScript(): String {
    return """
        (function() {
            if (window.__zaiBridgeInitialized) return;
            window.__zaiBridgeInitialized = true;

            window.zaiExecute = function(jsonStr) {
                if (window.ZaiPhoneAgent && window.ZaiPhoneAgent.executeAgentJson) {
                    return window.ZaiPhoneAgent.executeAgentJson(
                        typeof jsonStr === 'string' ? jsonStr : JSON.stringify(jsonStr)
                    );
                }
                return false;
            };

            document.addEventListener('click', function(e) {
                var target = e.target;
                if (!target) return;
                var codeEl = target.closest('code, pre');
                if (codeEl) {
                    var txt = (codeEl.innerText || '').trim();
                    if (txt.startsWith('{') && txt.endsWith('}') && txt.indexOf('"action"') !== -1) {
                        window.ZaiPhoneAgent.executeAgentJson(txt);
                    }
                }
            }, true);
        })();
    """.trimIndent()
}
