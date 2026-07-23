package com.invoicemail.hesbonit

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmailUtilsTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `sendPdfViaGmail starts an intent with fixed subject, body and recipient`() {
        val pdfFile = File.createTempFile("invoice", ".pdf", application.cacheDir).apply {
            writeText("dummy-pdf-content")
        }

        EmailUtils.sendPdfViaGmail(application, pdfFile, "recipient@example.com")

        val started = shadowOf(application).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_SEND, started.action)
        assertEquals("application/pdf", started.type)
        assertEquals(
            "recipient@example.com",
            started.getStringArrayExtra(Intent.EXTRA_EMAIL)?.firstOrNull()
        )
        assertEquals(
            application.getString(R.string.email_subject),
            started.getStringExtra(Intent.EXTRA_SUBJECT)
        )
        assertEquals(
            application.getString(R.string.email_body),
            started.getStringExtra(Intent.EXTRA_TEXT)
        )
        assertNotNull(started.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM))
    }
}