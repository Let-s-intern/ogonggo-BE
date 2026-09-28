package com.ogonggo.core.feedback.implement

import com.ogonggo.core.feedback.domain.Feedback
import com.ogonggo.core.feedback.implement.dto.FeedbackAppendDto
import com.ogonggo.core.feedback.persistence.FeedbackJpaRepository
import org.springframework.stereotype.Component

@Component
class FeedbackAppender internal constructor(
    private val feedbackRepository: FeedbackJpaRepository,
) {

    fun append(dto: FeedbackAppendDto): Feedback =
        feedbackRepository.save(
            Feedback(
                userId = dto.userId,
                satisfaction = dto.satisfaction,
                improvement = dto.improvement,
            ),
        )
}
