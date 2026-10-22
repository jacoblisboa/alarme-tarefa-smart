package br.com.yaakovbarabraham.ats

import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

/** Toca mp3/aac/ogg/flac/alac usando o VLC instalado no sistema (via vlcj). */
class VlcAudioPlayer : AudioPlayer {
    private val factory by lazy { MediaPlayerFactory() }
    private var player: MediaPlayer? = null

    @Synchronized
    override fun play(uri: String) {
        stop()
        runCatching {
            player = factory.mediaPlayers().newMediaPlayer().also {
                it.controls().setRepeat(true)
                it.media().play(uri)
            }
        }.onFailure { println("Falha ao tocar áudio (VLC instalado?): ${it.message}") }
    }

    @Synchronized
    override fun stop() {
        val p = player ?: return
        player = null
        Thread { runCatching { p.controls().stop(); p.release() } }.start() // evita deadlock no vlcj
    }
}

class DesktopPlatform : Platform {
    override val isMobile = false
    override val player: AudioPlayer = VlcAudioPlayer()
    private val dir = File(System.getProperty("user.home"), ".taskalarm").apply { mkdirs() }
    private val file = File(dir, "data.json")

    override fun loadText() = if (file.exists()) file.readText() else null
    override fun saveText(text: String) = file.writeText(text)

    override fun pickMusicFolder(onResult: (List<Song>) -> Unit) {
        SwingUtilities.invokeLater {
            val fc = JFileChooser(File(System.getProperty("user.home"), "Downloads")).apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Selecione a pasta com as músicas"
            }
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val songs = fc.selectedFile.listFiles()
                    ?.filter { it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS }
                    ?.sortedBy { it.name }
                    ?.map { Song(it.name, it.absolutePath) }
                    .orEmpty()
                onResult(songs)
            }
        }
    }
}
