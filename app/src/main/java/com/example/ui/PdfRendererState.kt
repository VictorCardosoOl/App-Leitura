package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.LruCache

class PdfRendererState(
    private val context: Context,
    private val uri: Uri
) {
    private var fileDescriptor: ParcelFileDescriptor? = null
    var pdfRenderer: PdfRenderer? = null
        private set
        
    init {
        try {
            fileDescriptor = if (uri.scheme == "file") {
                ParcelFileDescriptor.open(java.io.File(uri.path!!), ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }
            fileDescriptor?.let {
                pdfRenderer = PdfRenderer(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0
        
    fun close() {
        pdfRenderer?.close()
        fileDescriptor?.close()
        pdfRenderer = null
        fileDescriptor = null
    }
}

class PdfPageCache(
    private val rendererState: PdfRendererState
) {
    // Cache up to 3 pages (previous, current, next)
    private val cache = LruCache<Int, Bitmap>(3)
    
    suspend fun getPageBitmap(pageIndex: Int, width: Int, height: Int, density: Float): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= rendererState.pageCount) return@withContext null
        
        cache.get(pageIndex)?.let { return@withContext it }
        
        val renderer = rendererState.pdfRenderer ?: return@withContext null
        
        // Ensure synchronization since PdfRenderer isn't thread-safe
        synchronized(rendererState) {
            try {
                val page = renderer.openPage(pageIndex)
                
                // Adjust to screen density or target width
                val targetWidth = if (width > 0) width else (page.width * density).toInt()
                val targetHeight = if (height > 0) height else (page.height * density).toInt()
                
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                // PDF backgrounds are transparent. White is better.
                bitmap.eraseColor(android.graphics.Color.WHITE)
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                cache.put(pageIndex, bitmap)
                return@withContext bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }
}

@Composable
fun rememberPdfState(uri: Uri): Pair<PdfRendererState?, PdfPageCache?> {
    val context = LocalContext.current
    val combinedState = remember(uri) {
        val state = PdfRendererState(context, uri)
        val cache = PdfPageCache(state)
        Pair(state, cache)
    }

    DisposableEffect(combinedState) {
        onDispose {
            combinedState.first.close()
        }
    }

    return combinedState
}
