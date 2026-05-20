package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val browserDao: BrowserDao) {
    val allBookmarks: Flow<List<Bookmark>> = browserDao.getAllBookmarks()
    val allHistory: Flow<List<HistoryItem>> = browserDao.getAllHistory()

    suspend fun isBookmarked(url: String): Boolean {
        return browserDao.getBookmarkByUrl(url) != null
    }

    suspend fun insertBookmark(title: String, url: String) {
        // Remove if exists to replace/update timestamp
        browserDao.deleteBookmarkByUrl(url)
        browserDao.insertBookmark(Bookmark(title = title, url = url))
    }

    suspend fun removeBookmark(url: String) {
        browserDao.deleteBookmarkByUrl(url)
    }

    suspend fun insertHistoryItem(title: String, url: String) {
        // Skip default/blank pages or internal home URLs
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("chrome:")) return
        
        // Remove existing item with same url so it bumps to top
        browserDao.deleteHistoryByUrl(url)
        browserDao.insertHistoryItem(HistoryItem(title = title, url = url))
    }

    suspend fun deleteHistoryItem(item: HistoryItem) {
        browserDao.deleteHistoryItem(item)
    }

    suspend fun clearHistory() {
        browserDao.clearAllHistory()
    }
}
