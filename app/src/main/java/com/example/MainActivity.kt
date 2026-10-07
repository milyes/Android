package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.data.viewmodel.NavigationTab
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.DeviceScreen
import com.example.ui.screens.RcloneConfigScreen
import com.example.ui.screens.RecorderVaultScreen
import com.example.ui.screens.RecordingsListScreen
import com.example.ui.screens.TermuxMacroScreen
import com.example.ui.theme.AudioSyncHubTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AudioSyncHubTheme {
        AudioSyncApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSyncApp(viewModel: AudioSyncViewModel = viewModel()) {
  val currentTab by viewModel.currentTab.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Audio Sync Hub",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Z-CORE Voice & Google Drive Sync Infrastructure",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.setTab(NavigationTab.DEVICE_INFO) },
            modifier = Modifier.testTag("tab_device_info")
          ) {
            Icon(
              imageVector = if (currentTab == NavigationTab.DEVICE_INFO) Icons.Filled.Smartphone else Icons.Outlined.Smartphone,
              contentDescription = "Device UI Specs",
              tint = if (currentTab == NavigationTab.DEVICE_INFO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      NavigationBar(
        modifier = Modifier.testTag("bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets.navigationBars
      ) {
        NavigationBarItem(
          selected = currentTab == NavigationTab.RECORDER_VAULT,
          onClick = { viewModel.setTab(NavigationTab.RECORDER_VAULT) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.RECORDER_VAULT) Icons.Filled.Mic else Icons.Outlined.Mic,
              contentDescription = "Capture Vocale"
            )
          },
          label = { Text("Record", fontSize = 11.sp) },
          modifier = Modifier.testTag("tab_recorder")
        )

        NavigationBarItem(
          selected = currentTab == NavigationTab.RECORDINGS_LIST,
          onClick = { viewModel.setTab(NavigationTab.RECORDINGS_LIST) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.RECORDINGS_LIST) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
              contentDescription = "Fichiers Enregistrés"
            )
          },
          label = { Text("Files", fontSize = 11.sp) },
          modifier = Modifier.testTag("tab_recordings_list")
        )

        NavigationBarItem(
          selected = currentTab == NavigationTab.CLOUD_SYNC,
          onClick = { viewModel.setTab(NavigationTab.CLOUD_SYNC) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.CLOUD_SYNC) Icons.Filled.CloudSync else Icons.Outlined.CloudUpload,
              contentDescription = "Google Drive Sync"
            )
          },
          label = { Text("Sync", fontSize = 11.sp) },
          modifier = Modifier.testTag("tab_cloud_sync")
        )

        NavigationBarItem(
          selected = currentTab == NavigationTab.RCLONE_CONFIG,
          onClick = { viewModel.setTab(NavigationTab.RCLONE_CONFIG) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.RCLONE_CONFIG) Icons.Filled.Tune else Icons.Outlined.Tune,
              contentDescription = "Rclone Config"
            )
          },
          label = { Text("Rclone", fontSize = 11.sp) },
          modifier = Modifier.testTag("tab_rclone_config")
        )

        NavigationBarItem(
          selected = currentTab == NavigationTab.TERMUX_MACROS,
          onClick = { viewModel.setTab(NavigationTab.TERMUX_MACROS) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.TERMUX_MACROS) Icons.Filled.Terminal else Icons.Outlined.Terminal,
              contentDescription = "Macros Termux / ADB"
            )
          },
          label = { Text("Macros", fontSize = 11.sp) },
          modifier = Modifier.testTag("tab_macros")
        )

        NavigationBarItem(
          selected = currentTab == NavigationTab.AI_STUDIO,
          onClick = { viewModel.setTab(NavigationTab.AI_STUDIO) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.AI_STUDIO) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
              contentDescription = "Gemini AI"
            )
          },
          label = { Text("AI", fontSize = 11.sp) },
          modifier = Modifier.testTag("tab_ai_studio")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      AnimatedContent(
        targetState = currentTab,
        label = "tab_transition"
      ) { tab ->
        when (tab) {
          NavigationTab.RECORDER_VAULT -> RecorderVaultScreen(viewModel = viewModel)
          NavigationTab.RECORDINGS_LIST -> RecordingsListScreen(viewModel = viewModel)
          NavigationTab.CLOUD_SYNC -> CloudSyncScreen(viewModel = viewModel)
          NavigationTab.RCLONE_CONFIG -> RcloneConfigScreen(viewModel = viewModel)
          NavigationTab.TERMUX_MACROS -> TermuxMacroScreen(viewModel = viewModel)
          NavigationTab.DEVICE_INFO -> DeviceScreen(viewModel = viewModel)
          NavigationTab.AI_STUDIO -> AiStudioScreen(viewModel = viewModel)
        }
      }
    }
  }
}
