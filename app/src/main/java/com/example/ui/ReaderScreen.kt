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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    bookId: Int,
    viewModel: ReaderViewModel,
    onBack: () -> Unit
) {
    val bookFlow = remember(bookId) { viewModel.getBook(bookId) }
    val book by bookFlow.collectAsState()
    var isHorizontal by remember { mutableStateOf(false) }
    var isUiVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    // Fake pages
    val pageCount = 10 

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = ((book?.progress ?: 0f) * pageCount).toInt().coerceIn(0, pageCount - 1)
    )
    val pagerState = rememberPagerState(
        initialPage = ((book?.progress ?: 0f) * pageCount).toInt().coerceIn(0, pageCount - 1),
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
            viewModel.updateProgress(bookId, page.toFloat() / (pageCount - 1).coerceAtLeast(1))
        }
    }

    if (book == null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    
    val safeBook = book!!

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
                                    onClick = { isHorizontal = false },
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
                                    onClick = { isHorizontal = true },
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
                            modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Box(modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isUiVisible = !isUiVisible }) {
                        ReaderPageContent(type = safeBook.type, page = page)
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
                            ReaderPageContent(type = safeBook.type, page = page)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderPageContent(type: String, page: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (type == "COMIC" || type == "PDF") {
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
