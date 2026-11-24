package com.ogonggo.core.contentdailystat.persistence

import com.ogonggo.core.contentdailystat.domain.ContentDailyStat
import com.ogonggo.core.contentdailystat.domain.QContentDailyStat.contentDailyStat
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricScope
import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.domain.QContentEvent.contentEvent
import com.ogonggo.core.user.domain.QUserProfile.userProfile
import com.ogonggo.core.user.domain.UserGrade
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
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

/** 회원 프로필에서 리포트가 쓰는 값이다. */
internal data class ContentMemberProfileRow(
    val wishJob: String?,
    val grade: UserGrade?,
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

/** [type] 이벤트를 낸 서로 다른 방문자 수와 서로 다른 회원 수다. */
internal data class ContentUniqueCount(val visitors: Long, val members: Long)

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

    /**
     * [rows]가 센 대상의 조회 중 [scope] 밖에 있는 것이다. 30분 안 재조회를 가를 때 기준으로만 쓰고 세지 않는다.
     * 예를 들어 목록에서 연 조회(노출 없음)가 10분 뒤 광고로 다시 연 조회의 직전 조회가 된다.
     * 범위가 노출 몇 개일 때만 있다. 대상 하나나 전체는 그 대상의 조회가 모두 범위 안이다.
     */
    fun findViewsOutsideScope(
        scope: ContentMetricScope,
        rows: List<ContentEventRow>,
        from: LocalDateTime,
        toExclusive: LocalDateTime,
    ): List<ContentEventRow> {
        if (scope !is ContentMetricScope.Exposures || scope.isEmpty) return emptyList()
        val idsByType = rows.filter { it.targetType != null && it.targetId != null }
            .groupBy({ checkNotNull(it.targetType) }, { checkNotNull(it.targetId) })
            .mapValues { (_, ids) -> ids.toSet() }
        if (idsByType.isEmpty()) return emptyList()
        val onTargets = idsByType.map { (type, ids) -> contentEvent.targetType.eq(type).and(contentEvent.targetId.`in`(ids)) }
            .reduce { left, right -> left.or(right) }
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
                contentEvent.eventType.eq(ContentEventType.VIEW),
                onTargets,
                contentEvent.exposureId.isNull.or(contentEvent.exposureId.notIn(scope.exposureIds)),
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

    /**
     * 노출을 본 서로 다른 방문자 수를 집계 행 단위로 센다. 하루 범위로 부르면 "하루 한 번" 순 노출수다.
     * 방문자는 [ContentVisitorKeys] 규칙으로 가르며 표는 [from]~[toExclusive] 안에서 만든다.
     */
    fun countUniqueImpressions(scope: ContentMetricScope, from: LocalDateTime, toExclusive: LocalDateTime): List<ContentAdEventCount> {
        if (scope.isEmpty) return emptyList()
        val visitors = ContentVisitorKeys.expression(from, toExclusive).countDistinct()
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

    /**
     * 회원의 희망 직무 원문(렛츠커리어 선택지를 `, `로 이은 값)과 학년이다. 프로필이 없으면 넣지 않고,
     * 희망 직무나 학년이 비어 있으면 그 값만 null이다.
     */
    fun findProfiles(userIds: Collection<Long>): Map<Long, ContentMemberProfileRow> {
        if (userIds.isEmpty()) return emptyMap()
        return queryFactory.select(userProfile.userId, userProfile.wishJob, userProfile.grade)
            .from(userProfile)
            .where(userProfile.userId.`in`(userIds))
            .fetch()
            .associate {
                checkNotNull(it.get(userProfile.userId)) to ContentMemberProfileRow(it.get(userProfile.wishJob), it.get(userProfile.grade))
            }
    }

    /** [type] 이벤트 수다. 상세 집계에 넣지 않는 이벤트(공유)를 기간으로 센다. */
    fun countEvents(scope: ContentMetricScope, type: ContentEventType, from: LocalDateTime, toExclusive: LocalDateTime): Long {
        if (scope.isEmpty) return 0L
        return queryFactory.select(contentEvent.id.count())
            .from(contentEvent)
            .where(
                eventScope(scope),
                contentEvent.eventType.eq(type),
                contentEvent.occurredAt.goe(from),
                contentEvent.occurredAt.lt(toExclusive),
            )
            .fetchOne() ?: 0L
    }

    /** [type] 이벤트를 낸 서로 다른 방문자 수다. 방문자는 [ContentVisitorKeys] 규칙으로 가르며 표는 [from]~[toExclusive] 안에서 만든다. */
    fun countDistinctVisitors(scope: ContentMetricScope, type: ContentEventType, from: LocalDateTime, toExclusive: LocalDateTime): Long {
        if (scope.isEmpty) return 0L
        return queryFactory.select(ContentVisitorKeys.expression(from, toExclusive).countDistinct())
            .from(contentEvent)
            .where(
                eventScope(scope),
                contentEvent.eventType.eq(type),
                contentEvent.occurredAt.goe(from),
                contentEvent.occurredAt.lt(toExclusive),
            )
            .fetchOne() ?: 0L
    }

    /**
     * [from]~[toExclusive] 동안 [type] 이벤트를 낸 서로 다른 방문자(방문자 키)와 회원을 센다. 원본을 읽지 않고 SQL로 센다.
     * 방문자 표는 [keyFrom]~[keyToExclusive] 안에서 만든다. 코틀린 계산(`ContentMetricReader.read`의 상세 경로)과 같은 값을 내려면
     * 그 계산이 쓰는 범위(조회 시작 30분 전부터)를 넘긴다.
     */
    fun countUniques(
        scope: ContentMetricScope,
        type: ContentEventType,
        from: LocalDateTime,
        toExclusive: LocalDateTime,
        keyFrom: LocalDateTime,
        keyToExclusive: LocalDateTime,
    ): ContentUniqueCount {
        if (scope.isEmpty) return ContentUniqueCount(0L, 0L)
        val visitors = ContentVisitorKeys.expression(keyFrom, keyToExclusive).countDistinct()
        val members = contentEvent.userId.countDistinct()
        val row = queryFactory.select(visitors, members)
            .from(contentEvent)
            .where(
                eventScope(scope),
                contentEvent.eventType.eq(type),
                contentEvent.occurredAt.goe(from),
                contentEvent.occurredAt.lt(toExclusive),
            )
            .fetchOne()
        return ContentUniqueCount(row?.get(visitors) ?: 0L, row?.get(members) ?: 0L)
    }

    /**
     * [type] 이벤트를 낸 서로 다른 방문자 수를 날짜별로 센다. 이벤트가 없는 날은 빠진다.
     * 방문자 표는 [countDistinctVisitors]와 같이 [from]~[toExclusive] 전체로 만들어, 하루짜리 기간이면 두 값이 같다.
     */
    fun countDistinctVisitorsByDate(scope: ContentMetricScope, type: ContentEventType, from: LocalDateTime, toExclusive: LocalDateTime): Map<LocalDate, Long> {
        if (scope.isEmpty) return emptyMap()
        val date = Expressions.dateTemplate(java.sql.Date::class.java, "cast({0} as date)", contentEvent.occurredAt)
        val visitors = ContentVisitorKeys.expression(from, toExclusive).countDistinct()
        return queryFactory.select(date, visitors)
            .from(contentEvent)
            .where(
                eventScope(scope),
                contentEvent.eventType.eq(type),
                contentEvent.occurredAt.goe(from),
                contentEvent.occurredAt.lt(toExclusive),
            )
            .groupBy(date)
            .fetch()
            .associate { checkNotNull(it.get(date)).toLocalDate() to checkNotNull(it.get(visitors)) }
    }

    /**
     * [from]~[toExclusive] 안에서 회원 이벤트를 보낸 적 있는 방문자id마다 가장 작은 회원id를 모은 표다.
     * 범위·이벤트 종류와 상관없이 모든 이벤트에서 만든다. 회원 이벤트를 보낸 브라우저만 들어 있어 작다.
     */
    fun findVisitorKeys(from: LocalDateTime, toExclusive: LocalDateTime): ContentVisitorKeys {
        val userId = contentEvent.userId.min()
        val userByVisitor = queryFactory.select(contentEvent.visitorId, userId)
            .from(contentEvent)
            .where(contentEvent.userId.isNotNull, contentEvent.occurredAt.goe(from), contentEvent.occurredAt.lt(toExclusive))
            .groupBy(contentEvent.visitorId)
            .fetch()
            .associate { checkNotNull(it.get(contentEvent.visitorId)) to checkNotNull(it.get(userId)) }
        return ContentVisitorKeys(userByVisitor)
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

    private companion object {
        val DETAIL_EVENT_TYPES = listOf(ContentEventType.VIEW, ContentEventType.CTA_CLICK, ContentEventType.SCROLL)
        val AD_EVENT_TYPES = listOf(ContentEventType.IMPRESSION, ContentEventType.AD_CLICK)
    }
}
