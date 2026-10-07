package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RcloneParams
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.data.viewmodel.NavigationTab
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSynced
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RcloneConfigScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.setTab(NavigationTab.RECORDER_VAULT)
    }

    val context = LocalContext.current
    val rcloneParams by viewModel.rcloneParams.collectAsState()
    val taskState by viewModel.backgroundTaskState.collectAsState()
    val authState by viewModel.authState.collectAsState()

    var showSecretVisible by remember { mutableStateOf(false) }
    var showRcloneConfModal by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showLogsExpanded by remember { mutableStateOf(false) }

    val compiledCommand = remember(rcloneParams) {
        rcloneParams.buildCommand(rcloneParams.localSourcePath)
    }

    val generatedConf = remember(rcloneParams) {
        rcloneParams.generateRcloneConf()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("rclone_config_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        // --- 1. Header Card & Status ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rclone_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudSync,
                                    contentDescription = "Configuration Rclone & Google Drive",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google Drive & Rclone Config",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Identifiants, chemins & synchronisation auto",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Status Badge
                        Surface(
                            color = when {
                                taskState.isRunning -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                taskState.status == "SUCCESS" -> EmeraldSynced.copy(alpha = 0.2f)
                                taskState.status == "FAILED" || taskState.status == "CANCELLED" -> CrimsonError.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                taskState.isRunning -> MaterialTheme.colorScheme.primary
                                                taskState.status == "SUCCESS" -> EmeraldSynced
                                                taskState.status == "FAILED" || taskState.status == "CANCELLED" -> CrimsonError
                                                else -> AmberPending
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        taskState.isRunning -> "EN COURS"
                                        taskState.status == "SUCCESS" -> "ACTIF"
                                        taskState.status == "CANCELLED" -> "ANNULÉ"
                                        else -> "CONFIGURÉ"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = when {
                                        taskState.isRunning -> MaterialTheme.colorScheme.primary
                                        taskState.status == "SUCCESS" -> EmeraldSynced
                                        taskState.status == "FAILED" || taskState.status == "CANCELLED" -> CrimsonError
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }

                    // Active Background Task Progress Section if running
                    AnimatedVisibility(
                        visible = taskState.isRunning || taskState.status == "SUCCESS" || taskState.status == "CANCELLED",
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = taskState.currentFile.ifBlank { "Tâche de synchronisation Google Drive" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Transféré: ${taskState.transferredBytes} • Vitesse: ${taskState.transferSpeed} • ETA: ${taskState.eta}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                if (taskState.isRunning) {
                                    OutlinedButton(
                                        onClick = { viewModel.cancelBackgroundSyncTask() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("cancel_bg_task_btn")
                                    ) {
                                        Icon(imageVector = Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Annuler", fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = taskState.progress / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .testTag("bg_task_progress_bar"),
                                color = if (taskState.status == "SUCCESS") EmeraldSynced else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Progression: ${taskState.progress}% (${taskState.filesSyncedCount}/${taskState.totalFilesToSync} fichiers)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                TextButton(
                                    onClick = { showLogsExpanded = !showLogsExpanded },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = if (showLogsExpanded) "Masquer logs" else "Voir logs (${taskState.logs.size})",
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (showLogsExpanded && taskState.logs.isNotEmpty()) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        taskState.logs.takeLast(6).forEach { logLine ->
                                            Text(
                                                text = logLine,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = when {
                                                    logLine.contains("✓") || logLine.contains("SUCCESS") || logLine.contains("OK") -> EmeraldSynced
                                                    logLine.contains("ANNULÉ") || logLine.contains("ERR") -> CrimsonError
                                                    else -> TextSecondary
                                                },
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Action Controls: Trigger Automated Sync & Test Google Drive API ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.triggerBackgroundSyncTask() },
                    enabled = !taskState.isRunning,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .testTag("trigger_background_sync_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (taskState.isRunning) "Synchronisation..." else "Synchroniser Maintenant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.testGoogleDriveConnection() },
                    enabled = !taskState.isRunning,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(52.dp)
                        .testTag("test_gdrive_connection_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Sensors, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tester Accès", fontSize = 12.sp)
                }
            }
        }

        // --- 3. Google Drive Credentials Section ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gdrive_credentials_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Identifiants Google Drive API",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Authenticated user status pill
                        Surface(
                            color = if (authState.isSignedIn) EmeraldSynced.copy(alpha = 0.15f) else AmberPending.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            val userLabel = authState.email ?: authState.displayName ?: "Compte Google"
                            Text(
                                text = if (authState.isSignedIn) "Connecté: ${userLabel.take(16)}" else "Non authentifié",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (authState.isSignedIn) EmeraldSynced else AmberPending,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // OAuth Client ID
                    OutlinedTextField(
                        value = rcloneParams.clientId,
                        onValueChange = { viewModel.updateRcloneParams(rcloneParams.copy(clientId = it)) },
                        label = { Text("Google OAuth Client ID") },
                        placeholder = { Text("xxxx-xxxx.apps.googleusercontent.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gdrive_client_id_input"),
                        leadingIcon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
                        trailingIcon = {
                            if (rcloneParams.clientId.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateRcloneParams(rcloneParams.copy(clientId = "")) }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Effacer")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // OAuth Client Secret
                    OutlinedTextField(
                        value = rcloneParams.clientSecret,
                        onValueChange = { viewModel.updateRcloneParams(rcloneParams.copy(clientSecret = it)) },
                        label = { Text("Google OAuth Client Secret") },
                        placeholder = { Text("GOCSPX-xxxxxxxxxxxx") },
                        singleLine = true,
                        visualTransformation = if (showSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gdrive_client_secret_input"),
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showSecretVisible = !showSecretVisible }) {
                                Icon(
                                    imageVector = if (showSecretVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showSecretVisible) "Masquer secret" else "Afficher secret"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Service Account File or Token JSON (Expandable / Optional)
                    OutlinedTextField(
                        value = rcloneParams.serviceAccountFile,
                        onValueChange = { viewModel.updateRcloneParams(rcloneParams.copy(serviceAccountFile = it)) },
                        label = { Text("Fichier Service Account JSON (Optionnel)") },
                        placeholder = { Text("/path/to/service-account.json") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gdrive_service_account_input"),
                        leadingIcon = { Icon(Icons.Outlined.FilePresent, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Drive Scope Selector
                    Text(
                        text = "Portée de l'API Google Drive (--drive-scope)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "drive" to "drive (Accès Complet)",
                            "drive.file" to "drive.file (Spécifique)",
                            "drive.readonly" to "drive.readonly (Lecture)",
                            "drive.appdata" to "drive.appdata (Invisible)"
                        ).forEach { (scopeKey, scopeLabel) ->
                            FilterChip(
                                selected = rcloneParams.driveScope == scopeKey,
                                onClick = { viewModel.updateRcloneParams(rcloneParams.copy(driveScope = scopeKey)) },
                                label = { Text(scopeLabel, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Automated Cloud Synchronization Settings Card ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("automated_cloud_sync_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Autorenew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Synchronisation Automatique",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-sync after each new recording
                    ParamSwitchRow(
                        title = "Synchronisation Automatique après Enregistrement",
                        desc = "Téléverse immédiatement chaque nouvelle capture vocale vers Google Drive",
                        checked = rcloneParams.autoSyncOnRecord,
                        onCheckedChange = { viewModel.updateRcloneParams(rcloneParams.copy(autoSyncOnRecord = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Background scheduled worker
                    ParamSwitchRow(
                        title = "Tâche d'Arrière-Plan Planifiée",
                        desc = "Vérifie et synchronise périodiquement les enregistrements en arrière-plan",
                        checked = taskState.isScheduledWorkerActive,
                        onCheckedChange = { viewModel.toggleScheduledBackgroundWorker(it) }
                    )

                    if (taskState.isScheduledWorkerActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Fréquence de la tâche de fond",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("15 min", "30 min", "1 heure", "6 heures", "Quotidien").forEach { interval ->
                                FilterChip(
                                    selected = rcloneParams.backgroundSyncInterval == interval,
                                    onClick = { viewModel.updateRcloneParams(rcloneParams.copy(backgroundSyncInterval = interval)) },
                                    label = { Text(interval, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Wi-Fi Only constraint
                    ParamSwitchRow(
                        title = "Contrainte Wi-Fi Uniquement",
                        desc = "Suspend les téléversements sur réseau 4G/5G pour préserver le forfait data",
                        checked = rcloneParams.wifiOnlyConstraint,
                        onCheckedChange = { viewModel.updateRcloneParams(rcloneParams.copy(wifiOnlyConstraint = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Auto retry on failure
                    ParamSwitchRow(
                        title = "Nouvelle tentative automatique",
                        desc = "Réessaie le transfert en cas de perte temporaire de connectivité",
                        checked = rcloneParams.autoRetryOnFailure,
                        onCheckedChange = { viewModel.updateRcloneParams(rcloneParams.copy(autoRetryOnFailure = it)) }
                    )
                }
            }
        }

        // --- 5. Storage Paths & Rclone Destination Settings ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("storage_paths_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FolderCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Chemins Locaux & Distants",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Local Source Path
                    OutlinedTextField(
                        value = rcloneParams.localSourcePath,
                        onValueChange = { viewModel.updateRcloneParams(rcloneParams.copy(localSourcePath = it)) },
                        label = { Text("Dossier Source Local des Enregistrements") },
                        placeholder = { Text("./storage/recordings/") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rclone_local_path_input"),
                        leadingIcon = { Icon(Icons.Outlined.FolderSpecial, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Remote Name
                    OutlinedTextField(
                        value = rcloneParams.remoteName,
                        onValueChange = { viewModel.updateRcloneParams(rcloneParams.copy(remoteName = it)) },
                        label = { Text("Nom du Remote Rclone") },
                        placeholder = { Text("gdrive") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rclone_remote_name_input"),
                        leadingIcon = { Icon(Icons.Outlined.Storage, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Destination Remote Path
                    OutlinedTextField(
                        value = rcloneParams.remotePath,
                        onValueChange = { viewModel.updateRcloneParams(rcloneParams.copy(remotePath = it)) },
                        label = { Text("Chemin Distant Cible Google Drive") },
                        placeholder = { Text("gdrive:/Z-CORE/Captures/") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rclone_remote_path_input"),
                        leadingIcon = { Icon(Icons.Outlined.CloudUpload, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // File Filter
                    OutlinedTextField(
                        value = rcloneParams.fileFilter,
                        onValueChange = { viewModel.updateRcloneParams(rcloneParams.copy(fileFilter = it)) },
                        label = { Text("Filtres d'extensions audio (--include)") },
                        placeholder = { Text("*.wav, *.m4a, *.mp3, *.aac") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.AudioFile, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Root Folder ID (optional)
                    OutlinedTextField(
                        value = rcloneParams.rootFolderId,
                        onValueChange = { viewModel.updateRcloneParams(rcloneParams.copy(rootFolderId = it)) },
                        label = { Text("Google Drive Root Folder ID (Optionnel)") },
                        placeholder = { Text("1A2b3C4d5E6f7G8h9I...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.Tag, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // --- 6. Performance & Transfer Engine Settings ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Moteur de Transfert & Performance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Mode
                    Text(
                        text = "Mode d'action Rclone",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("copy", "sync", "move", "check").forEach { act ->
                            FilterChip(
                                selected = rcloneParams.syncAction == act,
                                onClick = { viewModel.updateRcloneParams(rcloneParams.copy(syncAction = act)) },
                                label = { Text(act.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Transfers & Checkers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Transfers: ${rcloneParams.transfers}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(1, 2, 4, 8).forEach { num ->
                                    FilterChip(
                                        selected = rcloneParams.transfers == num,
                                        onClick = { viewModel.updateRcloneParams(rcloneParams.copy(transfers = num)) },
                                        label = { Text("$num", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Checkers: ${rcloneParams.checkers}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(2, 4, 8, 16).forEach { num ->
                                    FilterChip(
                                        selected = rcloneParams.checkers == num,
                                        onClick = { viewModel.updateRcloneParams(rcloneParams.copy(checkers = num)) },
                                        label = { Text("$num", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chunk size & Bandwidth limit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chunk Size (--drive-chunk-size)",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("16M", "32M", "64M", "128M", "256M").forEach { size ->
                                    FilterChip(
                                        selected = rcloneParams.driveChunkSize == size,
                                        onClick = { viewModel.updateRcloneParams(rcloneParams.copy(driveChunkSize = size)) },
                                        label = { Text(size, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bande Passante (--bwlimit)",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Unlimited", "2M", "5M", "10M", "25M").forEach { bw ->
                                    FilterChip(
                                        selected = rcloneParams.bandwidthLimit == bw,
                                        onClick = { viewModel.updateRcloneParams(rcloneParams.copy(bandwidthLimit = bw)) },
                                        label = { Text(if (bw == "Unlimited") "Illimité" else bw, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fast List & Verbose & DryRun Switches
                    ParamSwitchRow(
                        title = "Optimisation Rapide (--fast-list)",
                        desc = "Réduit drastiquement le nombre de requêtes API Google Drive",
                        checked = rcloneParams.useFastList,
                        onCheckedChange = { viewModel.updateRcloneParams(rcloneParams.copy(useFastList = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    ParamSwitchRow(
                        title = "Mode Journal Détaillé (-v)",
                        desc = "Enregistre chaque événement de transfert dans le journal",
                        checked = rcloneParams.verbose,
                        onCheckedChange = { viewModel.updateRcloneParams(rcloneParams.copy(verbose = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    ParamSwitchRow(
                        title = "Simulation Dry Run (--dry-run)",
                        desc = "Vérifie les correspondances de fichiers sans téléversement effectif",
                        checked = rcloneParams.dryRun,
                        onCheckedChange = { viewModel.updateRcloneParams(rcloneParams.copy(dryRun = it)) }
                    )
                }
            }
        }

        // --- 7. Presets Quick Selector Bar ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Profils de Configuration Rclone",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Appliquez instantanément des réglages testés selon vos besoins.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetChip(
                            label = "🚀 Vitesse Max",
                            subtext = "8 transfers • 128M chunk",
                            onClick = { viewModel.applyRclonePreset("FAST") }
                        )
                        PresetChip(
                            label = "⚖️ Standard Z-CORE",
                            subtext = "4 transfers • 64M chunk",
                            onClick = { viewModel.applyRclonePreset("BALANCED") }
                        )
                        PresetChip(
                            label = "🔋 Éco Batterie & 4G",
                            subtext = "1 transfer • 5M limit • Wi-Fi",
                            onClick = { viewModel.applyRclonePreset("SAVER") }
                        )
                        PresetChip(
                            label = "🧪 Simulation Dry-Run",
                            subtext = "--dry-run test",
                            onClick = { viewModel.applyRclonePreset("DRY_RUN") }
                        )
                    }
                }
            }
        }

        // --- 8. Generated rclone.conf File Section ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rclone_conf_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fichier de Configuration rclone.conf",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("rclone.conf", generatedConf)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "rclone.conf copié dans le presse-papiers !", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copier rclone.conf",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Emplacement: ${rcloneParams.rcloneConfPath}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = generatedConf,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = EmeraldSynced,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // --- 9. Real-Time Compiled CLI Command Preview ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rclone_command_preview_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Commande Rclone Compilée en Direct",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Rclone Command", compiledCommand)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Commande Rclone copiée !", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copier Commande",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { showResetDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RestartAlt,
                                    contentDescription = "Réinitialiser",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = compiledCommand,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "Configuration sauvegardée avec succès !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_rclone_config_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sauvegarder la Configuration", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Réinitialiser les paramètres ?") },
            text = { Text("Voulez-vous restaurer les paramètres Rclone par défaut pour Google Drive ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetRcloneParamsToDefault()
                        showResetDialog = false
                    }
                ) {
                    Text("Réinitialiser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    subtext: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(text = subtext, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ParamSwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
