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

    /**
     * 이번에 집계할 날을 오래된 날부터 준다. 마지막 집계일 다음 날부터 어제까지이고, 집계가 처음이면 어제 하루다.
     * 어제는 이미 모았어도 포함한다(같은 날을 다시 모아도 결과가 같다). 오래 멈췄어도 한 번에 [MAX_CATCH_UP_DAYS]일까지만 거슬러 간다.
     * 하루씩 [aggregate]를 부르려면 이 서비스 밖(스케줄러)에서 불러야 날마다 트랜잭션이 따로 열린다.
     */
    fun datesToAggregate(): List<LocalDate> {
        val yesterday = yesterday()
        val lastStatDate = contentDailyStatManager.lastStatDate() ?: return listOf(yesterday)
        val start = maxOf(minOf(lastStatDate.plusDays(1), yesterday), yesterday.minusDays(MAX_CATCH_UP_DAYS - 1))
        return start.datesUntil(yesterday.plusDays(1)).toList()
    }

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

    companion object {
        const val MAX_CATCH_UP_DAYS = 31L
    }
}
