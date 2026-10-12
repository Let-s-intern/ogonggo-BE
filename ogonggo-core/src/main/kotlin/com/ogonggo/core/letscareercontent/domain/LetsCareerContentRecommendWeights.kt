package com.ogonggo.core.letscareercontent.domain

/**
 * 공고별 렛츠커리어 콘텐츠 추천의 점수 값이다. 운영자가 관리자 API로 바꾼다.
 *
 * - 직무·직군·준비 단계 점수는 공고와 얼마나 맞는지다.
 * - 갈래 가산점은 공고와 하나라도 맞는 콘텐츠(위 점수가 0보다 큰 것)에만 더한다. 무관한 프로그램이 가산점만으로
 *   올라오지 않게 하기 위해서다.
 * - [maxPerGroup]은 한 갈래(프로그램·자료집·블로그)에서 고르는 최대 개수다. 모자라면 넘어서 채운다.
 */
data class LetsCareerContentRecommendWeights(
    val roleScore: Int,
    val fieldScore: Int,
    val topicScore: Int,
    val maxTopicMatches: Int,
    val programBonus: Int,
    val materialBonus: Int,
    val blogBonus: Int,
    val maxPerGroup: Int,
) {
    init {
        listOf(roleScore, fieldScore, topicScore, programBonus, materialBonus, blogBonus).forEach {
            require(it in 0..MAX_SCORE) { "점수는 0 이상 $MAX_SCORE 이하여야 합니다." }
        }
        require(maxTopicMatches in 0..LetsCareerContentTopic.entries.size) {
            "준비 단계 최대 일치 개수는 0 이상 ${LetsCareerContentTopic.entries.size} 이하여야 합니다."
        }
        require(maxPerGroup in 1..MAX_PER_GROUP) { "갈래별 최대 개수는 1 이상 $MAX_PER_GROUP 이하여야 합니다." }
    }

    companion object {
        const val MAX_SCORE = 100
        const val MAX_PER_GROUP = 10

        /** 2026-10-10 로컬 공고로 비교해 정한 기본값. 공고마다 프로그램이 보통 1~2개 섞인다. */
        val DEFAULT = LetsCareerContentRecommendWeights(
            roleScore = 5,
            fieldScore = 3,
            topicScore = 2,
            maxTopicMatches = 2,
            programBonus = 2,
            materialBonus = 1,
            blogBonus = 0,
            maxPerGroup = 2,
        )
    }
}
