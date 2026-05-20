package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SpeedDialShortcut(
    val title: String,
    val url: String,
    val icon: ImageVector,
    val iconColor: Color
)

@Composable
fun SpeedDialDashboard(
    isPrivate: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Select curated color palette based on mode
    val backgroundBrush = if (isPrivate) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF13131A), // Cosmic charcoal
                Color(0xFF1E1E2A),
                Color(0xFF0F0F14)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.background
            )
        )
    }

    val textColor = if (isPrivate) Color(0xFFE2E2E9) else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isPrivate) Color(0xFF9EA0B0) else MaterialTheme.colorScheme.onSurfaceVariant

    val shortcuts = listOf(
        SpeedDialShortcut("Google", "https://www.google.com", Icons.Default.Search, Color(0xFF4285F4)),
        SpeedDialShortcut("Wikipedia", "https://www.wikipedia.org", Icons.Default.Book, Color(0xFF64748B)),
        SpeedDialShortcut("YouTube", "https://www.youtube.com", Icons.Default.VideoLibrary, Color(0xFFEF4444)),
        SpeedDialShortcut("Reddit", "https://www.reddit.com", Icons.Default.Language, Color(0xFFF97316)),
        SpeedDialShortcut("GitHub", "https://github.com", Icons.Default.Laptop, Color(0xFF0F172A)),
        SpeedDialShortcut("StackOverflow", "https://stackoverflow.com", Icons.Default.Code, Color(0xFFF97316))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo / Icon Anchor
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    if (isPrivate) Color(0xFF2E2E3E) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    CircleShape
                )
                .border(
                    width = 1.dp,
                    color = if (isPrivate) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isPrivate) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = "Incognito Mode icon",
                    tint = Color(0xFFCF8CF5), // Radiant violet accent
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Icon(
                    Icons.Default.CompassCalibration,
                    contentDescription = "Standard Browser logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title text header
        Text(
            text = if (isPrivate) "Private Browsing" else "Web Browser",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (isPrivate) {
                "History, cookies, and cached data won't be saved on this tab."
            } else {
                "Search the web or type a URL to begin"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = subtitleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .widthIn(max = 280.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Large UI speed dial options layout
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .border(
                    width = 1.dp,
                    color = if (isPrivate) Color(0xFF2E2E3E) else MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.large
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isPrivate) Color(0xFF222230) else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                     text = "Quick Access Websites",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isPrivate) Color(0xFFB1B3C4) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    items(shortcuts) { shortcut ->
                        SpeedDialItem(
                            shortcut = shortcut,
                            isPrivate = isPrivate,
                            onClick = { onNavigate(shortcut.url) }
                        )
                    }
                }
            }
        }

        if (isPrivate) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(Color(0xFF2C1E38), shape = CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = Color(0xFFCF8CF5),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Shield Active",
                    color = Color(0xFFCF8CF5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SpeedDialItem(
    shortcut: SpeedDialShortcut,
    isPrivate: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isPrivate) Color(0xFF2F2F44) else MaterialTheme.colorScheme.surface,
                    CircleShape
                )
                .border(
                    width = 1.dp,
                    color = if (isPrivate) Color(0xFF3C3C56) else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                shortcut.icon,
                contentDescription = "${shortcut.title} Link Shortcut",
                tint = if (isPrivate) shortcut.iconColor.copy(alpha = 0.9f) else shortcut.iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = shortcut.title,
            color = if (isPrivate) Color(0xFFCDCDD8) else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(72.dp)
        )
    }
}
