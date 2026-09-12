package com.flashypdfkit.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.flashypdfkit.model.PageOrganizeItem
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object PdfEngine {

    fun <T> withPdfRenderer(context: Context, uri: Uri, block: (PdfRenderer) -> T): T? {
        var pfd: ParcelFileDescriptor? = null
        var tempFile: File? = null
        return try {
            try {
                pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    val result = block(renderer)
                    renderer.close()
                    return result
                }
            } catch (e: Exception) {
                // Direct file descriptor was non-seekable or failed. Fall back to local temp copy.
                pfd?.close()
                pfd = null
            }

            // Copy to temp file to guarantee a seekable local descriptor
            tempFile = File.createTempFile("pdf_seekable_", ".pdf", context.cacheDir)
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output) > 0
                }
            } ?: false

            if (!copied || !tempFile.exists() || tempFile.length() == 0L) {
                return null
            }

            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val result = block(renderer)
            renderer.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { pfd?.close() } catch (_: Exception) {}
            try { tempFile?.delete() } catch (_: Exception) {}
        }
    }

    fun getPdfPageCount(context: Context, uri: Uri): Int {
        return withPdfRenderer(context, uri) { it.pageCount } ?: 0
    }

    fun renderPdfPage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 1080
    ): Bitmap? {
        return withPdfRenderer(context, uri) { renderer ->
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withPdfRenderer null
            val page = renderer.openPage(pageIndex)
            val width = page.width
            val height = page.height
            val aspectRatio = height.toFloat() / width.toFloat()
            val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(
                max(1, targetWidth),
                targetHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        }
    }

    fun imagesToPdf(context: Context, imageUris: List<Uri>, outputFile: File): Boolean {
        if (imageUris.isEmpty()) return false
        val pdfDocument = PdfDocument()
        try {
            for ((index, uri) in imageUris.withIndex()) {
                val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: continue
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (originalBitmap == null) continue

                val pageWidth = 595 // A4 standard width in points (72 dpi)
                val pageHeight = 842 // A4 standard height in points
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Draw white background
                canvas.drawColor(Color.WHITE)

                // Scale bitmap keeping aspect ratio to fit inside A4 page with margins
                val margin = 36f // 0.5 inch margin
                val availW = pageWidth - margin * 2
                val availH = pageHeight - margin * 2

                val scale = min(availW / originalBitmap.width, availH / originalBitmap.height)
                val drawW = originalBitmap.width * scale
                val drawH = originalBitmap.height * scale

                val left = margin + (availW - drawW) / 2f
                val top = margin + (availH - drawH) / 2f

                val destRect = RectF(left, top, left + drawW, top + drawH)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(originalBitmap, null, destRect, paint)

                pdfDocument.finishPage(page)
                originalBitmap.recycle()
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            try { pdfDocument.close() } catch (_: Exception) {}
            return false
        }
    }

    fun pdfToImages(
        context: Context,
        pdfUri: Uri,
        format: String,
        scaleFactor: Float,
        outputDir: File
    ): List<File> {
        outputDir.mkdirs()
        val ext = if (format.lowercase() == "png") "png" else "jpg"
        return withPdfRenderer(context, pdfUri) { renderer ->
            val resultFiles = mutableListOf<File>()
            val pageCount = renderer.pageCount
            val baseWidth = (1080 * scaleFactor).toInt().coerceIn(400, 2400)

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val aspectRatio = page.height.toFloat() / page.width.toFloat()
                val width = baseWidth
                val height = (width * aspectRatio).toInt().coerceIn(400, 3600)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val file = File(outputDir, "page_${i + 1}.$ext")
                FileOutputStream(file).use { out ->
                    if (ext == "png") {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } else {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                }
                bitmap.recycle()
                resultFiles.add(file)
            }
            resultFiles
        } ?: emptyList()
    }

    fun mergePdfs(context: Context, pdfUris: List<Uri>, outputFile: File): Boolean {
        if (pdfUris.isEmpty()) return false
        val pdfDocument = PdfDocument()
        var pageCounter = 1

        try {
            for (uri in pdfUris) {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: continue
                pfd.use { descriptor ->
                    val renderer = PdfRenderer(descriptor)
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val w = page.width
                        val h = page.height

                        // Standardize render resolution for output PDF
                        val renderW = 1240
                        val renderH = (renderW * (h.toFloat() / w.toFloat())).toInt()

                        val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        val pageInfo = PdfDocument.PageInfo.Builder(595, (595 * (h.toFloat() / w.toFloat())).toInt(), pageCounter++).create()
                        val newPage = pdfDocument.startPage(pageInfo)
                        val canvas = newPage.canvas

                        val rect = RectF(0f, 0f, pageInfo.pageWidth.toFloat(), pageInfo.pageHeight.toFloat())
                        canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

                        pdfDocument.finishPage(newPage)
                        bitmap.recycle()
                    }
                    renderer.close()
                }
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            try { pdfDocument.close() } catch (_: Exception) {}
            return false
        }
    }

    fun splitPdf(context: Context, pdfUri: Uri, selectedPages: List<Int>, outputFile: File): Boolean {
        if (selectedPages.isEmpty()) return false
        val pdfDocument = PdfDocument()
        var pageCounter = 1

        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return false
            pfd.use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                val totalPages = renderer.pageCount

                for (pNum in selectedPages) {
                    val pageIdx = pNum - 1
                    if (pageIdx in 0 until totalPages) {
                        val page = renderer.openPage(pageIdx)
                        val w = page.width
                        val h = page.height

                        val renderW = 1240
                        val renderH = (renderW * (h.toFloat() / w.toFloat())).toInt()

                        val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        val pageInfo = PdfDocument.PageInfo.Builder(595, (595 * (h.toFloat() / w.toFloat())).toInt(), pageCounter++).create()
                        val newPage = pdfDocument.startPage(pageInfo)
                        val canvas = newPage.canvas
                        val rect = RectF(0f, 0f, pageInfo.pageWidth.toFloat(), pageInfo.pageHeight.toFloat())
                        canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

                        pdfDocument.finishPage(newPage)
                        bitmap.recycle()
                    }
                }
                renderer.close()
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            try { pdfDocument.close() } catch (_: Exception) {}
            return false
        }
    }

    fun organizePdf(context: Context, pdfUri: Uri, pageItems: List<PageOrganizeItem>, outputFile: File): Boolean {
        if (pageItems.isEmpty()) return false
        val pdfDocument = PdfDocument()
        var newPageNum = 1

        val rendered = withPdfRenderer(context, pdfUri) { renderer ->
            val totalPages = renderer.pageCount

            for (item in pageItems) {
                val pageIdx = item.originalIndex
                if (pageIdx in 0 until totalPages) {
                    val page = renderer.openPage(pageIdx)
                    val w = page.width
                    val h = page.height

                    val renderW = 1240
                    val renderH = (renderW * (h.toFloat() / w.toFloat())).toInt()

                    val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val rotation = (item.rotationDegrees % 360 + 360) % 360
                    val rotatedBitmap = if (rotation != 0) {
                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                        val rb = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        bitmap.recycle()
                        rb
                    } else {
                        bitmap
                    }

                    val docW = if (rotation == 90 || rotation == 270) (595 * (h.toFloat() / w.toFloat())).toInt() else 595
                    val docH = if (rotation == 90 || rotation == 270) 595 else (595 * (h.toFloat() / w.toFloat())).toInt()

                    val pageInfo = PdfDocument.PageInfo.Builder(docW, docH, newPageNum++).create()
                    val newPage = pdfDocument.startPage(pageInfo)
                    val canvas = newPage.canvas
                    val rect = RectF(0f, 0f, docW.toFloat(), docH.toFloat())
                    canvas.drawBitmap(rotatedBitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

                    pdfDocument.finishPage(newPage)
                    rotatedBitmap.recycle()
                }
            }
            true
        } ?: false

        if (!rendered) {
            try { pdfDocument.close() } catch (_: Exception) {}
            return false
        }

        return try {
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            try { pdfDocument.close() } catch (_: Exception) {}
            false
        }
    }

    fun signPdf(
        context: Context,
        pdfUri: Uri,
        signatureBitmap: Bitmap,
        targetPageIndex: Int,
        normX: Float,
        normY: Float,
        normW: Float,
        normH: Float,
        rotationDegrees: Float = 0f,
        outputFile: File
    ): Boolean {
        val pdfDocument = PdfDocument()
        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return false
            pfd.use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                val totalPages = renderer.pageCount

                for (i in 0 until totalPages) {
                    val page = renderer.openPage(i)
                    val w = page.width
                    val h = page.height

                    val renderW = 1240
                    val renderH = (renderW * (h.toFloat() / w.toFloat())).toInt()

                    val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val pageW = 595
                    val pageH = (595 * (h.toFloat() / w.toFloat())).toInt()
                    val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, i + 1).create()
                    val newPage = pdfDocument.startPage(pageInfo)
                    val canvas = newPage.canvas
                    val rect = RectF(0f, 0f, pageW.toFloat(), pageH.toFloat())

                    canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

                    if (i == targetPageIndex) {
                        val signX = normX * pageW
                        val signY = normY * pageH
                        val signWidth = normW * pageW
                        val signHeight = normH * pageH

                        canvas.save()
                        val centerX = signX + signWidth / 2f
                        val centerY = signY + signHeight / 2f
                        canvas.rotate(rotationDegrees, centerX, centerY)
                        val signRect = RectF(signX, signY, signX + signWidth, signY + signHeight)
                        canvas.drawBitmap(signatureBitmap, null, signRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                        canvas.restore()
                    }

                    pdfDocument.finishPage(newPage)
                    bitmap.recycle()
                }
                renderer.close()
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            try { pdfDocument.close() } catch (_: Exception) {}
            return false
        }
    }

    fun compressPdf(context: Context, pdfUri: Uri, level: Int, outputFile: File): Boolean {
        // level 1: Light (1080w, 85% JPEG quality)
        // level 2: Balanced (800w, 70% JPEG quality)
        // level 3: Strong (600w, 50% JPEG quality)
        val targetW = when(level) {
            1 -> 1080
            3 -> 600
            else -> 800
        }
        val jpegQuality = when(level) {
            1 -> 85
            3 -> 50
            else -> 70
        }

        val pdfDocument = PdfDocument()
        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return false
            pfd.use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                val totalPages = renderer.pageCount

                for (i in 0 until totalPages) {
                    val page = renderer.openPage(i)
                    val w = page.width
                    val h = page.height
                    val aspectRatio = h.toFloat() / w.toFloat()

                    val renderH = (targetW * aspectRatio).toInt()
                    val bitmap = Bitmap.createBitmap(targetW, renderH, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    // Compress to compressed JPEG byte stream
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
                    val compressedBitmap = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size())
                    bitmap.recycle()

                    val pageW = 595
                    val pageH = (595 * aspectRatio).toInt()
                    val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, i + 1).create()
                    val newPage = pdfDocument.startPage(pageInfo)
                    val canvas = newPage.canvas

                    canvas.drawBitmap(compressedBitmap, null, RectF(0f, 0f, pageW.toFloat(), pageH.toFloat()), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    pdfDocument.finishPage(newPage)
                    compressedBitmap.recycle()
                }
                renderer.close()
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            try { pdfDocument.close() } catch (_: Exception) {}
            return false
        }
    }

    fun protectPdf(context: Context, pdfUri: Uri, password: String, outputFile: File): Boolean {
        return try {
            PDFBoxResourceLoader.init(context.applicationContext)
            val inputStream = context.contentResolver.openInputStream(pdfUri) ?: return false
            val document = inputStream.use { stream ->
                PDDocument.load(stream)
            }
            val ap = AccessPermission()
            val spp = StandardProtectionPolicy(password, password, ap).apply {
                encryptionKeyLength = 128
                permissions = ap
            }
            document.protect(spp)
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { fos ->
                document.save(fos)
            }
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: If original document could not be processed directly by PDFBox,
            // render pages and protect generated document
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                val tempUnprotected = File(context.cacheDir, "temp_render_${System.currentTimeMillis()}.pdf")
                val pdfDocument = PdfDocument()
                val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return false
                pfd.use { descriptor ->
                    val renderer = PdfRenderer(descriptor)
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val w = page.width
                        val h = page.height
                        val renderW = 1200
                        val renderH = (renderW * (h.toFloat() / w.toFloat())).toInt()
                        val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        val pageW = 595
                        val pageH = (595 * (h.toFloat() / w.toFloat())).toInt()
                        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, i + 1).create()
                        val newPage = pdfDocument.startPage(pageInfo)
                        newPage.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, pageW.toFloat(), pageH.toFloat()), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                        pdfDocument.finishPage(newPage)
                        bitmap.recycle()
                    }
                    renderer.close()
                }
                FileOutputStream(tempUnprotected).use { fos -> pdfDocument.writeTo(fos) }
                pdfDocument.close()

                val loadedDoc = PDDocument.load(tempUnprotected)
                val ap = AccessPermission()
                val spp = StandardProtectionPolicy(password, password, ap).apply {
                    encryptionKeyLength = 128
                    permissions = ap
                }
                loadedDoc.protect(spp)
                outputFile.parentFile?.mkdirs()
                FileOutputStream(outputFile).use { fos -> loadedDoc.save(fos) }
                loadedDoc.close()
                tempUnprotected.delete()
                true
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
                false
            }
        }
    }

    fun isPdfEncrypted(context: Context, pdfUri: Uri): Boolean {
        return try {
            PDFBoxResourceLoader.init(context.applicationContext)
            val inputStream = context.contentResolver.openInputStream(pdfUri) ?: return false
            inputStream.use { stream ->
                val doc = PDDocument.load(stream)
                val enc = doc.isEncrypted
                doc.close()
                enc
            }
        } catch (e: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
            true
        } catch (e: Exception) {
            false
        }
    }

    fun removePdfProtection(context: Context, pdfUri: Uri, password: String, outputFile: File): Boolean {
        return try {
            PDFBoxResourceLoader.init(context.applicationContext)
            val inputStream = context.contentResolver.openInputStream(pdfUri) ?: return false
            val document = inputStream.use { stream ->
                PDDocument.load(stream, password)
            }
            document.isAllSecurityToBeRemoved = true
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { fos ->
                document.save(fos)
            }
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
