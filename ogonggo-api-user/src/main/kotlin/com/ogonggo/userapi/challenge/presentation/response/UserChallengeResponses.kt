package com.ogonggo.userapi.challenge.presentation.response

import com.ogonggo.userapi.challenge.implement.dto.RecommendedChallengeDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class UserRecommendedChallengeResponse(
    @field:Schema(description = "렛츠커리어 챌린지 식별자. 렛츠커리어 챌린지 상세로 이동할 때 쓴다.")
    val challengeId: Long,
    val title: String,
    val shortDescription: String?,
    val thumbnailUrl: String?,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    @field:Schema(description = "챌린지 진행 시작 일시")
    val programStartAt: LocalDateTime?,
    @field:Schema(description = "챌린지 진행 종료 일시")
    val programEndAt: LocalDateTime?,
) {
    companion object {
        fun from(challenge: RecommendedChallengeDto): UserRecommendedChallengeResponse = UserRecommendedChallengeResponse(
            challengeId = challenge.challengeId,
            title = challenge.title,
            shortDescription = challenge.shortDescription,
            thumbnailUrl = challenge.thumbnailUrl,
            recruitmentStartAt = challenge.recruitmentStartAt,
            recruitmentEndAt = challenge.recruitmentEndAt,
            programStartAt = challenge.programStartAt,
            programEndAt = challenge.programEndAt,
        )
    }
}
