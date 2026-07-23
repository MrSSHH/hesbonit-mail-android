package com.invoicemail.hesbonit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfUtilsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun createTestJpeg(width: Int = 400, height: Int = 300): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val file = File.createTempFile("test_photo", ".jpg", context.cacheDir)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        bitmap.recycle()
        return file
    }

    @Test
    fun `createPdfFromImage produces a non-empty pdf file`() {
        val imageFile = createTestJpeg()

        val pdfFile = PdfUtils.createPdfFromImage(context, imageFile)

        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)
        assertTrue(pdfFile.name.endsWith(".pdf"))
    }

    @Test
    fun `createPdfFromImage produces a single A4-sized page`() {
        val imageFile = createTestJpeg()

        val pdfFile = PdfUtils.createPdfFromImage(context, imageFile)

        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                assertEquals(1, renderer.pageCount)
                renderer.openPage(0).use { page ->
                    assertEquals(595, page.width)
                    assertEquals(842, page.height)
                }
            }
        }
    }

    @Test
    fun `decodePreviewBitmap returns a valid downsampled bitmap`() {
        val imageFile = createTestJpeg(width = 3000, height = 2000)

        val preview = PdfUtils.decodePreviewBitmap(imageFile, reqSize = 800)

        assertTrue(preview.width > 0 && preview.height > 0)
        assertTrue(preview.width <= 3000 && preview.height <= 2000)
    }

    @Test
    fun `calculateInSampleSize returns power of two when image exceeds target`() {
        val options = BitmapFactory.Options().apply {
            outWidth = 4000
            outHeight = 3000
        }

        val sampleSize = PdfUtils.calculateInSampleSize(options, 1000, 1000)

        assertTrue(sampleSize >= 2)
        assertEquals(0, sampleSize and (sampleSize - 1))
    }

    @Test
    fun `calculateInSampleSize returns 1 when image is smaller than target`() {
        val options = BitmapFactory.Options().apply {
            outWidth = 200
            outHeight = 150
        }

        val sampleSize = PdfUtils.calculateInSampleSize(options, 2000, 2000)

        assertEquals(1, sampleSize)
    }

    @Test
    fun `rotateBitmapIfNeeded swaps dimensions for a 90-degree exif orientation`() {
        val imageFile = createTestJpeg(width = 400, height = 300)
        ExifInterface(imageFile.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val original = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)

        val rotated = PdfUtils.rotateBitmapIfNeeded(imageFile, original)

        assertEquals(300, rotated.width)
        assertEquals(400, rotated.height)
    }

    @Test
    fun `rotateBitmapIfNeeded leaves bitmap unchanged for normal exif orientation`() {
        val imageFile = createTestJpeg(width = 400, height = 300)
        val original = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)

        val result = PdfUtils.rotateBitmapIfNeeded(imageFile, original)

        assertEquals(400, result.width)
        assertEquals(300, result.height)
    }
}