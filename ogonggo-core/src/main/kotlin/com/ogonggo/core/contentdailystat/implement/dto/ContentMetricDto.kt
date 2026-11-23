package com.ogonggo.core.contentdailystat.implement.dto

/**
 * 한 범위(날짜·대상·노출·유입 경로)의 콘텐츠 지표다. 일별 집계 한 행이나 원본에서 바로 센 값이 같은 모양이다.
 *
 * - 조회수는 같은 방문자가 같은 대상을 30분 안에 다시 연 것을 뺀 값이다.
 * - 순 방문자 수는 이 범위 안에서 센 값이라, 여러 범위를 더하면 같은 방문자가 두 번 셀 수 있다.
 * - 스크롤 도달 수는 상세를 연 순 방문자 중 그 깊이까지 내린 방문자 수다. 방문자마다 가장 깊이 내린 값으로 센다.
 * - 순 노출수([uniqueImpressions])는 같은 방문자가 같은 노출을 하루(KST)에 여러 번 봐도 한 번으로 센 노출수다. 날짜를 더하면 하루 한 번씩 센 값이 된다.
 */
data class ContentMetricDto(
    val views: Long = 0,
    val uniqueVisitors: Long = 0,
    val memberViews: Long = 0,
    val memberUniqueVisitors: Long = 0,
    val ctaClicks: Long = 0,
    val ctaUniqueVisitors: Long = 0,
    val ctaUniqueMembers: Long = 0,
    val scrollCount: Long = 0,
    val scrollPercentSum: Long = 0,
    val durationMsSum: Long = 0,
    val scrollReach25: Long = 0,
    val scrollReach50: Long = 0,
    val scrollReach75: Long = 0,
    val scrollReach100: Long = 0,
    val impressions: Long = 0,
    val uniqueImpressions: Long = 0,
    val adClicks: Long = 0,
) {
    operator fun plus(other: ContentMetricDto) = ContentMetricDto(
        views = views + other.views,
        uniqueVisitors = uniqueVisitors + other.uniqueVisitors,
        memberViews = memberViews + other.memberViews,
        memberUniqueVisitors = memberUniqueVisitors + other.memberUniqueVisitors,
        ctaClicks = ctaClicks + other.ctaClicks,
        ctaUniqueVisitors = ctaUniqueVisitors + other.ctaUniqueVisitors,
        ctaUniqueMembers = ctaUniqueMembers + other.ctaUniqueMembers,
        scrollCount = scrollCount + other.scrollCount,
        scrollPercentSum = scrollPercentSum + other.scrollPercentSum,
        durationMsSum = durationMsSum + other.durationMsSum,
        scrollReach25 = scrollReach25 + other.scrollReach25,
        scrollReach50 = scrollReach50 + other.scrollReach50,
        scrollReach75 = scrollReach75 + other.scrollReach75,
        scrollReach100 = scrollReach100 + other.scrollReach100,
        impressions = impressions + other.impressions,
        uniqueImpressions = uniqueImpressions + other.uniqueImpressions,
        adClicks = adClicks + other.adClicks,
    )

    /** 스크롤 이벤트의 평균 깊이(%)다. 스크롤 이벤트가 없으면 null이다. 소수 첫째 자리까지. */
    val avgScrollPercent: Double?
        get() = if (scrollCount == 0L) null else round1(scrollPercentSum.toDouble() / scrollCount)

    /** 스크롤 이벤트에 실린 화면이 보인 시간의 평균(초)이다. 스크롤 이벤트가 없으면 null이다. 소수 첫째 자리까지. */
    val avgDurationSeconds: Double?
        get() = if (scrollCount == 0L) null else round1(durationMsSum.toDouble() / scrollCount / 1000)

    /** 깊이(0·25·50·75·100%)마다 도달한 순 방문자 수다. 0%는 순 방문자 전체다. */
    fun scrollReachAt(threshold: Int): Long = when (threshold) {
        0 -> uniqueVisitors
        25 -> scrollReach25
        50 -> scrollReach50
        75 -> scrollReach75
        100 -> scrollReach100
        else -> throw IllegalArgumentException("스크롤 깊이는 0, 25, 50, 75, 100 중 하나입니다.")
    }

    companion object {
        val ZERO = ContentMetricDto()
        val SCROLL_THRESHOLDS = listOf(0, 25, 50, 75, 100)

        /** 0~1 비율. 분모가 0이면 null이다. 소수 넷째 자리까지. */
        fun rate(numerator: Long, denominator: Long): Double? =
            if (denominator == 0L) null else Math.round(numerator.toDouble() / denominator * 10_000) / 10_000.0

        /** 평균(분자 ÷ 분모). 분모가 0이면 null이다. 소수 첫째 자리까지. */
        fun ratio(numerator: Long, denominator: Long): Double? =
            if (denominator == 0L) null else round1(numerator.toDouble() / denominator)

        private fun round1(value: Double): Double = Math.round(value * 10) / 10.0
    }
}
