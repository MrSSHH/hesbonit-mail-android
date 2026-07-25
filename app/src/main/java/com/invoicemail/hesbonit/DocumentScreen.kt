package com.invoicemail.hesbonit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun DocumentScreen(
    contact: Contact,
    pages: List<CapturedPage>,
    isProcessing: Boolean,
    onBack: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePage: (CapturedPage) -> Unit,
    onSend: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(contact.name, style = MaterialTheme.typography.titleMedium)
                Text(contact.email, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, MaterialTheme.colorScheme.outline),
            contentAlignment = Alignment.Center
        ) {
            if (pages.isNotEmpty()) {
                Image(
                    bitmap = pages.last().previewBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.photo_preview_desc),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(stringResource(R.string.no_pdf_yet))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (pages.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(pages, key = { it.file.absolutePath }) { page ->
                    val index = pages.indexOf(page)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Image(
                            bitmap = page.previewBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.page_number_desc, index + 1),
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable(enabled = !isProcessing) { onRemovePage(page) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.remove_page_desc),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onTakePhoto,
            enabled = !isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (pages.isNotEmpty()) stringResource(R.string.add_page)
                else stringResource(R.string.take_photo)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSend,
            enabled = pages.isNotEmpty() && !isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.preparing_pdf))
            } else {
                Icon(Icons.Default.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.send_email))
            }
        }
    }
}
