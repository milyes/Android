package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LanceBinFileState
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.ui.theme.EmeraldSynced
import com.example.ui.theme.TextSecondary
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun LanceBinUploadCard(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lanceBinState by viewModel.lanceBinState.collectAsState()
    val isSyncing by viewModel.isSyncingActive.collectAsState()

    var showHtmlPreviewDialog by remember { mutableStateOf(false) }

    // File Picker for custom LANCE_BIN.HTML upload / import
    val htmlFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    viewModel.saveImportedLanceBinContent(content)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur lecture fichier: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lance_bin_upload_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (lanceBinState.isSynced) EmeraldSynced.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row with Title and Cloud Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (lanceBinState.isSynced) EmeraldSynced.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = "Fichier LANCE_BIN.HTML",
                            tint = if (lanceBinState.isSynced) EmeraldSynced else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LANCE_BIN.HTML",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (lanceBinState.isSynced) EmeraldSynced.copy(alpha = 0.2f) else Color(0xFFFFA000).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (lanceBinState.isSynced) "SYNCHRONISÉ" else "PRÊT",
                                    color = if (lanceBinState.isSynced) EmeraldSynced else Color(0xFFFFA000),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Dashboard d'exécution & Lanceur RClone Terminal",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // File Information Details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Emplacement local :",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "./storage/LANCE_BIN.HTML",
                            color = EmeraldSynced,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Destination Cloud :",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = lanceBinState.cloudPath,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Taille du script :",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${String.format("%.1f", lanceBinState.fileSizeKb)} KB",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Action: Uploader LANCE_BIN.HTML Button
            Button(
                onClick = { viewModel.uploadLanceBinFile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_lance_bin_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                enabled = !isSyncing
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudUpload,
                    contentDescription = "Uploader LANCE_BIN.HTML",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "UPLOADER LANCE_BIN.HTML",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Action Row: Pick Custom File, Preview, and Regenerate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        htmlFilePickerLauncher.launch(arrayOf("text/html", "application/octet-stream", "*/*"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_lance_bin_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Importer", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { showHtmlPreviewDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_lance_bin_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aperçu", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        val file = viewModel.getLanceBinStorageFile()
                        clipboardManager.setText(AnnotatedString("rclone copy ${file.absolutePath} gdrive:/Z-CORE/LANCE_BIN.HTML"))
                        Toast.makeText(context, "Commande RClone copiée dans le presse-papier!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_lance_bin_cmd_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copier", fontSize = 11.sp)
                }
            }
        }
    }

    // Modal Dialog to display HTML preview
    if (showHtmlPreviewDialog) {
        Dialog(
            onDismissRequest = { showHtmlPreviewDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Dialog Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Code,
                                contentDescription = null,
                                tint = EmeraldSynced
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LANCE_BIN.HTML Aperçu Web",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { showHtmlPreviewDialog = false }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Fermer")
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // WebView rendering local LANCE_BIN.HTML
                    Box(modifier = Modifier.weight(1f)) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewClient = WebViewClient()
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    val contentToLoad = lanceBinState.content.ifEmpty {
                                        "<h1>LANCE_BIN.HTML vide</h1>"
                                    }
                                    loadDataWithBaseURL(null, contentToLoad, "text/html", "UTF-8", null)
                                }
                            },
                            update = { webView ->
                                if (lanceBinState.content.isNotEmpty()) {
                                    webView.loadDataWithBaseURL(null, lanceBinState.content, "text/html", "UTF-8", null)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Dialog Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.generateDefaultLanceBinHtml(showToast = true)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Régénérer")
                        }

                        Button(
                            onClick = {
                                showHtmlPreviewDialog = false
                                viewModel.uploadLanceBinFile()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uploader vers Cloud")
                        }
                    }
                }
            }
        }
    }
}
