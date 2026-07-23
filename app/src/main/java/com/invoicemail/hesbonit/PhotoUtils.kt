package com.invoicemail.hesbonit

import android.content.Context
import java.io.File

object PhotoUtils {
    fun createImageFile(context: Context): File {
        val imagesDir = File(context.cacheDir, "images").apply { if (!exists()) mkdirs() }
        return File(imagesDir, "photo_${System.currentTimeMillis()}.jpg")
    }
}