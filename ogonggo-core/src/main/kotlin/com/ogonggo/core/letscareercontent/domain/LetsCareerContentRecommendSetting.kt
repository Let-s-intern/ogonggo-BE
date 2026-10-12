package com.ogonggo.core.letscareercontent.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 공고별 렛츠커리어 콘텐츠 추천의 점수 설정이다. 한 행만 두며, 행이 없으면 [LetsCareerContentRecommendWeights.DEFAULT]를 쓴다.
 * 지우는 경로가 없어 삭제 일시를 두지 않는다.
 */
@Entity
@Table(name = "lets_career_content_recommend_settings")
class LetsCareerContentRecommendSetting internal constructor(
    weights: LetsCareerContentRecommendWeights,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 추천 점수 설정 식별자 */
        protected set

    @Column(name = "role_score", nullable = false)
    var roleScore: Int = weights.roleScore /* 직무가 같을 때 */
        protected set

    @Column(name = "field_score", nullable = false)
    var fieldScore: Int = weights.fieldScore /* 직군이 같을 때 */
        protected set

    @Column(name = "topic_score", nullable = false)
    var topicScore: Int = weights.topicScore /* 준비 단계 하나가 겹칠 때 */
        protected set

    @Column(name = "max_topic_matches", nullable = false)
    var maxTopicMatches: Int = weights.maxTopicMatches /* 준비 단계 점수를 셀 최대 개수 */
        protected set

    @Column(name = "program_bonus", nullable = false)
    var programBonus: Int = weights.programBonus /* 프로그램 가산점 */
        protected set

    @Column(name = "material_bonus", nullable = false)
    var materialBonus: Int = weights.materialBonus /* 무료 자료집 가산점 */
        protected set

    @Column(name = "blog_bonus", nullable = false)
    var blogBonus: Int = weights.blogBonus /* 블로그 가산점 */
        protected set

    @Column(name = "max_per_group", nullable = false)
    var maxPerGroup: Int = weights.maxPerGroup /* 한 갈래에서 고르는 최대 개수 */
        protected set

    fun replace(weights: LetsCareerContentRecommendWeights) {
        roleScore = weights.roleScore
        fieldScore = weights.fieldScore
        topicScore = weights.topicScore
        maxTopicMatches = weights.maxTopicMatches
        programBonus = weights.programBonus
        materialBonus = weights.materialBonus
        blogBonus = weights.blogBonus
        maxPerGroup = weights.maxPerGroup
    }

    fun weights() = LetsCareerContentRecommendWeights(
        roleScore = roleScore,
        fieldScore = fieldScore,
        topicScore = topicScore,
        maxTopicMatches = maxTopicMatches,
        programBonus = programBonus,
        materialBonus = materialBonus,
        blogBonus = blogBonus,
        maxPerGroup = maxPerGroup,
    )
}
