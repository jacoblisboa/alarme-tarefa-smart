package br.com.yaakovbarabraham.ats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlinx.datetime.*

enum class Screen { Home, Music }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(c: AppContainer) {
    MaterialTheme {
        var screen by remember { mutableStateOf(Screen.Home) }
        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        ModalNavigationDrawer(
            drawerState = drawer,
            drawerContent = {
                ModalDrawerSheet {
                    Text("Task Alarm", Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                    NavigationDrawerItem(
                        label = { Text("Tarefas") }, selected = screen == Screen.Home,
                        onClick = { screen = Screen.Home; scope.launch { drawer.close() } })
                    NavigationDrawerItem(
                        label = { Text("Arquivos de música") }, selected = screen == Screen.Music,
                        onClick = { screen = Screen.Music; scope.launch { drawer.close() } })
                }
            }
        ) {
            Scaffold(topBar = {
                TopAppBar(
                    title = { Text(if (screen == Screen.Home) "Tarefas" else "Músicas") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Default.Menu, "Menu") }
                    })
            }) { pad ->
                Box(Modifier.padding(pad).fillMaxSize()) {
                    when (screen) {
                        Screen.Home -> HomeScreen(c)
                        Screen.Music -> MusicScreen(c)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(c: AppContainer) {
    val data by c.repo.data.collectAsState()
    var selectedId by remember { mutableStateOf<String?>(null) }
    var dialogOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    val selected = data.tasks.find { it.id == selectedId }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { editing = null; dialogOpen = true }) {
            Icon(Icons.Default.Add, "Adicionar"); Spacer(Modifier.width(6.dp)); Text("Nova tarefa")
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (c.platform.isMobile) TaskCards(data.tasks, selectedId) { selectedId = it }
            else TaskTable(data.tasks, selectedId) { selectedId = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = selected != null, onClick = { editing = selected; dialogOpen = true }) {
                Icon(Icons.Default.Edit, null); Spacer(Modifier.width(4.dp)); Text("Editar")
            }
            OutlinedButton(enabled = selected != null, onClick = { selected?.let { c.engine.stopTask(it.id) } }) {
                Icon(Icons.Default.Stop, null); Spacer(Modifier.width(4.dp)); Text("Parar")
            }
            OutlinedButton(enabled = selected != null, onClick = {
                selected?.let { t ->
                    if (c.engine.ringing.value?.id == t.id) c.engine.dismiss()
                    c.repo.update { d -> d.copy(tasks = d.tasks.filterNot { it.id == t.id }) }
                    selectedId = null
                }
            }) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("Excluir") }
        }
        Text("Músicas de toque", style = MaterialTheme.typography.titleMedium)
        SongList(data.songs, Modifier.heightIn(max = 140.dp))
    }

    if (dialogOpen) {
        TaskDialog(editing, data.songs, onDismiss = { dialogOpen = false }) { t ->
            c.repo.update { d ->
                d.copy(tasks = if (d.tasks.any { it.id == t.id }) d.tasks.map { if (it.id == t.id) t else it } else d.tasks + t)
            }
            selectedId = t.id
            dialogOpen = false
        }
    }
}

@Composable
fun SongList(songs: List<Song>, modifier: Modifier = Modifier) {
    if (songs.isEmpty()) Text("Nenhuma música adicionada.", modifier)
    else LazyColumn(modifier) { items(songs) { Text("♪ ${it.name}", Modifier.padding(vertical = 2.dp)) } }
}

@Composable
private fun RowScope.Cell(text: String, weight: Float, bold: Boolean = false) =
    Text(text, Modifier.weight(weight).padding(horizontal = 6.dp),
        fontWeight = if (bold) FontWeight.Bold else null, maxLines = 2, overflow = TextOverflow.Ellipsis)

/** Tabela para Desktop. */
@Composable
fun TaskTable(tasks: List<Task>, selectedId: String?, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 8.dp)) {
            Cell("Nome Tarefa", 2f, true); Cell("Alarme Ativado", 1f, true); Cell("Repetir", 1f, true)
            Cell("Data e Hora", 1.5f, true); Cell("Descrição", 3f, true)
        }

        LazyColumn {
            items(tasks, key = { it.id }) { t ->
                val bg = if (t.id == selectedId) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                Row(Modifier.fillMaxWidth().background(bg).clickable { onSelect(t.id) }.padding(vertical = 8.dp)) {
                    Cell(t.title, 2f); Cell(if (t.enabled) "Sim" else "Não", 1f); Cell(if (t.repeat) "Sim" else "Não", 1f)
                    Cell(t.dateTime.display(), 1.5f); Cell(t.description, 3f)
                }
                HorizontalDivider()
            }
        }
    }
}

