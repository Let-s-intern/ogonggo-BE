package com.ogonggo.core.job.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 운영자가 고른 오늘의 공고 한 건이다. 삭제되지 않은 행 전체가 지금의 오늘의 공고 목록이다.
 * 추천 문구는 오늘의 공고 카드에만 보여 주는 운영 문구라 공고가 아니라 이 행에 둔다.
 * 목록을 바꾸면 기존 행을 소프트 삭제하고 새 행을 넣으므로 같은 공고의 지난 행이 여러 개 남을 수 있어 유니크 제약을 두지 않는다.
 */
@Entity
@Table(
    name = "today_jobs",
    indexes = [Index(name = "idx_today_jobs_active", columnList = "deleted_at, display_order")],
)
internal class TodayJob(
    @Column(name = "job_id", nullable = false)
    val jobId: Long, /* 오늘의 공고로 고른 채용공고 식별자 */

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int, /* 오늘의 공고 노출 순서 */

    @Column(name = "recommendation_title", nullable = false, length = RECOMMENDATION_TITLE_MAX_LENGTH)
    val recommendationTitle: String, /* 카드에 굵게 보여 주는 추천 문구 제목 */

    @Column(name = "recommendation_description", nullable = false, length = RECOMMENDATION_DESCRIPTION_MAX_LENGTH)
    val recommendationDescription: String, /* 추천 문구 제목 아래에 보여 주는 설명 */
) : BaseTimeEntity() {

    init {
        require(jobId > 0) { "채용공고 식별자는 양수여야 합니다." }
        require(displayOrder >= 0) { "노출 순서는 음수일 수 없습니다." }
        require(recommendationTitle.isNotBlank() && recommendationTitle.length <= RECOMMENDATION_TITLE_MAX_LENGTH) {
            "추천 문구 제목은 1자 이상 ${RECOMMENDATION_TITLE_MAX_LENGTH}자 이하여야 합니다."
        }
        require(
            recommendationDescription.isNotBlank() &&
                recommendationDescription.length <= RECOMMENDATION_DESCRIPTION_MAX_LENGTH,
        ) {
            "추천 문구 설명은 1자 이상 ${RECOMMENDATION_DESCRIPTION_MAX_LENGTH}자 이하여야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 오늘의 공고 식별자 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 오늘의 공고에서 뺀 일시 */
        protected set

    companion object {
        /** 카드 폭에서 한 줄 남짓 들어가는 길이다. */
        const val RECOMMENDATION_TITLE_MAX_LENGTH = 30
        const val RECOMMENDATION_DESCRIPTION_MAX_LENGTH = 50
    }
}
