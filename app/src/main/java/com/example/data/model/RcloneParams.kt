package com.example.data.model

data class RcloneParams(
    val remoteName: String = "gdrive",
    val remotePath: String = "gdrive:/Z-CORE/Captures/",
    val localSourcePath: String = "./storage/recordings/",
    val clientId: String = "",
    val clientSecret: String = "",
    val serviceAccountFile: String = "",
    val tokenJson: String = "",
    val rootFolderId: String = "",
    val syncAction: String = "copy", // "copy", "sync", "move", "check"
    val bandwidthLimit: String = "Unlimited", // "Unlimited", "2M", "5M", "10M", "25M", "50M"
    val transfers: Int = 4, // 1, 2, 4, 8
    val checkers: Int = 8, // 2, 4, 8, 16
    val driveChunkSize: String = "64M", // "8M", "16M", "32M", "64M", "128M", "256M"
    val driveScope: String = "drive", // "drive", "drive.file", "drive.readonly", "drive.appdata"
    val useFastList: Boolean = true,
    val useTrash: Boolean = false,
    val verbose: Boolean = true,
    val dryRun: Boolean = false,
    val fileFilter: String = "*.wav, *.m4a, *.mp3, *.aac",
    val autoSyncOnRecord: Boolean = true,
    val backgroundSyncInterval: String = "Immediate", // "Immediate", "15 min", "30 min", "1 hour", "6 hours", "Daily"
    val wifiOnlyConstraint: Boolean = false,
    val autoRetryOnFailure: Boolean = true,
    val rcloneConfPath: String = "~/.config/rclone/rclone.conf"
) {
    fun buildCommand(sourcePath: String = localSourcePath): String {
        val sb = StringBuilder()
        sb.append("rclone ").append(syncAction).append(" ")
        sb.append(if (sourcePath.isNotBlank()) sourcePath.trim() else "./storage/recordings/").append(" ")
        sb.append(if (remotePath.isNotBlank()) remotePath.trim() else "$remoteName:/")

        if (bandwidthLimit != "Unlimited" && bandwidthLimit.isNotBlank()) {
            sb.append(" --bwlimit ").append(bandwidthLimit)
        }
        if (transfers != 4) {
            sb.append(" --transfers ").append(transfers)
        }
        if (checkers != 8) {
            sb.append(" --checkers ").append(checkers)
        }
        if (driveChunkSize != "64M" && driveChunkSize.isNotBlank()) {
            sb.append(" --drive-chunk-size ").append(driveChunkSize)
        }
        if (driveScope != "drive") {
            sb.append(" --drive-scope ").append(driveScope)
        }
        if (rootFolderId.isNotBlank()) {
            sb.append(" --drive-root-folder-id ").append(rootFolderId.trim())
        }
        if (useFastList) {
            sb.append(" --fast-list")
        }
        if (!useTrash) {
            sb.append(" --drive-use-trash=false")
        }
        if (verbose) {
            sb.append(" -v")
        }
        if (dryRun) {
            sb.append(" --dry-run")
        }
        if (fileFilter.isNotBlank()) {
            val exts = fileFilter.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (ext in exts) {
                sb.append(" --include \"").append(ext).append("\"")
            }
        }
        return sb.toString()
    }

    fun generateRcloneConf(): String {
        val sb = StringBuilder()
        sb.append("[$remoteName]\n")
        sb.append("type = drive\n")
        sb.append("scope = $driveScope\n")
        if (clientId.isNotBlank()) {
            sb.append("client_id = $clientId\n")
        }
        if (clientSecret.isNotBlank()) {
            sb.append("client_secret = $clientSecret\n")
        }
        if (rootFolderId.isNotBlank()) {
            sb.append("root_folder_id = $rootFolderId\n")
        }
        if (serviceAccountFile.isNotBlank()) {
            sb.append("service_account_file = $serviceAccountFile\n")
        }
        if (tokenJson.isNotBlank()) {
            sb.append("token = $tokenJson\n")
        }
        sb.append("chunk_size = $driveChunkSize\n")
        sb.append("use_trash = $useTrash\n")
        return sb.toString()
    }
}
