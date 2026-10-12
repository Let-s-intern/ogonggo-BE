package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import com.ogonggo.userapi.user.business.LetsCareerNicknameSendService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** 오공고에서 고친 닉네임을 렛츠커리어로 주기적으로 보낸다. */
@Component
class LetsCareerNicknameSyncScheduler(
    private val letsCareerNicknameSendService: LetsCareerNicknameSendService,
    private val schedulerExecutionObserver: SchedulerExecutionObserver,
) {

    /** 실행 주기와 켜짐 여부는 `scheduled_jobs`가 정한다. `UserScheduledJobConfiguration` 참고. */
    @SchedulerLock(name = SCHEDULER_NAME, lockAtLeastFor = "PT20S", lockAtMostFor = "PT5M")
    fun sendPending() {
        val failedCount = schedulerExecutionObserver.observe(SCHEDULER_NAME) {
            letsCareerNicknameSendService.sendPending()
        }
        if (failedCount > 0) {
            log.warn("렛츠커리어로 보내지 못한 닉네임이 있습니다. failedCount={}", failedCount)
        }
    }

    companion object {
        const val SCHEDULER_NAME = "letsCareerNicknameSync"
        private val log = LoggerFactory.getLogger(LetsCareerNicknameSyncScheduler::class.java)
    }
}
