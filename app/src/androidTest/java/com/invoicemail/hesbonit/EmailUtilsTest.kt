package com.invoicemail.hesbonit

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EmailUtilsTest {

    @Test
    fun sendPdfViaGmail_buildsValidSendIntent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dummyFile = File(context.cacheDir, "test.pdf").apply { createNewFile() }

        val intent = EmailUtils.createEmailIntent(context, dummyFile, "test@example.com")

        assertNotNull(intent)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/pdf", intent.type)
        assertEquals("com.google.android.gm", intent.`package`)
    }
}
