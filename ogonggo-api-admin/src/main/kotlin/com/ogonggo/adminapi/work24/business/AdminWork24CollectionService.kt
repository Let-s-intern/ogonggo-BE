package com.ogonggo.adminapi.work24.business

import com.ogonggo.core.work24.implement.Work24CollectionTarget
import com.ogonggo.core.work24.implement.Work24Collector
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

/**
 * 고용24 채용정보는 채용공고로, 훈련과정은 부트캠프로 새 항목만 등록한다.
 *
 * 트랜잭션을 열지 않는다. 수집이 수십 분 걸릴 수 있어 커넥션을 잡고 있으면 안 되고,
 * 항목마다 반영되어야 중간에 실패해도 등록한 만큼은 남는다.
 */
@Service
class AdminWork24CollectionService(
    private val work24Collector: Work24Collector,
    private val clock: Clock,
) {

    /**
     * 한 대상이 실패해도 나머지는 계속 받는다. 인증키가 없거나 부트캠프 대표 이미지가 없는 대상은 호출하지 않고 건너뛴다.
     * 실패한 대상이 있어도 예외를 던지지 않고 결과에 담는다.
     */
    fun collectAll(): List<AdminWork24CollectResult> {
        val now = LocalDateTime.now(clock)
        return Work24CollectionTarget.entries.map { target -> collect(target, now) }
    }

    private fun collect(target: Work24CollectionTarget, now: LocalDateTime): AdminWork24CollectResult {
        if (!work24Collector.isReady(target)) {
            log.info("고용24 인증키나 부트캠프 대표 이미지가 설정되지 않아 수집을 건너뜁니다. target={}", target)
            return AdminWork24CollectResult.Skipped(target)
        }

        return try {
            val result = work24Collector.collect(target, now)
            log.info(
                "고용24 수집 완료. target={}, pages={}, appended={}, skipped={}, failed={}",
                target,
                result.pageCount,
                result.appendedCount,
                result.skippedCount,
                result.failedCount,
            )
            AdminWork24CollectResult.Collected(result)
        } catch (exception: Exception) {
            log.error("고용24 수집 실패. target={}", target, exception)
            AdminWork24CollectResult.Failed(target)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AdminWork24CollectionService::class.java)
    }
}
