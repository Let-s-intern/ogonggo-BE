package com.ogonggo.userapi.scheduling

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SchedulerExecutionObserver(
    private val meterRegistry: MeterRegistry,
) {

    fun <T> observe(scheduler: String, task: () -> T): T {
        val sample = Timer.start(meterRegistry)
        var result = SUCCESS

        try {
            val value = task()
            recordExecution(scheduler, SUCCESS)
            return value
        } catch (exception: Exception) {
            result = FAILURE
            recordExecution(scheduler, FAILURE)
            log.error("스케줄러 실행 실패. scheduler={}", scheduler, exception)
            throw exception
        } finally {
            sample.stop(
                Timer.builder(DURATION_METRIC)
                    .tag(SCHEDULER_TAG, scheduler)
                    .tag(RESULT_TAG, result)
                    .register(meterRegistry),
            )
        }
    }

    private fun recordExecution(scheduler: String, result: String) {
        meterRegistry.counter(
            EXECUTION_METRIC,
            SCHEDULER_TAG,
            scheduler,
            RESULT_TAG,
            result,
        ).increment()
    }

    private companion object {
        const val EXECUTION_METRIC = "ogonggo.scheduler.executions"
        const val DURATION_METRIC = "ogonggo.scheduler.duration"
        const val SCHEDULER_TAG = "scheduler"
        const val RESULT_TAG = "result"
        const val SUCCESS = "success"
        const val FAILURE = "failure"

        val log = LoggerFactory.getLogger(SchedulerExecutionObserver::class.java)
    }
}
