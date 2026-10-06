package com.ogonggo.adminapi.ingestion.work24.implement

import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24CollectionResultDto
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

/**
 * 하루 한 번 고용24 채용정보는 채용공고로, 훈련과정은 부트캠프로 새 항목만 등록한다.
 *
 * 기본값은 고용24 호출이 몰리는 낮 시간을 피한 새벽 4시다. 대상마다 수백 번 호출할 수 있어
 * 잠금을 넉넉히 잡고, 태스크가 여럿이어도 한 곳에서만 돌도록 ShedLock으로 막는다.
 *
 * 트랜잭션을 열지 않는다. 수집이 수십 분 걸릴 수 있어 커넥션을 잡고 있으면 안 되고,
 * 항목마다 반영되어야 중간에 실패해도 등록한 만큼은 남는다.
 */
@Component
class Work24CollectionScheduler(
    private val work24Collector: Work24Collector,
    private val clock: Clock,
) {

    /** 실행 주기와 켜짐 여부는 `scheduled_jobs`가 정한다. `AdminScheduledJobConfiguration` 참고. */
    @SchedulerLock(
        name = SCHEDULER_NAME,
        lockAtLeastFor = "\${ogonggo.work24.collection.lock-at-least-for:PT10M}",
        lockAtMostFor = "\${ogonggo.work24.collection.lock-at-most-for:PT3H}",
    )
    fun collect() {
        val results = collectAll()
        log.info(
            "고용24 일일 수집 종료. collected={}, skipped={}, failed={}",
            results.filterIsInstance<Work24CollectionResultDto.Collected>().map { it.target },
            results.filterIsInstance<Work24CollectionResultDto.Skipped>().map { it.target },
            results.filterIsInstance<Work24CollectionResultDto.Failed>().map { it.target },
        )
    }

    /**
     * 한 대상이 실패해도 나머지는 계속 받는다. 인증키가 없는 대상은 호출하지 않고 건너뛴다.
     * 실패한 대상이 있어도 예외를 던지지 않고 결과에 담는다.
     */
    internal fun collectAll(): List<Work24CollectionResultDto> {
        val now = LocalDateTime.now(clock)
        return Work24CollectionTarget.entries.map { target -> collect(target, now) }
    }

    private fun collect(target: Work24CollectionTarget, now: LocalDateTime): Work24CollectionResultDto {
        if (!work24Collector.isReady(target)) {
            log.info("고용24 인증키가 설정되지 않아 수집을 건너뜁니다. target={}", target)
            return Work24CollectionResultDto.Skipped(target)
        }

        return try {
            val result = work24Collector.collect(target, now)
            log.info(
                "고용24 수집 완료. target={}, pages={}, appended={}, skipped={}, excluded={}, failed={}",
                target,
                result.pageCount,
                result.appendedCount,
                result.skippedCount,
                result.excludedCount,
                result.failedCount,
            )
            Work24CollectionResultDto.Collected(result)
        } catch (exception: Exception) {
            log.error("고용24 수집 실패. target={}", target, exception)
            Work24CollectionResultDto.Failed(target)
        }
    }

    companion object {
        const val SCHEDULER_NAME = "work24DailyCollection"
        private val log = LoggerFactory.getLogger(Work24CollectionScheduler::class.java)
    }
}
