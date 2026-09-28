package com.ogonggo.adminapi.work24.implement

import com.ogonggo.adminapi.work24.business.AdminWork24CollectResult
import com.ogonggo.adminapi.work24.business.AdminWork24CollectionService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 하루 한 번 고용24 채용정보·훈련과정을 받아 채용공고·부트캠프로 등록한다.
 *
 * 기본값은 고용24 호출이 몰리는 낮 시간을 피한 새벽 4시다. 대상마다 수백 번 호출할 수 있어
 * 잠금을 넉넉히 잡고, 태스크가 여럿이어도 한 곳에서만 돌도록 ShedLock으로 막는다.
 */
@Component
class Work24CollectionScheduler(
    private val adminWork24CollectionService: AdminWork24CollectionService,
) {

    /** 실행 주기와 켜짐 여부는 `scheduled_jobs`가 정한다. `AdminScheduledJobConfiguration` 참고. */
    @SchedulerLock(
        name = SCHEDULER_NAME,
        lockAtLeastFor = "\${ogonggo.work24.collection.lock-at-least-for:PT10M}",
        lockAtMostFor = "\${ogonggo.work24.collection.lock-at-most-for:PT3H}",
    )
    fun collect() {
        val results = adminWork24CollectionService.collectAll()
        log.info(
            "고용24 일일 수집 종료. collected={}, skipped={}, failed={}",
            results.filterIsInstance<AdminWork24CollectResult.Collected>().map { it.target },
            results.filterIsInstance<AdminWork24CollectResult.Skipped>().map { it.target },
            results.filterIsInstance<AdminWork24CollectResult.Failed>().map { it.target },
        )
    }

    companion object {
        const val SCHEDULER_NAME = "work24DailyCollection"
        private val log = LoggerFactory.getLogger(Work24CollectionScheduler::class.java)
    }
}
