package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "Documento Indefinido"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ReaderViewModel,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var importedFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        // In a real app, we would process these content URIs and insert them into the Room database
        importedFiles = importedFiles + uris
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explorar", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground) },
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
                        selected = false,
                        onClick = onNavigateToLibrary,
                        icon = { Icon(Icons.Default.Book, contentDescription = "Library") },
                        label = { Text("Biblioteca") }
                    )
                    NavigationBarItem(
                        selected = true,
                        onClick = { },
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                        label = { Text("Explorar") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { },
                        icon = { Icon(Icons.Default.CloudSync, contentDescription = "Sync") },
                        label = { Text("Nuvem") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onNavigateToSettings,
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().clickable {
                    // Mime types: epub, pdf, cbz, cbr
                    filePickerLauncher.launch(arrayOf("application/epub+zip", "application/pdf", "application/x-cbz", "application/x-cbr"))
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Local Storage", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Armazenamento Local", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Procurar arquivos no dispositivo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().clickable {
                    // Launching with generic types to allow the Picker to also open Google Drive explicitly.
                    // Google Drive integration is native to Android via SAF!
                    filePickerLauncher.launch(arrayOf("application/epub+zip", "application/pdf", "application/x-cbz", "application/x-cbr"))
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AddToDrive, contentDescription = "Google Drive", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Google Drive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Importar da sua nuvem", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (importedFiles.isNotEmpty()) {
                Text("ARQUIVOS ENCONTRADOS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(importedFiles) { uri ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = "File", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    val fileName = getFileName(context, uri)
                                    Text(fileName, maxLines = 1, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Pendente de Inclusão", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(
                                    Icons.Default.AddCircleOutline, 
                                    contentDescription = "Add to Library", 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        val fileName = getFileName(context, uri)
                                        viewModel.insertBook(uri, fileName)
                                        importedFiles = importedFiles.filter { it != uri }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
