package br.com.yaakovbarabraham.ats

import android.app.Application
import com.taskalarm.AndroidPlatform

class TaskAlarmApp : Application() {
    lateinit var platform: AndroidPlatform
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        platform = AndroidPlatform(this)
        container = AppContainer(platform)
    }
}
