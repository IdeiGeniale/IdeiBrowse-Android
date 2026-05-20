package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BrowserTab
import com.example.data.BrowserViewModel

@Composable
fun FindInPagePanel(
    tab: BrowserTab,
    viewModel: BrowserViewModel,
    onFindNext: (forward: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Icon
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        // Text input field
        OutlinedTextField(
            value = tab.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(tab.id, it) },
            placeholder = { Text("Find in page...", fontSize = 13.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .focusRequester(focusRequester),
            shape = MaterialTheme.shapes.small
        )

        // Matches tracker indicator
        if (tab.searchQuery.isNotEmpty()) {
            Text(
                text = if (tab.searchMatchCount > 0) {
                    "${tab.searchMatchIndex}/${tab.searchMatchCount}"
                } else {
                    "0/0"
                },
                color = if (tab.searchMatchCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Navigation controls
        IconButton(
            onClick = { onFindNext(false) }, // Prev/Backward
            enabled = tab.searchMatchCount > 0,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = "Previous match"
            )
        }

        IconButton(
            onClick = { onFindNext(true) }, // Next/Forward
            enabled = tab.searchMatchCount > 0,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Next match"
            )
        }

        // Close search in page
        IconButton(
            onClick = { viewModel.setSearchActive(tab.id, false) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Close search",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
        }
    }
}
