package br.com.yaakovbarabraham.ats

import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue


/** Tela modal do alarme: aparece sobre outros apps e sobre a tela de bloqueio. */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        val c = (application as TaskAlarmApp).container
        setContent {
            val task by c.engine.ringing.collectAsState()
            val t = task
            LaunchedEffect(t == null) { if (t == null) finish() }
            if (t != null) AlarmScreen(t, c.songName(t.songUri)) {
                c.engine.dismiss()
                getSystemService(NotificationManager::class.java).cancel(2)
                finish()
            }
        }
    }
}
