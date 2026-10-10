package com.ogonggo.userapi.letscareercontent.presentation

import com.ogonggo.userapi.letscareercontent.business.LetsCareerContentSyncService
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** 공고 상세 추천에 쓰는 렛츠커리어 콘텐츠 사본을 주기적으로 렛츠커리어 목록과 맞춘다. */
@Component
class LetsCareerContentSyncScheduler(
    private val letsCareerContentSyncService: LetsCareerContentSyncService,
    private val schedulerExecutionObserver: SchedulerExecutionObserver,
) {

    /** 실행 주기와 켜짐 여부는 `scheduled_jobs`가 정한다. `UserScheduledJobConfiguration` 참고. */
    @SchedulerLock(name = SCHEDULER_NAME, lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    fun sync() {
        val result = schedulerExecutionObserver.observe(SCHEDULER_NAME) {
            letsCareerContentSyncService.sync()
        } ?: return
        log.info(
            "렛츠커리어 콘텐츠 사본을 맞췄습니다. created={}, updated={}, deleted={}",
            result.created,
            result.updated,
            result.deleted,
        )
    }

    companion object {
        const val SCHEDULER_NAME = "letsCareerContentSync"
        private val log = LoggerFactory.getLogger(LetsCareerContentSyncScheduler::class.java)
    }
}
