package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import com.example.data.BookEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: ReaderViewModel,
    onNavigateToReader: (Int) -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val books = uiState.books

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.AutoStories, contentDescription = "Logo", tint = MaterialTheme.colorScheme.primary)
                        Text("Lumen", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    Icon(Icons.Default.CloudDone, contentDescription = "Cloud", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp, start = 8.dp)
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(100.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("JS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Library") },
                    label = { Text("Biblioteca") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onBackground,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToExplore() },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                    label = { Text("Explorar") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.CloudSync, contentDescription = "Sync") },
                    label = { Text("Nuvem") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToSettings() },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Ajustes") }
                )
            }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // "Lendo Agora" Section
            books.firstOrNull()?.let { currentBook ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("LENDO AGORA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                        Text("${(currentBook.progress * 100).toInt()}% concluído", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.clickable { onNavigateToReader(currentBook.id) }
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 96.dp, height = 120.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = currentBook.coverUrl,
                                    contentDescription = currentBook.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                )
                            }
                            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.height(120.dp)) {
                                Text(currentBook.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                Text("Unknown Author", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(100.dp)) {
                                        Text(currentBook.type, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                    }
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(100.dp)) {
                                        Text("VERTICAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Settings Section Removed for cleaner UI

            // Library Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("BIBLIOTECA RECENTE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val fallbackBooks = books.take(2)
                    fallbackBooks.forEach { book ->
                        Column(modifier = Modifier.weight(1f).clickable { onNavigateToReader(book.id) }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.aspectRatio(0.75f).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Text(book.type.uppercase(), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(book.title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f).clickable { onNavigateToExplore() }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.aspectRatio(0.75f), contentAlignment = Alignment.Center) {
                            Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(12.dp), color = Color.Transparent, border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                        Text("Importar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

