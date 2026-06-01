package com.example.data.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PdfReaderUseCase(private val context: Context) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null

    // LRU Cache for rendered pages
    // Key: Page Index, Value: Rendered Bitmap
    private val pageCache: LruCache<Int, Bitmap> = object : LruCache<Int, Bitmap>(3) {
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted) {
                oldValue.recycle()
            }
        }
    }

    suspend fun openPdf(uri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            // Check if URI is a file scheme or content scheme
            fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            
            if (fileDescriptor != null) {
                pdfRenderer = PdfRenderer(fileDescriptor!!)
                return@withContext pdfRenderer?.pageCount ?: 0
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext 0
    }

    suspend fun getPage(pageIndex: Int, density: Float = 2.0f): Bitmap? = withContext(Dispatchers.IO) {
        if (pdfRenderer == null) return@withContext null

        val count = pdfRenderer?.pageCount ?: 0
        if (pageIndex < 0 || pageIndex >= count) return@withContext null

        // Check Cache
        val cachedBitmap = pageCache.get(pageIndex)
        if (cachedBitmap != null && !cachedBitmap.isRecycled) {
            return@withContext cachedBitmap
        }

        try {
            // Render new page
            currentPage?.close()
            val page = pdfRenderer!!.openPage(pageIndex)
            currentPage = page

            // Create bitmap based on page dimensions and screen density
            val bitmap = Bitmap.createBitmap(
                (page.width * density).toInt(),
                (page.height * density).toInt(),
                Bitmap.Config.ARGB_8888
            )

            // Render onto bitmap
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            // Put in cache
            pageCache.put(pageIndex, bitmap)

            return@withContext bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext null
    }

    fun close() {
        currentPage?.close()
        currentPage = null
        pdfRenderer?.close()
        pdfRenderer = null
        fileDescriptor?.close()
        fileDescriptor = null
        pageCache.evictAll()
    }
}
