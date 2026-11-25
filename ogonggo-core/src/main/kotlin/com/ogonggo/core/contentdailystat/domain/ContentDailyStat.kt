package com.ogonggo.core.contentdailystat.domain

import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricDto
import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate

/**
 * 콘텐츠 이벤트를 하루(KST)·대상·광고 노출·유입 경로마다 모은 한 행이다. 광고 성과 리포트가 전날까지의 숫자를 여기서 읽는다.
 *
 * 원본(`content_events`)에서 다시 만들 수 있는 파생 기록이다. 하루를 다시 모으면 그날 행을 지우고 새로 쓴다(물리 삭제).
 * 대상 없는 배너 노출·클릭은 대상 칸이, 광고로 들어오지 않은 조회는 노출 칸이 비어 있다.
 */
@Entity
@Table(
    name = "content_daily_stats",
    indexes = [
        Index(name = "idx_content_daily_stats_stat_date", columnList = "stat_date"),
        Index(name = "idx_content_daily_stats_exposure_date", columnList = "exposure_id, stat_date"),
        Index(name = "idx_content_daily_stats_target_date", columnList = "target_type, target_id, stat_date"),
    ],
)
internal class ContentDailyStat(
    @Column(name = "stat_date", nullable = false)
    val statDate: LocalDate, /* 집계한 날(KST) */

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    val targetType: ContentEventTargetType?, /* 대상 콘텐츠 종류. 대상 없는 배너 이벤트면 없다 */

    @Column(name = "target_id")
    val targetId: Long?, /* 대상 채용공고·부트캠프 식별자 */

    @Column(name = "exposure_id")
    val exposureId: Long?, /* 광고 노출 식별자. 광고로 들어오지 않았으면 없다 */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val source: ContentEventSource, /* 유입 경로 */

    @Column(nullable = false) val views: Long, /* 1분 안 재조회를 뺀 상세 조회수 */
    @Column(name = "unique_visitors", nullable = false) val uniqueVisitors: Long, /* 상세를 연 순 방문자 수 */
    @Column(name = "member_views", nullable = false) val memberViews: Long, /* 회원 조회수 */
    @Column(name = "member_unique_visitors", nullable = false) val memberUniqueVisitors: Long, /* 상세를 연 순 회원 수 */
    @Column(name = "cta_clicks", nullable = false) val ctaClicks: Long, /* CTA 클릭수 */
    @Column(name = "cta_unique_visitors", nullable = false) val ctaUniqueVisitors: Long, /* CTA를 누른 순 방문자 수 */
    @Column(name = "cta_unique_members", nullable = false) val ctaUniqueMembers: Long, /* CTA를 누른 순 회원 수 */
    @Column(name = "scroll_count", nullable = false) val scrollCount: Long, /* 스크롤 이벤트 수 */
    @Column(name = "scroll_percent_sum", nullable = false) val scrollPercentSum: Long, /* 스크롤 깊이(%) 합 */
    @Column(name = "duration_ms_sum", nullable = false) val durationMsSum: Long, /* 화면이 보인 시간(밀리초) 합 */
    @Column(name = "scroll_reach_25", nullable = false) val scrollReach25: Long, /* 25%까지 내린 순 방문자 수 */
    @Column(name = "scroll_reach_50", nullable = false) val scrollReach50: Long, /* 50%까지 내린 순 방문자 수 */
    @Column(name = "scroll_reach_75", nullable = false) val scrollReach75: Long, /* 75%까지 내린 순 방문자 수 */
    @Column(name = "scroll_reach_100", nullable = false) val scrollReach100: Long, /* 끝까지 내린 순 방문자 수 */
    @Column(nullable = false) val impressions: Long, /* 광고 노출수 */
    @Column(name = "unique_impressions", nullable = false) val uniqueImpressions: Long, /* 같은 방문자·노출은 하루 한 번으로 센 광고 노출수 */
    @Column(name = "ad_clicks", nullable = false) val adClicks: Long, /* 광고 소재 클릭수 */
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 일별 집계 식별자 */
        protected set

    fun toMetric() = ContentMetricDto(
        views = views,
        uniqueVisitors = uniqueVisitors,
        memberViews = memberViews,
        memberUniqueVisitors = memberUniqueVisitors,
        ctaClicks = ctaClicks,
        ctaUniqueVisitors = ctaUniqueVisitors,
        ctaUniqueMembers = ctaUniqueMembers,
        scrollCount = scrollCount,
        scrollPercentSum = scrollPercentSum,
        durationMsSum = durationMsSum,
        scrollReach25 = scrollReach25,
        scrollReach50 = scrollReach50,
        scrollReach75 = scrollReach75,
        scrollReach100 = scrollReach100,
        impressions = impressions,
        uniqueImpressions = uniqueImpressions,
        adClicks = adClicks,
    )

    companion object {
        fun of(statDate: LocalDate, key: ContentStatKey, metric: ContentMetricDto) = ContentDailyStat(
            statDate = statDate,
            targetType = key.targetType,
            targetId = key.targetId,
            exposureId = key.exposureId,
            source = key.source,
            views = metric.views,
            uniqueVisitors = metric.uniqueVisitors,
            memberViews = metric.memberViews,
            memberUniqueVisitors = metric.memberUniqueVisitors,
            ctaClicks = metric.ctaClicks,
            ctaUniqueVisitors = metric.ctaUniqueVisitors,
            ctaUniqueMembers = metric.ctaUniqueMembers,
            scrollCount = metric.scrollCount,
            scrollPercentSum = metric.scrollPercentSum,
            durationMsSum = metric.durationMsSum,
            scrollReach25 = metric.scrollReach25,
            scrollReach50 = metric.scrollReach50,
            scrollReach75 = metric.scrollReach75,
            scrollReach100 = metric.scrollReach100,
            impressions = metric.impressions,
            uniqueImpressions = metric.uniqueImpressions,
            adClicks = metric.adClicks,
        )
    }
}

/** 일별 집계 한 행을 가르는 값이다. */
data class ContentStatKey(
    val targetType: ContentEventTargetType?,
    val targetId: Long?,
    val exposureId: Long?,
    val source: ContentEventSource,
)
