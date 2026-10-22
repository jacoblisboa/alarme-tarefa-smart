package br.com.yaakovbarabraham.ats

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile


class MainActivity : ComponentActivity() {
    private var pending: ((List<Song>) -> Unit)? = null

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pending?.invoke(listSongs(uri))
        }
    }
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TaskAlarmApp

        app.platform.folderLauncher = { callback ->
            pending = callback
            // abre o seletor já na pasta Downloads
            folderPicker.launch(DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download"))
        }

        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        if (!Settings.canDrawOverlays(this)) { // permite abrir o alarme sobre outros apps
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        ContextCompat.startForegroundService(this, Intent(this, AlarmService::class.java))

        setContent { App(app.container) }
    }

    private fun listSongs(tree: Uri): List<Song> =
        DocumentFile.fromTreeUri(this, tree)?.listFiles()
            ?.filter { it.isFile && (it.name ?: "").substringAfterLast('.').lowercase() in AUDIO_EXTENSIONS }
            ?.sortedBy { it.name }
            ?.map { Song(it.name ?: "?", it.uri.toString()) }
            .orEmpty()
}
