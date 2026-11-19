package com.ogonggo.core.contentdailystat.persistence

import com.ogonggo.core.contentdailystat.domain.ContentDailyStat
import com.ogonggo.core.contentdailystat.domain.QContentDailyStat.contentDailyStat
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricScope
import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.domain.QContentEvent.contentEvent
import com.ogonggo.core.user.domain.QUserProfile.userProfile
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

internal interface ContentDailyStatJpaRepository : JpaRepository<ContentDailyStat, Long> {

    /** 하루를 다시 모으기 전에 그날 행을 지운다. 파생 기록이라 물리 삭제한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ContentDailyStat s where s.statDate = :statDate")
    fun deleteAllByStatDate(statDate: LocalDate): Int

    @Query("select min(s.statDate) from ContentDailyStat s")
    fun findFirstStatDate(): LocalDate?

    @Query("select max(s.statDate) from ContentDailyStat s")
    fun findLastStatDate(): LocalDate?
}

/** 상세 화면 이벤트(조회·CTA 클릭·스크롤) 한 건에서 집계에 쓰는 값이다. */
internal data class ContentEventRow(
    val eventType: ContentEventType,
    val targetType: ContentEventTargetType?,
    val targetId: Long?,
    val exposureId: Long?,
    val userId: Long?,
    val visitorId: String,
    val source: ContentEventSource,
    val scrollPercent: Int?,
    val occurredAt: LocalDateTime,
    val durationMs: Long?,
)

/** 광고 노출·클릭을 집계 행 단위로 센 수다. */
internal data class ContentAdEventCount(
    val eventType: ContentEventType,
    val targetType: ContentEventTargetType?,
    val targetId: Long?,
    val exposureId: Long?,
    val source: ContentEventSource,
    val count: Long,
)

@Repository
internal class ContentDailyStatQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    /** 상세 화면 이벤트를 받은 시각 순으로 준다. 광고 노출·클릭은 수가 많아 [countAdEvents]로 센다. */
    fun findDetailRows(scope: ContentMetricScope, from: LocalDateTime, toExclusive: LocalDateTime): List<ContentEventRow> {
        if (scope.isEmpty) return emptyList()
        return queryFactory
            .select(
                Projections.constructor(
                    ContentEventRow::class.java,
                    contentEvent.eventType,
                    contentEvent.targetType,
                    contentEvent.targetId,
                    contentEvent.exposureId,
                    contentEvent.userId,
                    contentEvent.visitorId,
                    contentEvent.source,
                    contentEvent.scrollPercent,
                    contentEvent.occurredAt,
                    contentEvent.durationMs,
                ),
            )
            .from(contentEvent)
            .where(
                eventScope(scope),
                contentEvent.eventType.`in`(DETAIL_EVENT_TYPES),
                contentEvent.occurredAt.goe(from),
                contentEvent.occurredAt.lt(toExclusive),
            )
            .orderBy(contentEvent.occurredAt.asc(), contentEvent.id.asc())
            .fetch()
    }

    fun countAdEvents(scope: ContentMetricScope, from: LocalDateTime, toExclusive: LocalDateTime): List<ContentAdEventCount> {
        if (scope.isEmpty) return emptyList()
        val count = contentEvent.id.count()
        return queryFactory
            .select(
                contentEvent.eventType,
                contentEvent.targetType,
                contentEvent.targetId,
                contentEvent.exposureId,
                contentEvent.source,
                count,
            )
            .from(contentEvent)
            .where(
                eventScope(scope),
                contentEvent.eventType.`in`(AD_EVENT_TYPES),
                contentEvent.occurredAt.goe(from),
                contentEvent.occurredAt.lt(toExclusive),
            )
            .groupBy(contentEvent.eventType, contentEvent.targetType, contentEvent.targetId, contentEvent.exposureId, contentEvent.source)
            .fetch()
            .map {
                ContentAdEventCount(
                    eventType = it.get(contentEvent.eventType)!!,
                    targetType = it.get(contentEvent.targetType),
                    targetId = it.get(contentEvent.targetId),
                    exposureId = it.get(contentEvent.exposureId),
                    source = it.get(contentEvent.source)!!,
                    count = it.get(count) ?: 0L,
                )
            }
    }

