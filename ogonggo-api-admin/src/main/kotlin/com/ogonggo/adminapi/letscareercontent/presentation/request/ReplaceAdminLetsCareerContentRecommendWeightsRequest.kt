package com.ogonggo.adminapi.letscareercontent.presentation.request

import com.ogonggo.core.letscareercontent.domain.LetsCareerContentRecommendWeights
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentRecommendWeights.Companion.MAX_PER_GROUP
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentRecommendWeights.Companion.MAX_SCORE
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

/** 추천 점수 전체다. 모든 칸을 보냅니다. */
data class ReplaceAdminLetsCareerContentRecommendWeightsRequest(
    @field:Schema(description = "공고와 직무가 같을 때 더하는 점수")
    @field:NotNull @field:Min(0) @field:Max(MAX_SCORE.toLong())
    val roleScore: Int?,

    @field:Schema(description = "공고와 직군이 같을 때 더하는 점수")
    @field:NotNull @field:Min(0) @field:Max(MAX_SCORE.toLong())
    val fieldScore: Int?,

    @field:Schema(description = "공고가 요구하는 준비 단계(자기소개서·면접 등)와 하나가 겹칠 때 더하는 점수")
    @field:NotNull @field:Min(0) @field:Max(MAX_SCORE.toLong())
    val topicScore: Int?,

    @field:Schema(description = "준비 단계 점수를 셀 최대 개수")
    @field:NotNull @field:Min(0) @field:Max(10)
    val maxTopicMatches: Int?,

    @field:Schema(description = "프로그램 가산점. 공고와 하나라도 맞는 프로그램에만 더한다")
    @field:NotNull @field:Min(0) @field:Max(MAX_SCORE.toLong())
    val programBonus: Int?,

    @field:Schema(description = "무료 자료집 가산점. 공고와 하나라도 맞는 자료집에만 더한다")
    @field:NotNull @field:Min(0) @field:Max(MAX_SCORE.toLong())
    val materialBonus: Int?,

    @field:Schema(description = "블로그 가산점. 공고와 하나라도 맞는 블로그에만 더한다")
    @field:NotNull @field:Min(0) @field:Max(MAX_SCORE.toLong())
    val blogBonus: Int?,

    @field:Schema(description = "한 갈래(프로그램·자료집·블로그)에서 고르는 최대 개수. 모자라면 다른 갈래로 채운다")
    @field:NotNull @field:Min(1) @field:Max(MAX_PER_GROUP.toLong())
    val maxPerGroup: Int?,
) {
    fun toWeights() = LetsCareerContentRecommendWeights(
        roleScore = checkNotNull(roleScore),
        fieldScore = checkNotNull(fieldScore),
        topicScore = checkNotNull(topicScore),
        maxTopicMatches = checkNotNull(maxTopicMatches),
        programBonus = checkNotNull(programBonus),
        materialBonus = checkNotNull(materialBonus),
        blogBonus = checkNotNull(blogBonus),
        maxPerGroup = checkNotNull(maxPerGroup),
    )
}
