package org.ktugrades.backend.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.*
import kotlin.concurrent.schedule

class SchedulerService {

    private val logger: Logger = LoggerFactory.getLogger("scheduler")

    fun runEveryInterval(minutes: Int, block: () -> Unit) {
        val timer = Timer()
        fun doRun() {
            val millis = (minutes * 60000).toLong()
            timer.schedule(millis) {
                try {
                    logger.info("Running scheduled task.")
                    block()
                } catch (e: Exception) {
                    logger.error("An error has occurred while running scheduled task", e)
                }
                doRun()
            }
        }
        doRun()
    }
}