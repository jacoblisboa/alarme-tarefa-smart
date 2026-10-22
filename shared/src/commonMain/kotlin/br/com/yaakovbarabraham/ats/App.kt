package br.com.yaakovbarabraham.ats
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
@Composable
fun App() {
    MaterialTheme {
        var mostrarModal by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { mostrarModal = true }) {
                Text("Abrir janela modal")
            }
        }

        if (mostrarModal) {
            AlertDialog(
                onDismissRequest = { mostrarModal = false },
                title = { Text("Janela modal") },
                text = { Text("Olá, Yisrael! Esta janela modal roda no Android e no Desktop.") },
                confirmButton = {
                    TextButton(onClick = { mostrarModal = false }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarModal = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}