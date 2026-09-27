package be.matchreview.app.domain

data class ExportPrivacyOptions(
    val includePlayerNames: Boolean = true,
    val includeNotes: Boolean = true,
    val includeMediaUris: Boolean = false
)

object ExportPrivacyRules {
    fun playerLabel(name: String?, playerId: Long?, options: ExportPrivacyOptions): String =
        when {
            playerId == null -> ""
            options.includePlayerNames -> name.orEmpty()
            else -> "Player ${playerId}"
        }

    fun note(value: String, options: ExportPrivacyOptions): String =
        if (options.includeNotes) value else ""

    fun mediaUri(value: String?, options: ExportPrivacyOptions): String =
        if (options.includeMediaUris) value.orEmpty() else ""
}
