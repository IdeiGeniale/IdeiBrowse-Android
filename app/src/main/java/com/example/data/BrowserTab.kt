package com.example.data

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "about:blank",
    val title: String = "New Tab",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isPrivate: Boolean = false,
    val searchActive: Boolean = false,
    val searchQuery: String = "",
    val searchMatchIndex: Int = 0,
    val searchMatchCount: Int = 0
) {
    val isNewTab: Boolean
        get() = url == "about:blank" || url.isBlank()
}
