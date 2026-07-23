package com.invoicemail.hesbonit

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object EmailUtils {

    private const val FILE_PROVIDER_AUTHORITY = "com.invoicemail.hesbonit.fileprovider"

    fun sendPdfViaGmail(context: Context, pdfFile: File, recipientEmail: String) {
        val pdfUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, pdfFile)

        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject))
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.email_body))
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val gmailIntent = Intent(baseIntent).setPackage("com.google.android.gm")

        try {
            if (gmailIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(gmailIntent)
            } else {
                context.startActivity(Intent.createChooser(baseIntent, null))
            }
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent.createChooser(baseIntent, null))
        }
    }
}