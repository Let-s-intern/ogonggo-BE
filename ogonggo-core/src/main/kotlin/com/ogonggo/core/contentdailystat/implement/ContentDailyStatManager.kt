package com.ogonggo.core.contentdailystat.implement

import com.ogonggo.core.contentdailystat.domain.ContentDailyStat
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricDto
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricScope
import com.ogonggo.core.contentdailystat.persistence.ContentDailyStatJpaRepository
import com.ogonggo.core.contentdailystat.persistence.ContentDailyStatQueryRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ContentDailyStatManager internal constructor(
    private val contentDailyStatRepository: ContentDailyStatJpaRepository,
    private val contentDailyStatQueryRepository: ContentDailyStatQueryRepository,
) {

    /**
     * 하루(KST) 이벤트를 대상·노출·유입 경로마다 모아 그날 행을 새로 쓰고, 쓴 행 수를 돌려준다.
     * 그날 행을 먼저 지우므로 같은 날을 여러 번 돌려도 결과가 같다. 호출한 서비스의 트랜잭션 안에서 부른다.
     */
    fun rebuild(statDate: LocalDate): Int {
        val from = statDate.atStartOfDay()
        val toExclusive = statDate.plusDays(1).atStartOfDay()
        val rows = contentDailyStatQueryRepository.findDetailRows(
            ContentMetricScope.All,
            from.minus(ContentMetricCalculator.DUPLICATE_VIEW_WINDOW),
            toExclusive,
        )
        val detail = ContentMetricCalculator.measure(rows, from, ContentMetricCalculator::statKeyOf)
        val ads = ContentMetricCalculator.adMetricsByKey(
            contentDailyStatQueryRepository.countAdEvents(ContentMetricScope.All, from, toExclusive),
            contentDailyStatQueryRepository.countUniqueImpressions(ContentMetricScope.All, from, toExclusive),
        )
        val stats = (detail.keys + ads.keys).map { key ->
            ContentDailyStat.of(statDate, key, (detail[key] ?: ContentMetricDto.ZERO) + (ads[key] ?: ContentMetricDto.ZERO))
        }
        contentDailyStatRepository.deleteAllByStatDate(statDate)
        contentDailyStatRepository.saveAll(stats)
        return stats.size
    }

    /** 집계가 있는 마지막 날이다. 아직 집계가 없으면 null이다. */
    fun lastStatDate(): LocalDate? = contentDailyStatRepository.findLastStatDate()
}
