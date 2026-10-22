package br.com.yaakovbarabraham.ats

import br.com.yaakovbarabraham.ats.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.minutes

/**
 * Motor do alarme. tick() é chamado em loop (a cada 1s) por:
 *  - Desktop: job do Quartz (thread do Quartz)
 *  - Android: ScheduledExecutorService dentro de um Foreground Service
 *
 * Regra de sobreposição: se um alarme já está tocando e outro vence,
 * o atual é parado e o novo começa.
 */
class AlarmEngine(private val repo: TaskRepository, private val player: AudioPlayer) {
    private val _ringing = MutableStateFlow<Task?>(null)
    val ringing: StateFlow<Task?> = _ringing

    /** Hook opcional (Android usa para abrir a tela por cima de outros apps). */
    @Volatile var onRing: ((Task) -> Unit)? = null

    @Synchronized
    fun tick(now: LocalDateTime = nowLocal()) {
        val tz = TimeZone.currentSystemDefault()
        val due = repo.data.value.tasks.filter { it.enabled && it.dateTime <= now }.sortedBy { it.dateTime }
        for (t in due) {
            val stale = now.toInstant(tz) - t.dateTime.toInstant(tz) > 5.minutes // perdido enquanto o app estava fechado
            repo.update { d -> d.copy(tasks = d.tasks.map { if (it.id == t.id) next(it, now) else it }) }
            if (!stale) ring(t)
        }
    }

    private fun next(t: Task, now: LocalDateTime): Task {
        if (!t.repeat) return t.copy(enabled = false)
        var dt = t.dateTime
        while (dt <= now || !dt.date.matches(t.weekDays)) dt = dt.date.plus(1, DateTimeUnit.DAY).atTime(dt.time)
        return t.copy(dateTime = dt)
    }

    private fun ring(t: Task) {
        player.stop()                 // para o alarme anterior
        _ringing.value = t
        t.songUri?.let { player.play(it) }
        onRing?.invoke(t)
    }

    @Synchronized
    fun dismiss() {
        player.stop()
        _ringing.value = null
    }

    /** Botão "Parar": silencia (se estiver tocando) e desativa o alarme da tarefa. */
    fun stopTask(id: String) {
        if (_ringing.value?.id == id) dismiss()
        repo.update { d -> d.copy(tasks = d.tasks.map { if (it.id == id) it.copy(enabled = false) else it }) }
    }
}
