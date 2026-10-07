package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommandMacro
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.data.viewmodel.NavigationTab
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxMacroScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.setTab(NavigationTab.RECORDER_VAULT)
    }

    val context = LocalContext.current
    val macros by viewModel.macros.collectAsState()

    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showTermuxGuideDialog by remember { mutableStateOf(false) }
    var showAddMacroDialog by remember { mutableStateOf(false) }
    var macroToDelete by remember { mutableStateOf<CommandMacro?>(null) }

    // Dialog inputs
    var newMacroName by remember { mutableStateOf("") }
    var newMacroCategory by remember { mutableStateOf("TERMUX") }
    var newMacroCommand by remember { mutableStateOf("") }
    var newMacroDesc by remember { mutableStateOf("") }

    val filteredMacros = remember(macros, selectedCategory, searchQuery) {
        macros.filter { macro ->
            val matchesCategory = when (selectedCategory) {
                "ALL" -> true
                "FAVORITES" -> macro.isFavorite
                else -> macro.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                    macro.name.contains(searchQuery, ignoreCase = true) ||
                    macro.commandText.contains(searchQuery, ignoreCase = true) ||
                    macro.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Banner & Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Macros & Scripts Termux / ADB",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Créez, gérez et déclenchez vos alias de commandes",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Row {
                            IconButton(
                                onClick = { showTermuxGuideDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Guide",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { showAddMacroDialog = true },
                                modifier = Modifier.testTag("add_macro_btn"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajouter", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Target Phone Number Display Badge
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PhoneAndroid,
                                    contentDescription = "Terminal mobile cible",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Terminal Cible Z_GHOST",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "+1 438 985-5041",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Target Phone", "+14389855041")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Numéro copié: +1 438 985-5041", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copier le numéro",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher une macro ou commande...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "Recherche")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Effacer")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("macro_search_field"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            ScrollableTabRow(
                selectedTabIndex = when (selectedCategory) {
                    "ALL" -> 0
                    "FAVORITES" -> 1
                    "RCLONE" -> 2
                    "TERMUX" -> 3
                    "ADB" -> 4
                    "Z_GHOST" -> 5
                    "DK" -> 6
                    "SCRIPT" -> 7
                    else -> 0
                },
                edgePadding = 0.dp,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                val categories = listOf("ALL", "FAVORITES", "RCLONE", "TERMUX", "ADB", "Z_GHOST", "DK", "SCRIPT")
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                when (cat) {
                                    "ALL" -> "Tous (${macros.size})"
                                    "FAVORITES" -> "★ Favoris"
                                    else -> cat
                                }
                            )
                        },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Macros List Interface
            if (filteredMacros.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aucune macro trouvée",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredMacros, key = { it.id }) { macro ->
                        MacroCard(
                            macro = macro,
                            onTrigger = { viewModel.triggerMacro(macro) },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Command", macro.commandText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Commande copiée: ${macro.name}", Toast.LENGTH_SHORT).show()
                            },
                            onFavoriteToggle = { viewModel.toggleFavoriteMacro(macro) },
                            onDelete = { macroToDelete = macro }
                        )
                    }
                }
            }
        }
    }

    // Modal Dialog: Add New Macro / Script Alias
    if (showAddMacroDialog) {
        AlertDialog(
            onDismissRequest = { showAddMacroDialog = false },
            title = { Text("Nouvelle Macro / Alias Termux") },
            text = {
                Column {
                    Text(
                        text = "Enregistrez une commande ou script shell exécutable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newMacroName,
                        onValueChange = { newMacroName = it },
                        label = { Text("Nom de la macro / alias") },
                        placeholder = { Text("Ex: Relancer Service Audio") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newMacroCategory,
                        onValueChange = { newMacroCategory = it },
                        label = { Text("Catégorie (TERMUX, RCLONE, ADB, SCRIPT...)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newMacroCommand,
                        onValueChange = { newMacroCommand = it },
                        label = { Text("Commande Shell / Script") },
                        placeholder = { Text("Ex: termux-tts-speak \"Synchronisation terminée\"") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newMacroDesc,
                        onValueChange = { newMacroDesc = it },
                        label = { Text("Description (Optionnel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addMacro(
                            name = newMacroName,
                            category = newMacroCategory,
                            commandText = newMacroCommand,
                            description = newMacroDesc
                        )
                        newMacroName = ""
                        newMacroCommand = ""
                        newMacroDesc = ""
                        showAddMacroDialog = false
                    },
                    modifier = Modifier.testTag("save_macro_confirm_btn")
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMacroDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialog: Delete Macro Confirmation
    macroToDelete?.let { macro ->
        AlertDialog(
            onDismissRequest = { macroToDelete = null },
            title = { Text("Supprimer la macro ?") },
            text = { Text("Voulez-vous vraiment supprimer '${macro.name}' (${macro.commandText}) ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMacro(macro)
                        macroToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { macroToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Termux Info Dialog
    if (showTermuxGuideDialog) {
        AlertDialog(
            onDismissRequest = { showTermuxGuideDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Télécharger Termux Officiel")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Important: Ne téléchargez PAS Termux depuis Google Play Store (version obsolète depuis 2020).\n",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sources officielles recommandées:\n" +
                                "1. F-Droid (Recommandé):\n   f-droid.org/packages/com.termux\n\n" +
                                "2. GitHub Officiel Releases:\n   github.com/termux/termux-app/releases\n\n" +
                                "Après installation, installez le paquet API avec:\n" +
                                "pkg update && pkg install termux-api",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermuxGuideDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}

@Composable
fun MacroCard(
    macro: CommandMacro,
    onTrigger: () -> Unit,
    onCopy: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("macro_card_${macro.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = macro.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = macro.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (macro.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favori",
                            tint = if (macro.isFavorite) MaterialTheme.colorScheme.primary else TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Supprimer macro",
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = macro.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = macro.commandText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Row {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("copy_macro_btn_${macro.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copier",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = onTrigger,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("trigger_macro_btn_${macro.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = "Lancer macro",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

