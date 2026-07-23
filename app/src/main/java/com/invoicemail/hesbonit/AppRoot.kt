package com.invoicemail.hesbonit

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class Screen { ContactList, Document }

private const val FILE_PROVIDER_AUTHORITY = "com.invoicemail.hesbonit.fileprovider"

@Composable
fun AppRoot(contactViewModel: ContactViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var screen by remember { mutableStateOf(Screen.ContactList) }
    val selectedContact by contactViewModel.selectedContact.collectAsState()

    var photoFile by remember { mutableStateOf<File?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val fileToProcess = pendingPhotoFile
        if (success && fileToProcess != null) {
            photoFile = fileToProcess
            previewBitmap = try {
                PdfUtils.decodePreviewBitmap(fileToProcess)
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                null
            }
        } else {
            Toast.makeText(context, context.getString(R.string.photo_capture_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val newFile = PhotoUtils.createImageFile(context)
            val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, newFile)
            pendingPhotoFile = newFile
            pendingPhotoUri = uri
            takePictureLauncher.launch(uri)
        } else {
            Toast.makeText(context, context.getString(R.string.permission_camera_denied), Toast.LENGTH_LONG).show()
        }
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val newFile = PhotoUtils.createImageFile(context)
            val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, newFile)
            pendingPhotoFile = newFile
            pendingPhotoUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    when (screen) {
        Screen.ContactList -> {
            ContactListScreen(
                viewModel = contactViewModel,
                onContactChosen = {
                    photoFile = null
                    previewBitmap = null
                    screen = Screen.Document
                }
            )
        }

        Screen.Document -> {
            val contact = selectedContact
            if (contact == null) {
                screen = Screen.ContactList
            } else {
                DocumentScreen(
                    contact = contact,
                    previewBitmap = previewBitmap,
                    isProcessing = isProcessing,
                    onBack = { screen = Screen.ContactList },
                    onTakePhoto = { launchCamera() },
                    onSend = {
                        val currentPhoto = photoFile
                        if (currentPhoto == null) {
                            Toast.makeText(context, context.getString(R.string.no_pdf_yet), Toast.LENGTH_SHORT).show()
                            return@DocumentScreen
                        }
                        isProcessing = true
                        coroutineScope.launch {
                            try {
                                val pdfFile = withContext(Dispatchers.IO) {
                                    PdfUtils.createPdfFromImage(context, currentPhoto)
                                }
                                EmailUtils.sendPdfViaGmail(context, pdfFile, contact.email)
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: context.getString(R.string.email_send_failed), Toast.LENGTH_LONG).show()
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                )
            }
        }
    }
}
