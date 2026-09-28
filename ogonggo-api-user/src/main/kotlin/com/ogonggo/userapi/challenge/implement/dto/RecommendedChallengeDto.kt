package com.ogonggo.userapi.challenge.implement.dto

import java.time.LocalDateTime

/** 렛츠커리어가 추천한 챌린지 하나다. 신청은 렛츠커리어에서 하므로 오공고는 보여주기만 한다. */
data class RecommendedChallengeDto(
    val challengeId: Long,
    val title: String,
    val shortDescription: String?,
    val thumbnailUrl: String?,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val programStartAt: LocalDateTime?,
    val programEndAt: LocalDateTime?,
)
