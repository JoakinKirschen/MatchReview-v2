package be.matchreview.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream

/** Hands files to other apps: open them in a viewer or share them. */
object ExternalApps {
    /** Opens [uri] in a viewer app. Returns false when no installed app can show it. */
    fun open(context: Context, uri: Uri, mimeType: String): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (notFound: ActivityNotFoundException) {
        false
    }

    /**
     * Writes a file into the app's share cache and opens Android's share sheet for it, so it
     * can go to WhatsApp, e-mail or Drive without first being saved somewhere.
     */
    fun shareNewFile(
        context: Context,
        fileName: String,
        mimeType: String,
        title: String,
        write: (OutputStream) -> Unit
    ) {
        val directory = File(context.cacheDir, "shared").apply { mkdirs() }
        // Only the latest shared file is kept; older ones are no longer needed.
        directory.listFiles()?.forEach { it.delete() }
        val file = File(directory, fileName)
        file.outputStream().use(write)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(
            Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
