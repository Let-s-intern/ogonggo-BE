package com.ogonggo.userapi.image.presentation

import com.ogonggo.userapi.image.business.ImageAssetCleanupService
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ImageAssetCleanupScheduler(
    private val imageAssetCleanupService: ImageAssetCleanupService,
    private val schedulerExecutionObserver: SchedulerExecutionObserver,
) {

    /** 실행 주기와 켜짐 여부는 `scheduled_jobs`가 정한다. `UserScheduledJobConfiguration` 참고. */
    @SchedulerLock(
        name = SCHEDULER_NAME,
        lockAtLeastFor = "\${ogonggo.storage.s3.cleanup.lock-at-least-for:PT55M}",
        lockAtMostFor = "\${ogonggo.storage.s3.cleanup.lock-at-most-for:PT2H}",
    )
    fun cleanup() {
        val deletedCount = schedulerExecutionObserver.observe(SCHEDULER_NAME) {
            imageAssetCleanupService.cleanup()
        }
        log.info("고아 이미지 정리 완료. deletedCount={}", deletedCount)
    }

    companion object {
        const val SCHEDULER_NAME = "imageAssetCleanup"
        private val log = LoggerFactory.getLogger(ImageAssetCleanupScheduler::class.java)
    }
}
