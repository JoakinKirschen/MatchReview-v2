package be.matchreview.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Android's own file browser for picking an image, opened in the Download folder. It needs
 * no storage permission and also reaches other folders, Drive and SD cards. Devices that do
 * not know the Download location simply open the browser at its default place.
 */
class OpenImageInDownloads : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input)
            .putExtra(DocumentsContract.EXTRA_INITIAL_URI, DOWNLOAD_FOLDER)

    private companion object {
        val DOWNLOAD_FOLDER: Uri =
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload")
    }
}
