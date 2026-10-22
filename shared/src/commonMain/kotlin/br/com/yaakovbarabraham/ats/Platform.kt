package br.com.yaakovbarabraham.ats

/** Toca o áudio em loop até stop(). Implementado por plataforma. */
interface AudioPlayer {
    fun play(uri: String)
    fun stop()
}

interface Platform {
    val isMobile: Boolean
    val player: AudioPlayer
    fun loadText(): String?
    fun saveText(text: String)
    /** Abre o seletor de pasta (Downloads) e devolve as músicas encontradas. */
    fun pickMusicFolder(onResult: (List<Song>) -> Unit)
}