package com.ogonggo.core.schedule.implement

import com.ogonggo.core.schedule.implement.dto.ScheduledJobDefinition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture

/**
 * 각 API가 등록한 [ScheduledJobDefinition]을 DB(`scheduled_jobs`)의 cron으로 예약한다.
 *
 * - 기동할 때 DB에 행이 없는 작업만 기본 cron으로 만든다.
 * - 1분마다 DB를 다시 읽어 cron이 바뀐 작업을 새 cron으로 다시 예약한다. 배포 없이 SQL로 바꾸면 1분 안에 반영된다.
 * - 켜짐 여부는 예약 시각마다 DB에서 읽는다. 꺼져 있으면 그 회차를 건너뛴다.
 * - cron이 잘못되었으면 기존 예약을 유지하고 오류 로그를 남긴다. 기동 시점이면 코드의 기본 cron으로 예약한다.
 * - 행을 지우면 켜진 것으로 보고 코드의 기본 cron을 쓴다. 멈추려면 행을 지우지 말고 `enabled`를 끈다.
 *
 * 태스크가 여럿이면 모든 태스크가 예약하고, 중복 실행은 각 작업의 ShedLock 잠금이 막는다.
 * 작업이 오래 걸려도 다른 작업을 막지 않도록 Spring 기본 스케줄러(스레드 1개) 대신 전용 스레드 풀을 쓴다.
 */
@Component
class ScheduledJobRegistrar(
    definitions: ObjectProvider<ScheduledJobDefinition>,
    private val scheduledJobReader: ScheduledJobReader,
    private val scheduledJobAppender: ScheduledJobAppender,
) : DisposableBean {

    private val definitions: List<ScheduledJobDefinition> = definitions.orderedStream().toList()
    private val schedules = ConcurrentHashMap<String, Schedule>()
    private val taskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = POOL_SIZE
        setThreadNamePrefix(THREAD_NAME_PREFIX)
        initialize()
    }

    init {
        val duplicated = this.definitions.groupBy { it.name }.filterValues { it.size > 1 }.keys
        require(duplicated.isEmpty()) { "스케줄 작업 이름이 겹칩니다: $duplicated" }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        definitions.forEach { definition ->
            scheduledJobAppender.appendIfAbsent(definition)
            reschedule(definition)
        }
        taskScheduler.scheduleWithFixedDelay(::refresh, REFRESH_INTERVAL)
    }

    /** DB의 cron이 바뀐 작업을 다시 예약한다. */
    fun refresh() {
        definitions.forEach { definition ->
            try {
                reschedule(definition)
            } catch (exception: Exception) {
                log.error("스케줄 작업 설정을 읽지 못했습니다. job={}", definition.name, exception)
            }
        }
    }

    /** 지금 예약에 쓰는 cron이다. 예약되지 않았으면 null이다. */
    fun scheduledCron(name: String): String? = schedules[name]?.appliedCron

    override fun destroy() {
        taskScheduler.shutdown()
    }

    private fun reschedule(definition: ScheduledJobDefinition) {
        val cron = scheduledJobReader.read(definition.name)?.cron ?: definition.defaultCron
        val current = schedules[definition.name]
        if (current?.requestedCron == cron) {
            return
        }

        val trigger = cronTrigger(cron)
        if (trigger == null) {
            log.error("스케줄 작업의 cron이 올바르지 않습니다. job={}, cron={}", definition.name, cron)
            if (current != null) {
                // 같은 값으로 매분 오류를 남기지 않도록 본 값만 기록하고 기존 예약을 유지한다.
                schedules[definition.name] = current.copy(requestedCron = cron)
                return
            }
        }

        val appliedCron = if (trigger != null) cron else definition.defaultCron
        current?.future?.cancel(false)
        val future = taskScheduler.schedule(
            { run(definition) },
            trigger ?: CronTrigger(definition.defaultCron, ZONE),
        )
        schedules[definition.name] = Schedule(requestedCron = cron, appliedCron = appliedCron, future = future)
        log.info("스케줄 작업을 예약했습니다. job={}, cron={}", definition.name, appliedCron)
    }

    private fun run(definition: ScheduledJobDefinition) {
        try {
            if (scheduledJobReader.read(definition.name)?.enabled == false) {
                log.info("꺼진 스케줄 작업이라 건너뜁니다. job={}", definition.name)
                return
            }
            definition.action()
        } catch (exception: Exception) {
            log.error("스케줄 작업 실행에 실패했습니다. job={}", definition.name, exception)
        }
    }

    private data class Schedule(
        val requestedCron: String,
        val appliedCron: String,
        val future: ScheduledFuture<*>?,
    )

    companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private val REFRESH_INTERVAL: Duration = Duration.ofMinutes(1)
        private const val POOL_SIZE = 4
        private const val THREAD_NAME_PREFIX = "scheduled-job-"
        private val log = LoggerFactory.getLogger(ScheduledJobRegistrar::class.java)

        private fun cronTrigger(cron: String): CronTrigger? =
            try {
                CronTrigger(cron, ZONE)
            } catch (exception: IllegalArgumentException) {
                null
            }
    }
}
