package br.com.yaakovbarabraham.ats

import android.R
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** Foreground service: mantém o loop (thread) que verifica as tarefas a cada segundo. */
class AlarmService : Service() {
    private var executor: ScheduledExecutorService? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("tick", "Serviço de alarmes", NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel("alarm", "Alarmes", NotificationManager.IMPORTANCE_HIGH).apply { setSound(null, null) })
        }

        val n = notificationBuilder("tick")
            .setSmallIcon(R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Task Alarm ativo").setContentText("Monitorando suas tarefas").build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)

        if (executor == null) {
            val engine = (application as TaskAlarmApp).container.engine
            engine.onRing = { showAlarm(it) }
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "taskalarm:tick").apply { acquire() }
            executor = Executors.newSingleThreadScheduledExecutor().also {
                it.scheduleWithFixedDelay({ runCatching { engine.tick() } }, 0, 1, TimeUnit.SECONDS)
            }
        }
        return START_STICKY
    }

    private fun showAlarm(task: Task) {
        val intent = Intent(this, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = notificationBuilder("alarm")
            .setSmallIcon(R.drawable.ic_lock_idle_alarm)
            .setContentTitle(task.title).setContentText(task.description)
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_MAX) // usado só abaixo do API 26 (sem canais)
            .setFullScreenIntent(pi, true).setAutoCancel(true).build()
        getSystemService(NotificationManager::class.java).notify(2, n)
        runCatching { startActivity(intent) } // funciona direto com a permissão "sobrepor outros apps"
    }

    /** Canais de notificação só existem a partir do API 26. */
    @Suppress("DEPRECATION")
    private fun notificationBuilder(channelId: String): Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, channelId)
        else Notification.Builder(this)

    override fun onDestroy() {
        executor?.shutdownNow(); executor = null
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}

