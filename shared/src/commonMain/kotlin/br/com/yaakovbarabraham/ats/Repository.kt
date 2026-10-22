package br.com.yaakovbarabraham.ats

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

class TaskRepository(private val platform: Platform) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _data = MutableStateFlow(
        runCatching { platform.loadText()?.let { json.decodeFromString<AppData>(it) } }.getOrNull() ?: AppData()
    )
    val data: StateFlow<AppData> = _data

    @Synchronized
    fun update(transform: (AppData) -> AppData) {
        _data.value = transform(_data.value)
        runCatching { platform.saveText(json.encodeToString(_data.value)) }
    }
}

class AppContainer(val platform: Platform) {
    val repo = TaskRepository(platform)
    val engine = AlarmEngine(repo, platform.player)
    fun songName(uri: String?): String? = repo.data.value.songs.find { it.uri == uri }?.name
}
