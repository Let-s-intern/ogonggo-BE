package com.ogonggo.core.contentdailystat.implement.dto

/** 기간 안에 광고를 본 서로 다른 방문자 수(도달)와 광고를 누른 서로 다른 방문자 수다. 회원이든 비회원이든 한 사람으로 센다. */
data class ContentAdReachDto(
    val impressionVisitors: Long,
    val adClickVisitors: Long,
)
