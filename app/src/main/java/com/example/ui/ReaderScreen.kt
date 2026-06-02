package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.BookEntity
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow

import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import android.app.Activity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    bookId: Int,
    viewModel: ReaderViewModel,
    onBack: () -> Unit
) {
    val bookFlow = remember(bookId) { viewModel.getBook(bookId) }
    val book by bookFlow.collectAsState()

    if (book == null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        ReaderScreenContent(safeBook = book!!, viewModel = viewModel, onBack = onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreenContent(
    safeBook: BookEntity,
    viewModel: ReaderViewModel,
    onBack: () -> Unit
) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window

    val uiState by viewModel.uiState.collectAsState()
    val isRtl = uiState.isRtl
    val isScrollMode = uiState.isScrollMode
    val isHorizontal = !isScrollMode

    var isUiVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    var pdfCache by remember { mutableStateOf<PdfPageCache?>(null) }
    var actualPageCount by remember { mutableStateOf(10) }
    
    if (safeBook.type == "PDF" && safeBook.filePath.isNotEmpty()) {
        val uri = android.net.Uri.parse(safeBook.filePath)
        val combinedState = rememberPdfState(uri)
        pdfCache = combinedState.second
        if ((combinedState.first?.pageCount ?: 0) > 0) {
            actualPageCount = combinedState.first!!.pageCount
        }
    } else if (safeBook.type == "COMIC" && safeBook.filePath.isNotEmpty()) {
        val uri = android.net.Uri.parse(safeBook.filePath)
        val context = LocalView.current.context
        LaunchedEffect(uri) {
            withContext(Dispatchers.IO) {
                val count = com.example.utils.DocumentExtractor.getComicPageCount(context, uri)
                if (count > 0) actualPageCount = count
            }
        }
    } else if (safeBook.type == "EPUB" && safeBook.filePath.isNotEmpty()) {
        val uri = android.net.Uri.parse(safeBook.filePath)
        val context = LocalView.current.context
        LaunchedEffect(uri) {
            withContext(Dispatchers.IO) {
                val count = com.example.utils.DocumentExtractor.getEpubChapterCount(context, uri)
                if (count > 0) actualPageCount = count
            }
        }
    }

    val pageCount = actualPageCount 
    var isEyeProtectionEnabled by remember { mutableStateOf(false) }
    var textSize by remember { mutableStateOf(18) }
    var readerTheme by remember { mutableStateOf("LIGHT") } // "LIGHT", "DARK", "SEPIA"
    var showSettingsDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = ((safeBook.progress) * pageCount).toInt().coerceIn(0, pageCount - 1)
    )
    val pagerState = rememberPagerState(
        initialPage = ((safeBook.progress) * pageCount).toInt().coerceIn(0, pageCount - 1),
        pageCount = { pageCount }
    )
    
    val currentPage by remember(isHorizontal) {
        derivedStateOf {
            if (isHorizontal) pagerState.currentPage else listState.firstVisibleItemIndex
        }
    }

    LaunchedEffect(listState, pagerState, isHorizontal) {
        snapshotFlow { 
            if (isHorizontal) pagerState.currentPage else listState.firstVisibleItemIndex 
        }.collect { page ->
            viewModel.updateProgress(safeBook.id, page.toFloat() / (pageCount - 1).coerceAtLeast(1))
        }
    }

    LaunchedEffect(isUiVisible) {
        window?.let { win ->
            val insetsController = WindowCompat.getInsetsController(win, view)
            if (isUiVisible) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isUiVisible,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                TopAppBar(
                    title = { Text(safeBook.title, maxLines = 1, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(modifier = Modifier.padding(4.dp)) {
                                IconButton(
                                    onClick = { isEyeProtectionEnabled = !isEyeProtectionEnabled },
                                    modifier = Modifier.size(36.dp).background(
                                        color = if (isEyeProtectionEnabled) Color(0xFFFFAA00).copy(alpha = 0.2f) else Color.Transparent,
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Proteção Ocular",
                                        tint = if (isEyeProtectionEnabled) Color(0xFFFFAA00) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.toggleScrollMode(true) },
                                    modifier = Modifier.size(36.dp).background(
                                        color = if (!isHorizontal) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = "Vertical Mode",
                                        tint = if (!isHorizontal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showSettingsDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz, // using placeholder icon for settings since we don't have Settings imported
                                        contentDescription = "Configurações",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isUiVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Página ${currentPage + 1} de $pageCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { newValue ->
                                scope.launch {
                                    if (isHorizontal) {
                                        pagerState.scrollToPage(newValue.toInt())
                                    } else {
                                        listState.scrollToItem(newValue.toInt())
                                    }
                                }
                            },
                            valueRange = 0f..(pageCount - 1).toFloat(),
                            steps = pageCount - 2,
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Ajustar progresso da leitura" }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isUiVisible) padding else PaddingValues(0.dp))
                .background(Color.Black)
        ) {
            if (isHorizontal) {
                // Horizontal Reading
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = isRtl
                ) { page ->
                    Box(modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isUiVisible = !isUiVisible }) {
                        ReaderPageContent(type = safeBook.type, filePath = safeBook.filePath, page = page, pdfCache = pdfCache, textSize = textSize, theme = readerTheme)
                    }
                }
            } else {
                // Vertical Reading
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(pageCount) { page ->
                        Box(modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isUiVisible = !isUiVisible }) {
                            ReaderPageContent(type = safeBook.type, filePath = safeBook.filePath, page = page, pdfCache = pdfCache, textSize = textSize, theme = readerTheme)
                        }
                    }
                }
            }
            
            if (isEyeProtectionEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFAA00).copy(alpha = 0.2f))
                )
            }
            
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("Configurações de Leitura") },
                    text = {
                        Column {
                            Text("Tamanho da Fonte: $textSize")
                            Slider(
                                value = textSize.toFloat(),
                                onValueChange = { textSize = it.toInt() },
                                valueRange = 12f..36f
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Tema:")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Button(onClick = { readerTheme = "LIGHT" }, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)) { Text("Claro") }
                                Button(onClick = { readerTheme = "DARK" }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)) { Text("Escuro") }
                                Button(onClick = { readerTheme = "SEPIA" }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4ECD8), contentColor = Color(0xFF5B4636))) { Text("Sépia") }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("Fechar")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ReaderPageContent(type: String, filePath: String, page: Int, pdfCache: PdfPageCache? = null, textSize: Int = 18, theme: String = "LIGHT") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (type == "PDF" && pdfCache != null) {
            PdfPageImage(pdfCache = pdfCache, page = page)
        } else if (type == "COMIC" && filePath.isNotEmpty()) {
            ComicPageImage(uriString = filePath, page = page)
        } else if (type == "EPUB" && filePath.isNotEmpty()) {
            EpubChapterView(uriString = filePath, chapterIndex = page, textSize = textSize, theme = theme)
        } else {
            Text("Conteúdo não disponível para $type", color = Color.Black)
        }
    }
}

@Composable
fun ComicPageImage(uriString: String, page: Int) {
    val context = LocalView.current.context
    var bitmap by remember(page) { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(page) {
        withContext(Dispatchers.IO) {
            val uri = android.net.Uri.parse(uriString)
            bitmap = com.example.utils.DocumentExtractor.getComicPage(context, uri, page)
        }
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val state = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        val extraWidth = (scale - 1) * 1000
        val extraHeight = (scale - 1) * 1000
        offset = androidx.compose.ui.geometry.Offset(
            x = (offset.x + panChange.x * scale).coerceIn(-extraWidth/2, extraWidth/2),
            y = (offset.y + panChange.y * scale).coerceIn(-extraHeight/2, extraHeight/2)
        )
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Página de Quadrinho ${page + 1}",
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .transformable(state)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    } else {
        CircularProgressIndicator()
    }
}

@Composable
fun EpubChapterView(uriString: String, chapterIndex: Int, textSize: Int = 18, theme: String = "LIGHT") {
    val context = LocalView.current.context
    var htmlContent by remember(chapterIndex) { mutableStateOf<String?>(null) }

    LaunchedEffect(chapterIndex) {
        withContext(Dispatchers.IO) {
            val uri = android.net.Uri.parse(uriString)
            htmlContent = com.example.utils.DocumentExtractor.getEpubChapterHtml(context, uri, chapterIndex)
        }
    }

    if (htmlContent != null) {
        val (bgColor, textColor) = when(theme) {
            "DARK" -> Pair("#121212", "#E0E0E0")
            "SEPIA" -> Pair("#F4ECD8", "#5B4636")
            else -> Pair("#FFFFFF", "#333333")
        }

        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    settings.javaScriptEnabled = false
                    settings.defaultTextEncodingName = "utf-8"
                }
            },
            update = { webView ->
                val styledHtml = "<html><head><style>body { background-color: $bgColor; color: $textColor; font-size: ${textSize}px; line-height: 1.6; padding: 16px; margin: 0; } img { max-width: 100%; height: auto; }</style></head><body>$htmlContent</body></html>"
                webView.loadDataWithBaseURL(null, styledHtml, "text/html", "utf-8", null)
                webView.setBackgroundColor(android.graphics.Color.parseColor(bgColor))
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        CircularProgressIndicator()
    }
}

@Composable
fun PdfPageImage(pdfCache: PdfPageCache, page: Int) {
    val context = LocalView.current.context
    val density = LocalView.current.resources.displayMetrics.density
    
    var bitmap by remember(page) { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(page) {
        bitmap = pdfCache.getPageBitmap(page, 0, 0, density)
    }
    
    // Pinch to zoom state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
    val state = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        val extraWidth = (scale - 1) * 1000 // Approximate boundaries
        val extraHeight = (scale - 1) * 1000
        val maxX = extraWidth / 2
        val maxY = extraHeight / 2
        
        offset = androidx.compose.ui.geometry.Offset(
            x = (offset.x + panChange.x * scale).coerceIn(-maxX, maxX),
            y = (offset.y + panChange.y * scale).coerceIn(-maxY, maxY)
        )
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "PDF Page ${page + 1}",
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .transformable(state)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.FillWidth
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
