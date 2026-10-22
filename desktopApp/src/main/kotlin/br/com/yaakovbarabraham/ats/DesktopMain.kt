package br.com.yaakovbarabraham.ats

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    val container = AppContainer(DesktopPlatform())
    QuartzTicker.start(container.engine)

    application {
        Window(
            onCloseRequest = { QuartzTicker.stop(); exitApplication() },
            title = "Task Alarm",
            state = rememberWindowState(width = 1100.dp, height = 750.dp)
        ) { App(container) }

        // Diálogo do alarme: janela sempre no topo, sobrepondo os outros aplicativos
        val ringing by container.engine.ringing.collectAsState()
        ringing?.let { t ->
            Window(
                onCloseRequest = container.engine::dismiss,
                title = "Alarme",
                alwaysOnTop = true,
                resizable = false,
                state = rememberWindowState(width = 460.dp, height = 360.dp, position = WindowPosition(Alignment.Center))
            ) { AlarmScreen(t, container.songName(t.songUri), container.engine::dismiss) }
        }
    }
}
