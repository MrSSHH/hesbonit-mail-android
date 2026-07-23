package com.invoicemail.hesbonit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import androidx.annotation.VisibleForTesting
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object PdfUtils {

    private const val A4_WIDTH_POINTS = 595
    private const val A4_HEIGHT_POINTS = 842
    private const val MARGIN_POINTS = 24

    fun createPdfFromImage(context: Context, imageFile: File): File {
        var bitmap = decodeSampledBitmap(imageFile, reqWidth = 2000, reqHeight = 2000)
        bitmap = rotateBitmapIfNeeded(imageFile, bitmap)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_POINTS, A4_HEIGHT_POINTS, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val maxWidth = A4_WIDTH_POINTS - (MARGIN_POINTS * 2)
        val maxHeight = A4_HEIGHT_POINTS - (MARGIN_POINTS * 2)

        val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val boxRatio = maxWidth.toFloat() / maxHeight.toFloat()

        val drawWidth: Int
        val drawHeight: Int
        if (bitmapRatio > boxRatio) {
            drawWidth = maxWidth
            drawHeight = (maxWidth / bitmapRatio).toInt()
        } else {
            drawHeight = maxHeight
            drawWidth = (maxHeight * bitmapRatio).toInt()
        }

        val left = (A4_WIDTH_POINTS - drawWidth) / 2
        val top = (A4_HEIGHT_POINTS - drawHeight) / 2
        val destRect = Rect(left, top, left + drawWidth, top + drawHeight)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, destRect, paint)

        pdfDocument.finishPage(page)

        val pdfDir = File(context.cacheDir, "pdfs").apply { if (!exists()) mkdirs() }
        val pdfFile = File(pdfDir, "invoice_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use { out -> pdfDocument.writeTo(out) }
        pdfDocument.close()
        bitmap.recycle()

        return pdfFile
    }

    fun decodePreviewBitmap(imageFile: File, reqSize: Int = 800): Bitmap {
        val bitmap = decodeSampledBitmap(imageFile, reqSize, reqSize)
        return rotateBitmapIfNeeded(imageFile, bitmap)
    }

    private fun decodeSampledBitmap(imageFile: File, reqWidth: Int, reqHeight: Int): Bitmap {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFile.absolutePath, boundsOptions)

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)
            inJustDecodeBounds = false
        }

        return BitmapFactory.decodeFile(imageFile.absolutePath, options)
            ?: throw IllegalStateException("׳׳ ׳ ׳™׳×׳ ׳׳§׳¨׳•׳ ׳׳× ׳”׳×׳׳•׳ ׳” ׳©׳¦׳•׳׳׳”")
    }

    @VisibleForTesting
    internal fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    @VisibleForTesting
    internal fun rotateBitmapIfNeeded(imageFile: File, bitmap: Bitmap): Bitmap {
        val exif = ExifInterface(imageFile.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotationDegrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}