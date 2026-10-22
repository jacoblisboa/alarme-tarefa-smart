package com.taskalarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import br.com.yaakovbarabraham.ats.AudioPlayer
import br.com.yaakovbarabraham.ats.Platform
import br.com.yaakovbarabraham.ats.Song
import java.io.File

class AndroidPlayer(private val ctx: Context) : AudioPlayer {
    private var mp: MediaPlayer? = null

    @Synchronized
    override fun play(uri: String) {
        stop()
        runCatching {
            mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                setDataSource(ctx, Uri.parse(uri))
                isLooping = true
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        }
    }

    @Synchronized
    override fun stop() {
        mp?.let { runCatching { it.stop() }; it.release() }
        mp = null
    }
}

class AndroidPlatform(ctx: Context) : Platform {
    override val isMobile = true
    override val player: AudioPlayer = AndroidPlayer(ctx.applicationContext)
    private val file = File(ctx.filesDir, "data.json")

    /** Definido pela MainActivity (que registra o seletor de pasta). */
    var folderLauncher: (((List<Song>) -> Unit) -> Unit)? = null

    override fun loadText() = if (file.exists()) file.readText() else null
    override fun saveText(text: String) = file.writeText(text)
    override fun pickMusicFolder(onResult: (List<Song>) -> Unit) { folderLauncher?.invoke(onResult) }
}
