package com.example.androidpicpdf.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.graphics.ColorUtils
import com.example.androidpicpdf.model.FrameStyle
import com.example.androidpicpdf.model.Page
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object PdfExporter {
    private const val A4_WIDTH_PX = 2480
    private const val A4_HEIGHT_PX = 3508
    private const val HEADER_TEXT_SIZE = 48f
    private const val CAPTION_TEXT_SIZE = 32f

    fun exportProject(
        context: Context,
        pages: List<Page>,
        frameEnabled: Boolean,
        frameStyle: FrameStyle,
        frameColorHex: String
    ): File {
        val document = PdfDocument()
        val contentResolver = context.contentResolver
        pages.forEachIndexed { index, page ->
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PX, A4_HEIGHT_PX, index + 1).create()
            val pdfPage = document.startPage(pageInfo)
            drawPage(pdfPage.canvas, contentResolver, page, frameEnabled, frameStyle, frameColorHex)
            document.finishPage(pdfPage)
        }

        val fileName = createFileName()
        val outputFile = File(context.cacheDir, fileName)
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        return outputFile
    }

    private fun drawPage(
        canvas: Canvas,
        contentResolver: ContentResolver,
        page: Page,
        frameEnabled: Boolean,
        frameStyle: FrameStyle,
        frameColorHex: String
    ) {
        val margin = 140f
        val padding = 24f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = HEADER_TEXT_SIZE
        }
        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = CAPTION_TEXT_SIZE
        }
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = frameStyle.widthDp * 3f
            color = ColorUtils.setAlphaComponent(android.graphics.Color.parseColor(frameColorHex), 200)
        }

        val headerHeight = if (page.title.isNotBlank()) HEADER_TEXT_SIZE + padding else 0f
        val availableHeight = A4_HEIGHT_PX - margin * 2 - headerHeight
        val availableWidth = A4_WIDTH_PX - margin * 2

        if (page.title.isNotBlank()) {
            canvas.drawText(page.title, margin, margin + HEADER_TEXT_SIZE, headerPaint)
        }

        val rows = when (page.photos.size) {
            6 -> 3
            5 -> 2
            else -> 2
        }
        val columns = if (page.photos.size >= 5) 3 else 2
        val cellWidth = (availableWidth - padding * (columns - 1)) / columns
        val cellHeight = (availableHeight - padding * (rows - 1)) / rows
        val startY = margin + headerHeight

        page.photos.forEachIndexed { index, photo ->
            val row = index / columns
            val column = index % columns
            if (row >= rows) return@forEachIndexed

            val left = margin + column * (cellWidth + padding)
            val top = startY + row * (cellHeight + padding)
            val captionHeight = if (photo.caption.isNotBlank()) CAPTION_TEXT_SIZE + padding else 0f
            val imageRect = RectF(left, top, left + cellWidth, top + cellHeight - captionHeight)

            drawImage(contentResolver, Uri.parse(photo.uri), canvas, imageRect)
            if (frameEnabled) {
                canvas.drawRect(imageRect, framePaint)
            }
            if (photo.caption.isNotBlank()) {
                canvas.drawText(photo.caption, left, imageRect.bottom + CAPTION_TEXT_SIZE, captionPaint)
            }
        }
    }

    private fun drawImage(contentResolver: ContentResolver, uri: Uri, canvas: Canvas, rect: RectF) {
        val bitmap = decodeBitmap(contentResolver, uri, rect.width().toInt(), rect.height().toInt()) ?: return
        val srcRect = centerCrop(bitmap, rect.width().toInt(), rect.height().toInt())
        canvas.drawBitmap(bitmap, srcRect, rect, null)
        bitmap.recycle()
    }

    private fun decodeBitmap(
        contentResolver: ContentResolver,
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        val sampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, targetWidth: Int, targetHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > targetHeight || width > targetWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= targetHeight && (halfWidth / inSampleSize) >= targetWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun centerCrop(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Rect {
        val srcWidth = bitmap.width.toFloat()
        val srcHeight = bitmap.height.toFloat()
        val scale = maxOf(targetWidth / srcWidth, targetHeight / srcHeight)
        val scaledWidth = srcWidth * scale
        val scaledHeight = srcHeight * scale
        val left = (scaledWidth - targetWidth) / 2f / scale
        val top = (scaledHeight - targetHeight) / 2f / scale
        val right = left + targetWidth / scale
        val bottom = top + targetHeight / scale
        return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }

    private fun createFileName(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault())
        return "Fotoseiten_${formatter.format(System.currentTimeMillis())}.pdf"
    }
}
