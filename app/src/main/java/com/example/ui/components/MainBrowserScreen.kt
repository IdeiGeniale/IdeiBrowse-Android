package com.example.ui.components

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isCurrentTabBookmarked.collectAsStateWithLifecycle()

    val bookmarks by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val history by viewModel.historyState.collectAsStateWithLifecycle()

    // Persistent Map of in-memory WebViews, matching individual Tab IDs
    val webViewMap = remember { mutableStateOf(mutableMapOf<String, WebView>()) }

    var addressInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Modal Sheet Controllers
    var showTabsSheet by remember { mutableStateOf(false) }
    var showLibrarySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Sync input address when URLs are loaded/swapped
    LaunchedEffect(activeTab?.url) {
        val currentUrl = activeTab?.url ?: ""
        addressInput = if (currentUrl == "about:blank") "" else currentUrl
    }

    val isDarkBackground = activeTab?.isPrivate == true
    val themeContainerColor = if (isDarkBackground) Color(0xFF1B1B26) else MaterialTheme.colorScheme.surfaceVariant
    val themeContentColor = if (isDarkBackground) Color(0xFFE2E2E9) else MaterialTheme.colorScheme.onSurface
    val textBarBorderColor = if (isDarkBackground) Color(0xFF333348) else MaterialTheme.colorScheme.outline

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Address Toolbar & Progress Bar Container
            Surface(
                color = themeContainerColor,
                contentColor = themeContentColor,
                tonalElevation = 2.dp,
                modifier = Modifier.statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Connection Security indicator or Logo icon
                        val isHttps = activeTab?.url?.startsWith("https://", ignoreCase = true) == true
                        val isBlank = activeTab?.isNewTab == true

                        val safetyIcon = when {
                            isBlank -> Icons.Default.Language
                            isHttps -> Icons.Default.Lock
                            else -> Icons.Default.Warning
                        }
                        val safetyTint = when {
                            isBlank -> if (activeTab?.isPrivate == true) Color(0xFFCF8CF5) else MaterialTheme.colorScheme.primary
                            isHttps -> if (activeTab?.isPrivate == true) Color(0xFF81C784) else Color(0xFF15803D) // Safety Green
                            else -> if (activeTab?.isPrivate == true) Color(0xFFFFB74D) else Color(0xFFFF9800) // Warning yellow
                        }

                        Icon(
                            safetyIcon,
                            contentDescription = "Security Status indicator",
                            tint = safetyTint,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(2.dp)
                        )

                        // URL Search input address bar
                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            placeholder = {
                                Text(
                                    "Search Google or type URL",
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isDarkBackground) Color(0xFF9EA0B0) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    focusManager.clearFocus()
                                    activeTab?.let { tab ->
                                        val destination = viewModel.normalizeUrl(addressInput)
                                        viewModel.updateTabUrl(tab.id, destination)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (isDarkBackground) Color(0xFF262635) else MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = if (isDarkBackground) Color(0xFF222230) else MaterialTheme.colorScheme.surface,
                                focusedBorderColor = if (isDarkBackground) Color(0xFFCF8CF5) else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = textBarBorderColor
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 13.sp,
                                color = themeContentColor
                            ),
                            shape = RoundedCornerShape(50), // Fully rounded address bar (rounded-full)
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    if (addressInput.isNotEmpty()) {
                                        IconButton(onClick = { addressInput = "" }) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear address bar",
                                                tint = if (isDarkBackground) Color(0xFFB1B3C4) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Bookmark Toggle Anchor inside textfield
                                    IconButton(
                                        onClick = {
                                            activeTab?.let { tab ->
                                                viewModel.toggleBookmark(tab.title, tab.url)
                                            }
                                        },
                                        enabled = activeTab?.isNewTab == false
                                    ) {
                                        Icon(
                                            if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Bookmark this page",
                                            tint = if (isBookmarked) {
                                                Color(0xFFFFB300) // Radiant bookmark yellow
                                            } else {
                                                if (isDarkBackground) Color(0xFFB1B3C4) else MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        )

                        // Reload / Navigation action button
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                activeTab?.let { tab ->
                                    val currentWeb = webViewMap.value[tab.id]
                                    if (tab.isLoading) {
                                        currentWeb?.stopLoading()
                                    } else {
                                        if (tab.isNewTab) {
                                            val dest = viewModel.normalizeUrl(addressInput)
                                            viewModel.updateTabUrl(tab.id, dest)
                                        } else {
                                            currentWeb?.reload()
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (activeTab?.isLoading == true) Icons.Default.Close else Icons.Default.Refresh,
                                contentDescription = if (activeTab?.isLoading == true) "Stop Loading" else "Refresh Page",
                                tint = if (isDarkBackground) Color(0xFFE2E2E9) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Progress Loader indicator
                    AnimatedVisibility(
                        visible = activeTab?.isLoading == true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val loadValue = (activeTab?.progress ?: 0) / 100f
                        LinearProgressIndicator(
                            progress = { loadValue },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp),
                            color = if (activeTab?.isPrivate == true) Color(0xFFCF8CF5) else MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }

                    // Find in Page Panel Overlay (horizontal tray below URL bar)
                    if (activeTab?.searchActive == true) {
                        activeTab?.let { tab ->
                            FindInPagePanel(
                                tab = tab,
                                viewModel = viewModel,
                                onFindNext = { forward ->
                                    webViewMap.value[tab.id]?.findNext(forward)
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Elegant Navigation Anchor Deck
            Surface(
                color = themeContainerColor,
                contentColor = themeContentColor,
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier
                    .navigationBarsPadding() // Full notch/gesture bar safety
                    .border(
                        width = 1.dp,
                        color = if (isDarkBackground) Color(0xFF2E2E3E) else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Key
                    IconButton(
                        onClick = {
                            activeTab?.let { tab ->
                                webViewMap.value[tab.id]?.goBack()
                            }
                        },
                        enabled = activeTab?.canGoBack == true
                    ) {
                        Icon(
                            Icons.Default.ArrowBackIos,
                            contentDescription = "Back page navigation",
                            tint = if (activeTab?.canGoBack == true) {
                                if (isDarkBackground) Color(0xFFCF8CF5) else MaterialTheme.colorScheme.primary
                            } else {
                                if (isDarkBackground) Color(0xFF4C4C5C) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Forward Key
                    IconButton(
                        onClick = {
                            activeTab?.let { tab ->
                                webViewMap.value[tab.id]?.goForward()
                            }
                        },
                        enabled = activeTab?.canGoForward == true
                    ) {
                        Icon(
                            Icons.Default.ArrowForwardIos,
                            contentDescription = "Forward page navigation",
                            tint = if (activeTab?.canGoForward == true) {
                                if (isDarkBackground) Color(0xFFCF8CF5) else MaterialTheme.colorScheme.primary
                            } else {
                                if (isDarkBackground) Color(0xFF4C4C5C) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Home Key
                    IconButton(
                        onClick = {
                            activeTab?.let { tab ->
                                focusManager.clearFocus()
                                viewModel.updateTabUrl(tab.id, "about:blank")
                                viewModel.updateTabTitle(tab.id, "New Tab")
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Navigate Home dashboard",
                            tint = if (isDarkBackground) Color(0xFFB1B3C4) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Tabs Count Picker Box
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDarkBackground) Color(0xFF2E2E3E) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isDarkBackground) Color(0xFFCF8CF5) else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { showTabsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${tabs.size}",
                            color = if (isDarkBackground) Color(0xFFE2E2E9) else MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Menu Picker
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Open web options drawer",
                            tint = if (isDarkBackground) Color(0xFFB1B3C4) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Master Viewport Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            activeTab?.let { tab ->
                if (tab.isNewTab) {
                    SpeedDialDashboard(
                        isPrivate = tab.isPrivate,
                        onNavigate = { dest ->
                            viewModel.updateTabUrl(tab.id, viewModel.normalizeUrl(dest))
                        }
                    )
                } else {
                    WebViewContainer(
                        tab = tab,
                        viewModel = viewModel,
                        webViewMap = webViewMap.value,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // --- Bottom Sheet Overlays ---

        // 1. Tab Manager Sheet
        if (showTabsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTabsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                TabsManagerSheet(
                    tabs = tabs,
                    activeTabId = activeTabId,
                    onSelectTab = { viewModel.selectTab(it) },
                    onCloseTab = { cid ->
                        webViewMap.value[cid]?.destroy()
                        webViewMap.value.remove(cid)
                        viewModel.closeTab(cid)
                    },
                    onAddNewTab = { privateSession ->
                        viewModel.addNewTab("about:blank", privateSession)
                    },
                    onDismiss = { showTabsSheet = false }
                )
            }
        }

        // 2. Library Hub Sheet (Bookmarks & History tabs)
        if (showLibrarySheet) {
            ModalBottomSheet(
                onDismissRequest = { showLibrarySheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                HistoryBookmarksSheet(
                    bookmarks = bookmarks,
                    history = history,
                    onSelectUrl = { targetUrl ->
                        activeTab?.let { tab ->
                            viewModel.updateTabUrl(tab.id, targetUrl)
                        }
                    },
                    onDeleteBookmark = { url -> viewModel.removeBookmark(url) },
                    onDeleteHistoryItem = { item -> viewModel.deleteHistoryEntry(item) },
                    onClearHistory = { viewModel.clearHistory() },
                    onDismiss = { showLibrarySheet = false }
                )
            }
        }

        // 3. Quick Options Menu Drawer
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Browser Options",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Divider(modifier = Modifier.padding(bottom = 12.dp))

                    // Mode toggles
                    activeTab?.let { tab ->
                        // Switch Private Session
                        ListItem(
                            headlineContent = { Text("Private Incognito Mode") },
                            supportingContent = { Text("Hide searches and local traces") },
                            leadingContent = {
                                Icon(
                                    if (tab.isPrivate) Icons.Default.Shield else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (tab.isPrivate) Color(0xFFCF8CF5) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = tab.isPrivate,
                                    onCheckedChange = {
                                        viewModel.togglePrivateMode(tab.id)
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.togglePrivateMode(tab.id)
                            }
                        )

                        // Switch Desktop Mode
                        ListItem(
                            headlineContent = { Text("Desktop Website Version") },
                            supportingContent = { Text("Request desktop instead of mobile UA") },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Laptop,
                                    contentDescription = null,
                                    tint = if (tab.isDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = tab.isDesktopMode,
                                    onCheckedChange = {
                                        viewModel.toggleDesktopMode(tab.id)
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.toggleDesktopMode(tab.id)
                            }
                        )

                        // Search Text inside active web page
                        ListItem(
                            headlineContent = { Text("Find in Page") },
                            supportingContent = { Text("Search and highlight page keywords") },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null
                                )
                            },
                            modifier = if (!tab.isNewTab) {
                                Modifier.clickable {
                                    showSettingsSheet = false
                                    viewModel.setSearchActive(tab.id, true)
                                }
                            } else {
                                Modifier
                            },
                            colors = if (tab.isNewTab) {
                                ListItemDefaults.colors(
                                    headlineColor = themeContentColor.copy(alpha = 0.38f),
                                    supportingColor = themeContentColor.copy(alpha = 0.38f),
                                    leadingIconColor = themeContentColor.copy(alpha = 0.38f)
                                )
                            } else {
                                ListItemDefaults.colors()
                            }
                        )
                    }

                    // Open Library Hub
                    ListItem(
                        headlineContent = { Text("Library Hub") },
                        supportingContent = { Text("Saved Bookmarks & Local History log") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.clickable {
                            showSettingsSheet = false
                            showLibrarySheet = true
                        }
                    )
                }
            }
        }
    }
}
