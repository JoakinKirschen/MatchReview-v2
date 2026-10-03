package be.matchreview.app.domain

/** A backup is due once a match has finished after the last successful backup. */
object BackupReminderRules {
    fun isDue(lastBackupEpochMs: Long?, lastFinishedMatchEpochMs: Long?): Boolean =
        lastFinishedMatchEpochMs != null &&
            (lastBackupEpochMs == null || lastBackupEpochMs < lastFinishedMatchEpochMs)
}
