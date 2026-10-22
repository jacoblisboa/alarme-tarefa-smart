package br.com.yaakovbarabraham.ats

import kotlinx.datetime.*
import kotlin.time.Clock
import kotlinx.serialization.Serializable

@Serializable
data class Song(val name: String, val uri: String)

@Serializable
data class Task(
    val id: String,
    val title: String,
    val description: String,
    val dateTime: LocalDateTime,
    val songUri: String? = null,
    val repeat: Boolean = false,   // repete no mesmo horário
    val enabled: Boolean = true,   // "Alarme Ativado"
    val weekDays: List<Int> = emptyList() // dias ISO (1=seg..7=dom) da repetição; vazio = todos os dias
)

@Serializable
data class AppData(val tasks: List<Task> = emptyList(), val songs: List<Song> = emptyList())

val AUDIO_EXTENSIONS = setOf("mp3", "aac", "m4a", "ogg", "flac", "alac")

fun nowLocal(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
fun newId(): String = Clock.System.now().toEpochMilliseconds().toString() + (0..999).random()

private fun p(n: Int) = n.toString().padStart(2, '0')
fun LocalDateTime.display() = "${p(dayOfMonth)}/${p(monthNumber)}/$year ${p(hour)}:${p(minute)}"

val WEEK_DAYS = listOf(
    1 to "Segunda", 2 to "Terça", 3 to "Quarta", 4 to "Quinta",
    5 to "Sexta", 6 to "Sábado", 7 to "Domingo"
)

fun weekDaysLabel(days: Set<Int>): String =
    if (days.isEmpty() || days.size == 7) "Todos os dias"
    else WEEK_DAYS.filter { it.first in days }.joinToString(", ") { it.second.take(3) }

fun LocalDate.matches(days: Collection<Int>) = days.isEmpty() || dayOfWeek.isoDayNumber in days

/** Primeira data/hora a partir de [dt] que cai em um dos [days] (vazio = qualquer dia). */
fun firstOccurrence(dt: LocalDateTime, days: Collection<Int>): LocalDateTime {
    var d = dt.date
    repeat(7) {
        if (d.matches(days)) return d.atTime(dt.time)
        d = d.plus(1, DateTimeUnit.DAY)
    }
    return dt
}

/** Próxima data/hora futura com o horário [time] que cai em um dos [days] (vazio = qualquer dia). */
fun firstOccurrenceFromNow(time: LocalTime, days: Collection<Int>): LocalDateTime {
    val now = nowLocal()
    var d = now.date
    repeat(8) {
        val candidate = d.atTime(time)
        if (d.matches(days) && candidate > now) return candidate
        d = d.plus(1, DateTimeUnit.DAY)
    }
    return now.date.atTime(time)
}
