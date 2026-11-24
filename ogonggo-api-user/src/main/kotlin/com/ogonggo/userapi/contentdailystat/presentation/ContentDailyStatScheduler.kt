package com.ogonggo.userapi.contentdailystat.presentation

import com.ogonggo.userapi.contentdailystat.business.ContentDailyStatService
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ContentDailyStatScheduler(
    private val contentDailyStatService: ContentDailyStatService,
    private val schedulerExecutionObserver: SchedulerExecutionObserver,
) {

    /**
     * 마지막 집계일 다음 날부터 어제까지의 콘텐츠 이벤트를 하루씩 일별 집계로 모은 뒤 보관 기간(365일)이 지난 원본을 지운다.
     * 실행 주기와 켜짐 여부는 `scheduled_jobs`가 정한다. `UserScheduledJobConfiguration` 참고.
     */
    @SchedulerLock(name = SCHEDULER_NAME, lockAtLeastFor = "PT1M", lockAtMostFor = "PT30M")
    fun aggregate() {
        schedulerExecutionObserver.observe(SCHEDULER_NAME) {
            val statDates = contentDailyStatService.datesToAggregate()
            val rowCount = statDates.sumOf { contentDailyStatService.aggregate(it) }
            val removedCount = contentDailyStatService.removeExpiredEvents(REMOVE_BATCH_SIZE)
            log.info(
                "콘텐츠 일별 집계 완료. statDates={}~{}, dayCount={}, rowCount={}, removedEventCount={}",
                statDates.first(), statDates.last(), statDates.size, rowCount, removedCount,
            )
        }
    }

    companion object {
        const val SCHEDULER_NAME = "contentDailyStatAggregation"
        private const val REMOVE_BATCH_SIZE = 1000
        private val log = LoggerFactory.getLogger(ContentDailyStatScheduler::class.java)
    }
}