/** "Tabela" própria para celular: um cartão por tarefa. */
@Composable
fun TaskCards(tasks: List<Task>, selectedId: String?, onSelect: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tasks, key = { it.id }) { t ->
            val sel = t.id == selectedId
            Card(
                Modifier.fillMaxWidth().clickable { onSelect(t.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(t.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Data e Hora: ${t.dateTime.display()}")
                    Text("Alarme: ${if (t.enabled) "Ativado" else "Desativado"}  •  Repetir: ${if (t.repeat) "Sim" else "Não"}")
                    if (t.description.isNotBlank()) Text(t.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun MusicScreen(c: AppContainer) {
    val data by c.repo.data.collectAsState()
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = {
            c.platform.pickMusicFolder { found ->
                c.repo.update { d -> d.copy(songs = (d.songs + found).distinctBy { it.uri }) }
            }
        }) { Text("+ adicionar nova música") }
        Text("Formatos: mp3, aac, ogg, flac, alac", style = MaterialTheme.typography.bodySmall)
        SongList(data.songs, Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(initial: Task?, songs: List<Song>, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    val base = initial?.dateTime ?: nowLocal()
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var date by remember { mutableStateOf(base.date) }
    var time by remember { mutableStateOf(LocalTime(base.hour, base.minute)) }
    var songUri by remember { mutableStateOf(initial?.songUri) }
    var repeat by remember { mutableStateOf(initial?.repeat ?: false) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var days by remember { mutableStateOf(initial?.weekDays?.toSet() ?: emptySet<Int>()) }
    var daysMenu by remember { mutableStateOf(false) }

    var menu by remember { mutableStateOf(false) }

    fun two(n: Int) = n.toString().padStart(2, '0')

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova tarefa" else "Editar tarefa") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(),
                    label = { Text("Título da tarefa") }, singleLine = true)
                OutlinedTextField(desc, { desc = it }, Modifier.fillMaxWidth(),
                    label = { Text("Descrição da tarefa") }, minLines = 2)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ showDate = true }, Modifier.weight(1f)) {
                        Text("${two(date.dayOfMonth)}/${two(date.monthNumber)}/${date.year}")
                    }
                    OutlinedButton({ showTime = true }, Modifier.weight(1f)) { Text("${two(time.hour)}:${two(time.minute)}") }
                }
                ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
                    OutlinedTextField(
                        value = songs.find { it.uri == songUri }?.name ?: "Selecione a música",
                        onValueChange = {}, readOnly = true, label = { Text("Música") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        songs.forEach { s ->
                            DropdownMenuItem(text = { Text(s.name) }, onClick = { songUri = s.uri; menu = false })
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(repeat, { repeat = it }); Text("Repetir nesse horário")
                }
                if (repeat) {
                    ExposedDropdownMenuBox(expanded = daysMenu, onExpandedChange = { daysMenu = it }) {
                        OutlinedTextField(
                            value = weekDaysLabel(days), onValueChange = {}, readOnly = true,
                            label = { Text("Dias da semana") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(daysMenu) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                        ExposedDropdownMenu(expanded = daysMenu, onDismissRequest = { daysMenu = false }) {
                            WEEK_DAYS.forEach { (n, name) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(n in days, null)
                                            Spacer(Modifier.width(8.dp)); Text(name)
                                        }
                                    },
                                    onClick = { days = if (n in days) days - n else days + n } // não fecha o menu
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                val dayList = if (repeat) days.sorted() else emptyList()
                onSave(Task(
                    id = initial?.id ?: newId(), title = title.trim(), description = desc.trim(),
                    dateTime = if (dayList.isEmpty()) date.atTime(time) else firstOccurrenceFromNow(time, dayList),
                    songUri = songUri, repeat = repeat, enabled = true, weekDays = dayList))
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )

    if (showDate) {
        val st = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton({
                    st.selectedDateMillis?.let { date = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }
                }) { Text("OK") }
            }) { DatePicker(st) }
    }
    if (showTime) {
        val st = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)

        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = { TextButton({ time = LocalTime(st.hour, st.minute); showTime = false }) { Text("OK") } },
            text = { TimePicker(st) })
    }
}

/** Conteúdo do diálogo de alarme (janela always-on-top no Desktop, Activity sobre outros apps no Android). */
@Composable
fun AlarmScreen(task: Task, songName: String?, onStop: () -> Unit) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Alarm, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                Text(task.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                if (task.description.isNotBlank())
                    Text(task.description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                if (songName != null) Text("♪ $songName", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onStop) { Icon(Icons.Default.Stop, null); Spacer(Modifier.width(6.dp)); Text("Parar alarme") }
            }
        }
    }
}
