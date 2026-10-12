package com.ogonggo.adminapi.letscareercontent.presentation.response

import com.ogonggo.core.letscareercontent.domain.LetsCareerContentRecommendWeights

/** 칸의 뜻은 수정 요청과 같습니다. */
data class AdminLetsCareerContentRecommendWeightsResponse(
    val roleScore: Int,
    val fieldScore: Int,
    val topicScore: Int,
    val maxTopicMatches: Int,
    val programBonus: Int,
    val materialBonus: Int,
    val blogBonus: Int,
    val maxPerGroup: Int,
) {
    companion object {
        fun from(weights: LetsCareerContentRecommendWeights) = AdminLetsCareerContentRecommendWeightsResponse(
            roleScore = weights.roleScore,
            fieldScore = weights.fieldScore,
            topicScore = weights.topicScore,
            maxTopicMatches = weights.maxTopicMatches,
            programBonus = weights.programBonus,
            materialBonus = weights.materialBonus,
            blogBonus = weights.blogBonus,
            maxPerGroup = weights.maxPerGroup,
        )
    }
}
