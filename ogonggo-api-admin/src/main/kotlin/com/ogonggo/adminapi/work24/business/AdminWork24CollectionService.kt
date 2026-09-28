package com.ogonggo.adminapi.work24.business

import com.ogonggo.core.work24.implement.Work24CollectionTarget
import com.ogonggo.core.work24.implement.Work24Collector
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

/**
 * 고용24 목록을 모두 받아 처음 보는 항목을 저장한다.
 *
 * 트랜잭션을 열지 않는다. 항목 저장이 유니크 제약 위반을 삼키므로 바깥 트랜잭션이 있으면 안 되고,
 * 페이지마다 반영되어야 중간에 실패해도 받은 만큼은 남는다.
 */
@Service
class AdminWork24CollectionService(
    private val work24Collector: Work24Collector,
    private val clock: Clock,
) {

    /**
     * 한 대상이 실패해도 나머지는 계속 받는다. 인증키가 없는 서비스는 호출하지 않고 건너뛴다.
     * 실패한 대상이 있어도 예외를 던지지 않고 결과에 담는다.
     */
    fun collectAll(): List<AdminWork24CollectResult> {
        val today = LocalDate.now(clock)
        return Work24CollectionTarget.entries.map { target -> collect(target, today) }
    }

    private fun collect(target: Work24CollectionTarget, today: LocalDate): AdminWork24CollectResult {
        if (!work24Collector.isConfigured(target)) {
            log.info("고용24 인증키가 없어 수집을 건너뜁니다. target={}", target)
            return AdminWork24CollectResult.Skipped(target)
        }

        return try {
            val result = work24Collector.collect(target, today)
            log.info(
                "고용24 수집 완료. target={}, pages={}, fetched={}, appended={}",
                target,
                result.pageCount,
                result.fetchedCount,
                result.appendedCount,
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
