package com.ogonggo.core.feedback.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 사용자가 남긴 서비스 개선 의견이다. 만족스러운 점과 아쉬운 점 두 문항을 받고, 하나 이상 채워야 한다.
 *
 * 로그인 없이도 남길 수 있어 작성자가 없을 수 있다. 한 사용자가 여러 번 남길 수 있으므로 유니크 제약을 두지 않는다.
 * 제출한 의견을 고치거나 지우는 기능이 없어 소프트 삭제 컬럼을 두지 않는다.
 */
@Entity
@Table(name = "feedbacks")
class Feedback internal constructor(
    userId: Long?,
    satisfaction: String?,
    improvement: String?,
) : BaseTimeEntity() {

    init {
        require(userId == null || userId > 0) { "사용자 식별자는 양수여야 합니다." }
        require(!satisfaction.isNullOrBlank() || !improvement.isNullOrBlank()) {
            "만족스러운 점과 아쉬운 점 중 하나 이상 입력해야 합니다."
        }
        require((satisfaction?.length ?: 0) <= MAX_ANSWER_LENGTH && (improvement?.length ?: 0) <= MAX_ANSWER_LENGTH) {
            "각 문항은 ${MAX_ANSWER_LENGTH}자 이하여야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 개선 의견 식별자 */
        protected set

    @Column(name = "user_id")
    val userId: Long? = userId /* 작성한 사용자 식별자. 비로그인 작성이면 없다 */

    @Column(length = MAX_ANSWER_LENGTH)
    val satisfaction: String? = satisfaction /* 이용 중 가장 만족스러운 점 */

    @Column(length = MAX_ANSWER_LENGTH)
    val improvement: String? = improvement /* 아쉬운 점이나 개선됐으면 하는 점 */

    companion object {
        const val MAX_ANSWER_LENGTH = 1000
    }
}
