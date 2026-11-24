package com.ogonggo.core.contentdailystat.implement

import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricScope
import com.ogonggo.core.contentdailystat.persistence.ContentDailyStatJpaRepository
import com.ogonggo.core.contentdailystat.persistence.ContentDailyStatQueryRepository
import com.ogonggo.core.contentevent.domain.ContentEvent
import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.persistence.ContentEventJpaRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(ContentMetricReader::class, ContentDailyStatManager::class, ContentDailyStatQueryRepository::class)
internal class ContentMetricReaderPersistenceTest @Autowired constructor(
    private val contentMetricReader: ContentMetricReader,
    private val contentDailyStatManager: ContentDailyStatManager,
    private val contentDailyStatRepository: ContentDailyStatJpaRepository,
    private val contentEventRepository: ContentEventJpaRepository,
) {

    private val scope = ContentMetricScope.Exposures(setOf(EXPOSURE_ID))

    @Test
    fun `목록에서 연 조회 10분 뒤 광고로 다시 연 조회는 오늘 칸에서도 세지 않아 일별 집계와 같다`() {
        // given
        save(view(at(10, 0), exposureId = null, source = ContentEventSource.LIST))
        save(view(at(10, 10)))
        save(view(at(11, 0)))

        // when
        val today = contentMetricReader.read(scope, DAY, DAY, today = DAY)
        contentDailyStatManager.rebuild(DAY)
        val aggregated = contentMetricReader.read(scope, DAY, DAY, today = DAY.plusDays(1))

        // then
        assertEquals(1L, today.period.views)
        assertEquals(aggregated.period.views, today.period.views)
        assertEquals(contentDailyStatRepository.findAll().single { it.exposureId == EXPOSURE_ID }.views, today.period.views)
    }

    @Test
    fun `다른 노출로 연 조회와 비회원 키의 조회도 범위 밖이면 직전 조회로만 보고 회원 키 조회와 한 방문자로 가른다`() {
        // given
        save(view(at(10, 0), exposureId = OTHER_EXPOSURE_ID, browser = "a"))
        save(view(at(10, 5), userId = 7L, browser = "a"))
        save(view(at(10, 6), browser = "b"))

        // when
        val period = contentMetricReader.read(scope, DAY, DAY, today = DAY).period

        // then
        assertEquals(1L, period.views)
        assertEquals(0L, period.memberViews)
        assertEquals(2L, period.uniqueVisitors)
    }

    @Test
    fun `30분이 지난 뒤 광고로 연 조회는 센다`() {
        // given
        save(view(at(10, 0), exposureId = null, source = ContentEventSource.LIST))
        save(view(at(10, 30)))

        // when
        val period = contentMetricReader.read(scope, DAY, DAY, today = DAY).period

        // then
        assertEquals(1L, period.views)
    }

    @Test
    fun `회원별 조회수도 범위 밖 직전 조회를 보고 센다`() {
        // given
        save(view(at(10, 0), userId = 7L, browser = "m7", exposureId = null, source = ContentEventSource.LIST))
        save(view(at(10, 10), userId = 7L, browser = "m7"))
        save(view(at(12, 0), userId = 8L, browser = "m8"))

        // when
        val activities = contentMetricReader.readMemberActivities(scope, DAY, DAY, today = DAY).associateBy { it.userId }

        // then
        assertEquals(0L, activities.getValue(7L).views)
        assertEquals(true, activities.getValue(7L).viewed)
        assertEquals(1L, activities.getValue(8L).views)
    }

    private fun save(event: ContentEvent) = contentEventRepository.save(event)

    private fun at(hour: Int, minute: Int): LocalDateTime = DAY.atTime(hour, minute)

    private fun view(
        occurredAt: LocalDateTime,
        userId: Long? = null,
        exposureId: Long? = EXPOSURE_ID,
        source: ContentEventSource = ContentEventSource.AD_CARD,
        browser: String = "a",
    ) = ContentEvent(
        eventType = ContentEventType.VIEW,
        targetType = ContentEventTargetType.JOB,
        targetId = JOB_ID,
        exposureId = exposureId,
        userId = userId,
        visitorId = browser,
        source = source,
        scrollPercent = null,
        durationMs = null,
        occurredAt = occurredAt,
    )

    private companion object {
        val DAY: LocalDate = LocalDate.of(2026, 11, 18)
        const val JOB_ID = 15818L
        const val EXPOSURE_ID = 31L
        const val OTHER_EXPOSURE_ID = 32L
    }
}
