package com.example.ui.components

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.BrowserTab
import com.example.data.BrowserViewModel

@Composable
fun WebViewContainer(
    tab: BrowserTab,
    viewModel: BrowserViewModel,
    webViewMap: MutableMap<String, WebView>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val webView = remember(tab.id) {
        webViewMap.getOrPut(tab.id) {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }

                // Initial private settings
                if (tab.isPrivate) {
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    clearCache(true)
                } else {
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                }

                // Store mobile User Agent in the tag as fallback
                tag = settings.userAgentString

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        viewModel.updateTabUrl(tab.id, url ?: "")
                        viewModel.updateTabProgress(tab.id, 0, true)
                        viewModel.updateNavigationState(tab.id, canGoBack(), canGoForward())
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        viewModel.updateTabUrl(tab.id, url ?: "")
                        viewModel.updateTabProgress(tab.id, 100, false)
                        viewModel.updateNavigationState(tab.id, canGoBack(), canGoForward())

                        val pageTitle = view?.title ?: ""
                        viewModel.updateTabTitle(tab.id, pageTitle)
                        viewModel.addHistoryEntry(pageTitle, url ?: "", tab.isPrivate)
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        viewModel.updateNavigationState(tab.id, canGoBack(), canGoForward())
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        return if (url.startsWith("http://") || url.startsWith("https://")) {
                            false // let WebView render HTTP links
                        } else {
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(url)
                                )
                                context.startActivity(intent)
                                true
                            } catch (e: Exception) {
                                true // consume unmappable intent to avoid crash
                            }
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        viewModel.updateTabProgress(tab.id, newProgress, newProgress < 100)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        viewModel.updateTabTitle(tab.id, title ?: "")
                    }
                }

                setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                    viewModel.updateSearchMatches(
                        tab.id,
                        if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0,
                        numberOfMatches
                    )
                }

                // Load initial URL if relevant
                if (tab.url != "about:blank" && tab.url.isNotBlank()) {
                    loadUrl(tab.url)
                }
            }
        }
    }

    // Intercept hardware system back button if active tab can navigate within history
    BackHandler(enabled = tab.canGoBack) {
        webView.goBack()
    }

    // React to Desktop Mode switches
    LaunchedEffect(tab.isDesktopMode) {
        val originalUA = webView.tag as? String ?: webView.settings.userAgentString
        webView.settings.apply {
            userAgentString = if (tab.isDesktopMode) {
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            } else {
                originalUA
            }
            useWideViewPort = tab.isDesktopMode
            loadWithOverviewMode = tab.isDesktopMode
        }
        // Force reload with new agent configuration
        if (webView.url != null && webView.url != "about:blank") {
            webView.reload()
        }
    }

    // Handle Private Mode cookies and storage
    LaunchedEffect(tab.isPrivate) {
        val cookieManager = CookieManager.getInstance()
        if (tab.isPrivate) {
            cookieManager.setAcceptCookie(false)
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webView.clearCache(true)
        } else {
            cookieManager.setAcceptCookie(true)
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        }
    }

    // Handle Active Find in Page triggers
    LaunchedEffect(tab.searchActive, tab.searchQuery) {
        if (tab.searchActive && tab.searchQuery.isNotEmpty()) {
            webView.findAllAsync(tab.searchQuery)
        } else {
            webView.clearMatches()
            viewModel.updateSearchMatches(tab.id, 0, 0)
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize(),
        update = { wv ->
            // Trigger load if URL was mutated outside (e.g., speed dial click, history click)
            val currentWebUrl = wv.url ?: ""
            if (tab.url != "about:blank" && tab.url.isNotBlank() && currentWebUrl != tab.url) {
                wv.loadUrl(tab.url)
            }
        }
    )
}
