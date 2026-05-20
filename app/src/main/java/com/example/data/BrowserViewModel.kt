package com.example.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BrowserDatabase.getDatabase(application)
    private val repository = BrowserRepository(db.browserDao())

    // UI state flows
    val bookmarksState = repository.allBookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val historyState = repository.allHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Tabs state
    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    // Derived State: Active BrowserTab object
    val activeTab: StateFlow<BrowserTab?> = combine(_tabs, _activeTabId) { tabs, activeId ->
        tabs.find { it.id == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Bookmarked state for the current active tab's URL
    val isCurrentTabBookmarked = combine(activeTab, bookmarksState) { tab, bookmarks ->
        tab?.let { t -> bookmarks.any { it.url == t.url } } ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // Initialize with at least one active tab on first launch
        addNewTab("about:blank", isPrivate = false)
    }

    // --- Tab Actions ---
    fun addNewTab(url: String = "about:blank", isPrivate: Boolean = false) {
        val normalizedUrl = normalizeUrl(url)
        val newTab = BrowserTab(
            url = normalizedUrl,
            title = if (normalizedUrl == "about:blank") "New Tab" else "Loading...",
            isPrivate = isPrivate
        )
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        val closedTabIndex = currentTabs.indexOfFirst { it.id == tabId }
        if (closedTabIndex == -1) return

        val newTabs = currentTabs.filterNot { it.id == tabId }
        _tabs.value = newTabs

        if (_activeTabId.value == tabId) {
            if (newTabs.isNotEmpty()) {
                // Select next available tab or the last one
                val nextSelectedIndex = if (closedTabIndex < newTabs.size) closedTabIndex else newTabs.size - 1
                _activeTabId.value = newTabs[nextSelectedIndex].id
            } else {
                // If all tabs closed, create a default clean tab
                addNewTab("about:blank", isPrivate = false)
            }
        }
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
    }

    // Update status from WebView callbacks
    fun updateTabUrl(tabId: String, url: String) {
        _tabs.update { list ->
            list.map {
                if (it.id == tabId) it.copy(url = url) else it
            }
        }
    }

    fun updateTabProgress(tabId: String, progress: Int, isLoading: Boolean) {
        _tabs.update { list ->
            list.map {
                if (it.id == tabId) it.copy(progress = progress, isLoading = isLoading) else it
            }
        }
    }

    fun updateNavigationState(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        _tabs.update { list ->
            list.map {
                if (it.id == tabId) it.copy(canGoBack = canGoBack, canGoForward = canGoForward) else it
            }
        }
    }

    fun updateTabTitle(tabId: String, title: String) {
        _tabs.update { list ->
            list.map {
                if (it.id == tabId) {
                    val finalTitle = title.ifBlank { "Unlabeled Page" }
                    it.copy(title = finalTitle)
                } else it
            }
        }
    }

    // Toggle Incognito/Theme at Tab Level
    fun togglePrivateMode(tabId: String) {
        _tabs.update { list ->
            list.map {
                if (it.id == tabId) it.copy(isPrivate = !it.isPrivate) else it
            }
        }
    }

    // Toggle Desktop Mode at Tab Level
    fun toggleDesktopMode(tabId: String) {
        _tabs.update { list ->
            list.map {
                if (it.id == tabId) it.copy(isDesktopMode = !it.isDesktopMode) else it
            }
        }
    }

    // Find in Page
    fun setSearchActive(tabId: String, active: Boolean) {
        _tabs.update { list ->
            list.map {
                if (it.id == tabId) {
                    if (!active) {
                        it.copy(searchActive = false, searchQuery = "", searchMatchCount = 0, searchMatchIndex = 0)
                    } else {
                        it.copy(searchActive = true)
                    }
                } else it
            }
        }
    }

    fun updateSearchQuery(tabId: String, query: String) {
        _tabs.update { list ->
            list.map {
                if (it.id == tabId) it.copy(searchQuery = query) else it
            }
        }
    }

    fun updateSearchMatches(tabId: String, index: Int, total: Int) {
        _tabs.update { list ->
            list.map {
                if (it.id == tabId) it.copy(searchMatchIndex = index, searchMatchCount = total) else it
            }
        }
    }

    // --- Bookmarks & History Operations ---
    fun toggleBookmark(title: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.isBookmarked(url)) {
                repository.removeBookmark(url)
            } else {
                repository.insertBookmark(title.ifBlank { url }, url)
            }
        }
    }

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBookmark(title, url)
        }
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeBookmark(url)
        }
    }

    fun addHistoryEntry(title: String, url: String, tabIsPrivate: Boolean) {
        if (tabIsPrivate) return // Do not persist history in incognito tabs
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertHistoryItem(title, url)
        }
    }

    fun deleteHistoryEntry(item: HistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHistoryItem(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    // --- URL Normalization Helper ---
    fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "about:blank"
        if (trimmed == "about:blank") return trimmed

        // Check if it's already a complete valid HTTP/HTTPS URL
        if (trimmed.startsWith("http://", ignoreCase = true) || 
            trimmed.startsWith("https://", ignoreCase = true) || 
            trimmed.startsWith("file://", ignoreCase = true) || 
            trimmed.startsWith("content://", ignoreCase = true)) {
            return trimmed
        }

        // Check if it looks like a domain name: standard regex for domains
        val domainRegex = "^([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(/.*)?$".toRegex()
        val hasSpaces = trimmed.contains(" ")

        return if (!hasSpaces && domainRegex.matches(trimmed)) {
            "https://$trimmed"
        } else {
            // Treat as a search query
            try {
                "https://www.google.com/search?q=" + URLEncoder.encode(trimmed, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                "https://www.google.com/search?q=$trimmed"
            }
        }
    }
}
