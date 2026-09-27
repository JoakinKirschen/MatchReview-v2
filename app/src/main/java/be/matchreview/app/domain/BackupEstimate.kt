package be.matchreview.app.domain

object BackupEstimate {
    private const val DATABASE_ALLOWANCE_BYTES = 2L * 1024L * 1024L

    fun estimatedBytes(recordedVideoBytes: Long, includeMedia: Boolean): Long =
        DATABASE_ALLOWANCE_BYTES + if (includeMedia) recordedVideoBytes.coerceAtLeast(0L) else 0L

    fun humanReadable(bytes: Long): String = when {
        bytes >= 1024L * 1024L * 1024L -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
