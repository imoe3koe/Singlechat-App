package com.singlechat.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.singlechat.data.MessageModel

@Composable
fun ChatScreen(
    messages: List<MessageModel>,
    currentUserEmail: String?,
    currentGroupToken: String?,
    onSendMessage: (content: String, type: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var showCallDialog by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // File picker launcher for images/documents
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val mime = context.contentResolver.getType(it) ?: ""
            val type = if (mime.contains("pdf", ignoreCase = true)) "pdf" else "image"
            onSendMessage(it.toString(), type)
            Toast.makeText(context, "Berkas berhasil dikirim!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // Chat Sub-Header with Voice Call Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RUANG OBROLAN",
                    style = MaterialTheme.typography.titleSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Voice Call Button [☎]
            IconButton(
                onClick = { showCallDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("btn_voice_call")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Panggilan Suara Kelompok",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada pesan. Mulai obrolan sekarang!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(messages, key = { it.id ?: (it.content + it.created_at) }) { msg ->
                val isMe = msg.sender_email.equals(currentUserEmail, ignoreCase = true)
                MessageItem(
                    message = msg,
                    isMe = isMe,
                    onImageClick = { previewImageUrl = it },
                    onOpenFile = { content ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Membuka: $content", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Bottom Message Input & Attach Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attach Button (+)
            IconButton(
                onClick = { showAttachmentDialog = true },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("btn_attach")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Lampiran",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text Input
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Tulis pesan...", fontSize = 13.sp) },
                singleLine = true,
                shape = RectangleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(horizontal = 4.dp)
                    .testTag("message_input")
            )

            // Send Button
            Button(
                onClick = {
                    val trimmed = inputText.trim()
                    if (trimmed.isNotEmpty()) {
                        onSendMessage(trimmed, "text")
                        inputText = ""
                    }
                },
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .height(44.dp)
                    .padding(start = 4.dp)
                    .testTag("btn_send")
            ) {
                Text(
                    text = "KIRIM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }

    // Attachment Options Dialog
    if (showAttachmentDialog) {
        AlertDialog(
            onDismissRequest = { showAttachmentDialog = false },
            title = {
                Text(
                    text = "Kirim Lampiran",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pilih jenis lampiran untuk dikirim ke kelompok:", style = MaterialTheme.typography.bodySmall)

                    OutlinedButton(
                        onClick = {
                            showAttachmentDialog = false
                            filePicker.launch("*/*")
                        },
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih Berkas dari Memori HP")
                    }

                    OutlinedButton(
                        onClick = {
                            showAttachmentDialog = false
                            onSendMessage("https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800", "image")
                        },
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kirim Foto Diskusi Tim")
                    }

                    OutlinedButton(
                        onClick = {
                            showAttachmentDialog = false
                            onSendMessage("Dokumen_Laporan_Kelompok_${currentGroupToken ?: "SC"}.pdf", "pdf")
                        },
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kirim Dokumen PDF Proyek")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttachmentDialog = false }) {
                    Text("TUTUP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            shape = RectangleShape
        )
    }

    // Voice Call Dialog (Jitsi Meet Integration)
    if (showCallDialog) {
        val callUrl = "https://meet.jit.si/SingleChat-${currentGroupToken ?: "SC-ROOM"}"
        AlertDialog(
            onDismissRequest = { showCallDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Panggilan Suara Kelompok",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ruang panggilan suara untuk kelompok: ${currentGroupToken ?: ""}")
                    Text(
                        text = callUrl,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Semua anggota kelompok dapat bergabung secara instan tanpa perlu akun terpisah.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCallDialog = false
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(callUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Membuka link panggilan...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text("GABUNG SEKARANG", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCallDialog = false }) {
                    Text("BATAL", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            shape = RectangleShape
        )
    }

    // Image Preview Dialog
    previewImageUrl?.let { imgUrl ->
        AlertDialog(
            onDismissRequest = { previewImageUrl = null },
            title = { Text("Pratinjau Gambar", fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = "Gambar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { previewImageUrl = null }) {
                    Text("TUTUP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            shape = RectangleShape
        )
    }
}

@Composable
private fun MessageItem(
    message: MessageModel,
    isMe: Boolean,
    onImageClick: (String) -> Unit,
    onOpenFile: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Sender info (for other members)
        if (!isMe) {
            Text(
                text = message.sender_email.substringBefore("@"),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        // Message Content Box
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    RectangleShape
                )
                .background(
                    if (isMe) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface
                )
                .padding(10.dp)
        ) {
            val contentColor = if (isMe) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground

            when (message.message_type) {
                "image" -> {
                    Column {
                        AsyncImage(
                            model = message.content,
                            contentDescription = "Gambar terkirim",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clickable { onImageClick(message.content) },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message.created_at ?: "",
                            fontSize = 9.sp,
                            color = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
                "pdf" -> {
                    Column(modifier = Modifier.clickable { onOpenFile(message.content) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "PDF",
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = contentColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message.created_at ?: "",
                            fontSize = 9.sp,
                            color = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
                else -> {
                    Column {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = message.created_at ?: "",
                            fontSize = 9.sp,
                            color = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}
