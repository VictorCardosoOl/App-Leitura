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
    }

    val pageCount = actualPageCount 
    var isEyeProtectionEnabled by remember { mutableStateOf(false) }

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
                                    onClick = { viewModel.toggleScrollMode(false) },
                                    modifier = Modifier.size(36.dp).background(
                                        color = if (isHorizontal) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Horizontal Mode",
                                        tint = if (isHorizontal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        ReaderPageContent(type = safeBook.type, page = page, pdfCache = pdfCache)
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
                            ReaderPageContent(type = safeBook.type, page = page, pdfCache = pdfCache)
                        }
                    }
                }
            }
            
            if (isEyeProtectionEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFAA00).copy(alpha = 0.2f))
                        // Clickable intercept to pass down gestures while overlaying color isn't perfectly transparent to touches if we don't pass them, 
                        // actually just use pointerInput to let touches pass through or don't set clickable. Background alone doesn't consume clicks.
                )
            }
        }
    }
}

@Composable
fun ReaderPageContent(type: String, page: Int, pdfCache: PdfPageCache? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (type == "PDF" && pdfCache != null) {
            PdfPageImage(pdfCache = pdfCache, page = page)
        } else if (type == "COMIC" || type == "PDF") {
            // Mock comic/pdf page image
            val url = "https://images.unsplash.com/photo-1578301978018-3005759f48f7?auto=format&fit=crop&w=600&q=80&text=Page+${page + 1}"
            AsyncImage(
                model = url,
                contentDescription = "Page ${page + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Mock EPUB Content
            Text(
                text = "EPUB Content Page ${page + 1}\n\nLorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                color = Color.Black,
                modifier = Modifier.padding(32.dp),
                textAlign = TextAlign.Justify
            )
        }
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
