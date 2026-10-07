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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommandMacro
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.data.viewmodel.NavigationTab
import com.example.ui.theme.EmeraldSynced
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.setTab(NavigationTab.RECORDER_VAULT)
    }

    val context = LocalContext.current
    var selectedSection by remember { mutableStateOf(0) }
    var libSearchQuery by remember { mutableStateOf("") }

    val nativeLibs = remember {
        listOf(
            "lib_vnd_client.dk.samsung.so" to "Digital Key Vendor Client Library",
            "lib_native_client.dk.samsung.so" to "Digital Key Native Core Client",
            "lib_nativeJni.dk.samsung.so" to "Digital Key JNI Bridge",
            "org.carconnectivity.android.digitalkey.secureelement" to "Car Connectivity Digital Key SE",
            "org.carconnectivity.android.digitalkey.rangingintent" to "CCC Digital Key UWB Ranging",
            "com.samsung.android.knox.knoxsdk" to "Samsung Knox SDK v36 Framework",
            "com.samsung.android.authfw" to "Samsung Authentication Framework",
            "libOpenCL.so" to "Adreno GPU OpenCL Acceleration",
            "libadsprpc.so" to "Qualcomm ADSP RPC Subsystem",
            "libcdsprpc.so" to "Qualcomm Compute DSP Subsystem",
            "libsdsprpc.so" to "Qualcomm Sensor DSP Subsystem",
            "libtensorflowlite_c.camera.samsung.so" to "Samsung Camera AI TensorFlow Lite Engine",
            "libFaceRecognition.arcsoft.so" to "ArcSoft Facial Biometric Engine",
            "com.google.android.gms" to "Google Play Services Runtime (v233515045)",
            "android.hardware.nfc.ese" to "Embedded Secure Element (eSE) NFC",
            "vendor.qti.hardware.fingerprint.V1_0" to "Qualcomm Fingerprint HAL"
        )
    }

    val filteredLibs = remember(libSearchQuery) {
        if (libSearchQuery.isBlank()) nativeLibs
        else nativeLibs.filter {
            it.first.contains(libSearchQuery, ignoreCase = true) ||
                    it.second.contains(libSearchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("device_screen_container")
    ) {
        // --- Header Device Banner ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("device_header_card"),
            shape = RoundedCornerShape(18.dp),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Smartphone,
                                contentDescription = "Device Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Samsung Galaxy S22",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Modèle: SM-S901W • Brand: samsung",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Surface(
                        color = EmeraldSynced.copy(alpha = 0.2f),
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
                                    .background(EmeraldSynced)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACTIF (Z_GHOST)",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSynced,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Key specs pill bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SpecPill(label = "SoC", value = "Qualcomm qcom")
                    SpecPill(label = "RAM", value = "7.58 GB")
                    SpecPill(label = "Écran", value = "1080x2340 480dpi")
                    SpecPill(label = "OS", value = "Android 13 (API 33)")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Section Navigation Tabs ---
        TabRow(
            selectedTabIndex = selectedSection,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .testTag("device_sections_tabrow")
        ) {
            Tab(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                text = { Text("Identifiants", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                text = { Text("Hardware", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedSection == 2,
                onClick = { selectedSection = 2 },
                text = { Text("Système & Sec", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedSection == 3,
                onClick = { selectedSection = 3 },
                text = { Text("Libs Natives", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Section Contents ---
        when (selectedSection) {
            0 -> IdentifiersSection(context)
            1 -> HardwareSection()
            2 -> SystemSecuritySection(context)
            3 -> NativeLibrariesSection(libSearchQuery, { libSearchQuery = it }, filteredLibs, context)
        }
    }
}

@Composable
private fun SpecPill(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
            Text(text = value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun IdentifiersSection(context: Context) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().testTag("identifiers_section_list")
    ) {
        item {
            InfoCard(
                title = "Identifiants Cibles",
                items = listOf(
                    "Android ID" to "3867290799904350142",
                    "Numéro de Série" to "r0q:R3CT60MHGWL",
                    "IMEI" to "354818254529107",
                    "Appareil / Produit" to "r0q / r0qcsx",
                    "Modèle / Marque" to "SM-S901W (samsung)",
                    "Partenaire Client ID" to "android-samsung-ss"
                ),
                context = context
            )
        }

        item {
            InfoCard(
                title = "Réseau Cellulaire & Opérateur",
                items = listOf(
                    "Opérateur SIM" to "Fido (MCC/MNC: 302370)",
                    "Opérateur Cellulaire" to "Rogers Wireless (MCC/MNC: 302720)",
                    "IMSI (Tronqué)" to "302370507900000",
                    "Identifiant FSI" to "1962",
                    "Région / Timezone" to "fr-CA (America/Toronto)"
                ),
                context = context
            )
        }
    }
}

@Composable
private fun HardwareSection() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().testTag("hardware_section_list")
    ) {
        item {
            InfoCard(
                title = "Architecture & Proprocesseur",
                items = listOf(
                    "Plateforme / SoC" to "Qualcomm Snapdragon 8 Gen 1 (qcom)",
                    "Cœurs de Processeur" to "8 cœurs physiques",
                    "ABIs Compatibles" to "arm64-v8a, armeabi-v7a, armeabi",
                    "Mémoire RAM Totale" to "7.58 GB (7,580,258,304 octets)",
                    "Type Appareil" to "Smartphone (Low RAM = false)"
                )
            )
        }

        item {
            InfoCard(
                title = "Écran & Rendu Graphique",
                items = listOf(
                    "Résolution Écran" to "1080 x 2340 pixels",
                    "Densité Écran" to "480 DPI (Normal layout, 360dp)",
                    "Version GLES" to "OpenGL ES 3.2 (Engine 196610)",
                    "Extensions Adreno" to "GL_QCOM_tiled_rendering, GL_OVR_multiview, GL_EXT_texture_norm16",
                    "Vulkan Level" to "Vulkan 1.1 (Version 4198400)"
                )
            )
        }

        item {
            InfoCard(
                title = "Capteurs & Hardware Supporté",
                items = listOf(
                    "Capteurs Détectés" to "Accéléromètre, Gyroscope, Boussole, Baromètre, Capteur de pas, Capteur de proximité, Capteur de lumière",
                    "Biométrie & Sécurité" to "Scanner empreinte digitale (Fingerprint) & Reconnaissance faciale",
                    "NFC & CarConnectivity" to "NFC Host Card Emulation (HCE/eSE), CCC Digital Key UWB Ranging"
                )
            )
        }
    }
}

@Composable
private fun SystemSecuritySection(context: Context) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().testTag("system_security_section_list")
    ) {
        item {
            InfoCard(
                title = "SE & Microprogramme",
                items = listOf(
                    "Version Android" to "Android 13 (SDK 33)",
                    "Build ID / Fingerprint" to "samsung/r0qcsx/r0q:13/TP1A.220624.014/S901WVLU3CWGI",
                    "Correctif de Sécurité" to "2023-08-01",
                    "Version Radio / Baseband" to "S901WVLU3CWGI",
                    "Version Bootloader" to "S901WVLU3CWGI",
                    "Google Play Services" to "v233515045"
                ),
                context = context
            )
        }

        item {
            InfoCard(
                title = "Infrastructures Samsung & Digital Key",
                items = listOf(
                    "Samsung Knox SDK" to "API Level 33, 34, 35, 36 (Knox Analytics & RemoteDesktop)",
                    "CarConnectivity Digital Key" to "CCC Digital Key (RangingIntent, SecureElement, TimeSync)",
                    "OEM Key Config" to "FMC (Google Play Auto-Install Key)",
                    "Keystore Hardware" to "StrongBox Keystore Level 4 / Hardware Keystore 100"
                ),
                context = context
            )
        }
    }
}

@Composable
private fun NativeLibrariesSection(
    query: String,
    onQueryChange: (String) -> Unit,
    libs: List<Pair<String, String>>,
    context: Context
) {
    Column(modifier = Modifier.fillMaxWidth().testTag("native_libs_section")) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Filtrer les librairies ou composants...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth().testTag("lib_search_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(libs) { (libName, desc) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = libName,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Library", libName)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copie: $libName", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copier nom de librairie",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    items: List<Pair<String, String>>,
    context: Context? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            items.forEachIndexed { index, (label, valStr) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                        Text(
                            text = valStr,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = if (valStr.contains(":") || valStr.contains("0") || valStr.contains("3867")) FontFamily.Monospace else FontFamily.Default,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }

                    if (context != null) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(label, valStr)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "$label copié!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copier $label",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
