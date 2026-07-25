package com.invoicemail.hesbonit

import android.graphics.Bitmap
import java.io.File

/**
 * A single captured photo page: the JPEG file on disk plus its decoded preview bitmap.
 */
data class CapturedPage(
    val file: File,
    val previewBitmap: Bitmap
)
