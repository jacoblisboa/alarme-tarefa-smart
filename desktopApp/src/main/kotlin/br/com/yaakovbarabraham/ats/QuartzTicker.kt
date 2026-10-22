package br.com.yaakovbarabraham.ats
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.util.Properties

@DisallowConcurrentExecution
class TickJob : Job {
    override fun execute(ctx: JobExecutionContext) {
        (ctx.mergedJobDataMap["engine"] as AlarmEngine).tick()
    }
}

/** Loop do alarme: um job do Quartz executando a cada 1 segundo em thread própria. */
object QuartzTicker {
    private var scheduler: Scheduler? = null

    fun start(engine: AlarmEngine) {
        val props = Properties().apply {
            setProperty("org.quartz.scheduler.skipUpdateCheck", "true")
            setProperty("org.quartz.scheduler.makeSchedulerThreadDaemon", "true")
            setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool")
            setProperty("org.quartz.threadPool.threadCount", "2")
            setProperty("org.quartz.threadPool.makeThreadsDaemons", "true")
        }
        val s = StdSchedulerFactory(props).scheduler
        val job = JobBuilder.newJob(TickJob::class.java).withIdentity("alarm-tick").build()
        job.jobDataMap["engine"] = engine
        val trigger = TriggerBuilder.newTrigger().withIdentity("alarm-trigger").startNow()
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1).repeatForever())
            .build()
        s.scheduleJob(job, trigger)
        s.start()
        scheduler = s
    }

    fun stop() { runCatching { scheduler?.shutdown() } }
}
