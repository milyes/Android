package com.example.data.model

data class LanceBinFileState(
    val fileName: String = "LANCE_BIN.HTML",
    val localPath: String = "",
    val cloudPath: String = "gdrive:/Z-CORE/LANCE_BIN.HTML",
    val existsLocally: Boolean = false,
    val isUploaded: Boolean = false,
    val isSynced: Boolean = false,
    val fileSizeKb: Float = 0f,
    val lastUpdated: Long = 0L,
    val content: String = ""
)