    /** 노출을 본 서로 다른 방문자 수를 집계 행 단위로 센다. 하루 범위로 부르면 "하루 한 번" 순 노출수다. */
    fun countUniqueImpressions(scope: ContentMetricScope, from: LocalDateTime, toExclusive: LocalDateTime): List<ContentAdEventCount> {
        if (scope.isEmpty) return emptyList()
        val visitors = visitorKey.countDistinct()
        return queryFactory
            .select(contentEvent.targetType, contentEvent.targetId, contentEvent.exposureId, contentEvent.source, visitors)
            .from(contentEvent)
            .where(
                eventScope(scope),
                contentEvent.eventType.eq(ContentEventType.IMPRESSION),
                contentEvent.occurredAt.goe(from),
                contentEvent.occurredAt.lt(toExclusive),
            )
            .groupBy(contentEvent.targetType, contentEvent.targetId, contentEvent.exposureId, contentEvent.source)
            .fetch()
            .map {
                ContentAdEventCount(
                    eventType = ContentEventType.IMPRESSION,
                    targetType = it.get(contentEvent.targetType),
                    targetId = it.get(contentEvent.targetId),
                    exposureId = it.get(contentEvent.exposureId),
                    source = it.get(contentEvent.source)!!,
                    count = it.get(visitors) ?: 0L,
                )
            }
    }

    /** 광고 소재를 누른 회원 식별자다. */
    fun findAdClickMemberIds(scope: ContentMetricScope, from: LocalDateTime, toExclusive: LocalDateTime): Set<Long> {
        if (scope.isEmpty) return emptySet()
        return queryFactory.select(contentEvent.userId).distinct()
            .from(contentEvent)
            .where(
                eventScope(scope),
                contentEvent.eventType.eq(ContentEventType.AD_CLICK),
                contentEvent.userId.isNotNull,
                contentEvent.occurredAt.goe(from),
                contentEvent.occurredAt.lt(toExclusive),
            )
            .fetch()
            .filterNotNull()
            .toSet()
    }

    /** 회원의 희망 직무 원문(렛츠커리어 선택지를 `, `로 이은 값)이다. 프로필이 없거나 비어 있으면 넣지 않는다. */
    fun findWishJobs(userIds: Collection<Long>): Map<Long, String> {
        if (userIds.isEmpty()) return emptyMap()
        return queryFactory.select(userProfile.userId, userProfile.wishJob)
            .from(userProfile)
            .where(userProfile.userId.`in`(userIds), userProfile.wishJob.isNotNull)
            .fetch()
            .associate { checkNotNull(it.get(userProfile.userId)) to checkNotNull(it.get(userProfile.wishJob)) }
    }

    fun findFirstOccurredAt(): LocalDateTime? =
        queryFactory.select(contentEvent.occurredAt.min()).from(contentEvent).fetchOne()

    fun findStats(scope: ContentMetricScope, from: LocalDate, to: LocalDate): List<ContentDailyStat> {
        if (scope.isEmpty) return emptyList()
        return queryFactory.selectFrom(contentDailyStat)
            .where(statScope(scope), contentDailyStat.statDate.between(from, to))
            .orderBy(contentDailyStat.statDate.asc(), contentDailyStat.id.asc())
            .fetch()
    }

    private fun eventScope(scope: ContentMetricScope): BooleanExpression? = when (scope) {
        is ContentMetricScope.Exposures -> contentEvent.exposureId.`in`(scope.exposureIds)
        is ContentMetricScope.Target -> contentEvent.targetType.eq(scope.targetType).and(contentEvent.targetId.eq(scope.targetId))
        ContentMetricScope.All -> null
    }

    private fun statScope(scope: ContentMetricScope): BooleanExpression? = when (scope) {
        is ContentMetricScope.Exposures -> contentDailyStat.exposureId.`in`(scope.exposureIds)
        is ContentMetricScope.Target -> contentDailyStat.targetType.eq(scope.targetType).and(contentDailyStat.targetId.eq(scope.targetId))
        ContentMetricScope.All -> null
    }

    /** 방문자는 회원이면 회원 식별자, 아니면 익명 방문자 식별자다. */
    private val visitorKey = CaseBuilder()
        .`when`(contentEvent.userId.isNotNull).then(contentEvent.userId.stringValue().prepend("u:"))
        .otherwise(contentEvent.visitorId.prepend("v:"))

    private companion object {
        val DETAIL_EVENT_TYPES = listOf(ContentEventType.VIEW, ContentEventType.CTA_CLICK, ContentEventType.SCROLL)
        val AD_EVENT_TYPES = listOf(ContentEventType.IMPRESSION, ContentEventType.AD_CLICK)
    }
}
