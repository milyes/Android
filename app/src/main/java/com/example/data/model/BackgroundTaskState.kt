package com.example.data.model

data class BackgroundTaskState(
    val isRunning: Boolean = false,
    val taskId: String? = null,
    val taskType: String = "Google Drive Sync Worker",
    val status: String = "IDLE", // "IDLE", "INITIALIZING", "AUTHENTICATING", "SCANNING", "TRANSFERRING", "FINALIZING", "SUCCESS", "FAILED", "CANCELLED"
    val progress: Int = 0,
    val currentFile: String = "",
    val transferredBytes: String = "0 B",
    val totalBytes: String = "0 B",
    val transferSpeed: String = "0 KB/s",
    val eta: String = "--",
    val filesSyncedCount: Int = 0,
    val totalFilesToSync: Int = 0,
    val logs: List<String> = emptyList(),
    val lastCompletedTimestamp: Long? = null,
    val isScheduledWorkerActive: Boolean = false,
    val nextScheduledRun: String = "--"
)
