package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPostApplication
import com.ogonggo.core.community.persistence.RecruitmentPostApplicationJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RecruitmentPostApplicationManager internal constructor(
    private val applicationRepository: RecruitmentPostApplicationJpaRepository,
) {

    /** 같은 사용자·모집글 이력은 하나로 유지하고 재접근 시 최근 시각만 갱신한다. */
    fun recordClick(
        postId: Long,
        userId: Long,
        clickedAt: LocalDateTime,
    ) {
        val application = applicationRepository.findByPostIdAndUserId(postId, userId)
            ?: RecruitmentPostApplication(
                postId = postId,
                userId = userId,
                firstClickedAt = clickedAt,
                lastClickedAt = clickedAt,
            )

        application.recordClick(clickedAt)
        applicationRepository.save(application)
    }
}
