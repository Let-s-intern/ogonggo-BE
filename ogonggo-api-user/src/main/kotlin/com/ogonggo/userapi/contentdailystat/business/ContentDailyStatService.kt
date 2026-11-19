package com.ogonggo.userapi.contentdailystat.business

import com.ogonggo.core.contentdailystat.implement.ContentDailyStatManager
import com.ogonggo.core.contentevent.implement.ContentEventRemover
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

/** 광고 성과 리포트의 일별 집계와 원본 정리다. 예약 작업이 매일 새벽 부른다. */
@Service
class ContentDailyStatService(
    private val contentDailyStatManager: ContentDailyStatManager,
    private val contentEventRemover: ContentEventRemover,
    private val clock: Clock,
) {

    /** 전날(KST) 이벤트를 모아 그날 행을 새로 쓴다. 지우고 쓰기를 한 트랜잭션으로 묶어 리포트가 빈 날을 보지 않게 한다. */
    @Transactional
    fun aggregate(statDate: LocalDate): Int = contentDailyStatManager.rebuild(statDate)

    fun yesterday(): LocalDate = LocalDate.now(clock).minusDays(1)

    /** 보관 기간이 지난 원본을 묶음마다 커밋하며 지운다. 지운 수를 돌려준다. */
    fun removeExpiredEvents(batchSize: Int): Int {
        val today = LocalDate.now(clock)
        var total = 0
        do {
            val removed = contentEventRemover.removeExpired(today, batchSize)
            total += removed
        } while (removed == batchSize)
        return total
    }
}
