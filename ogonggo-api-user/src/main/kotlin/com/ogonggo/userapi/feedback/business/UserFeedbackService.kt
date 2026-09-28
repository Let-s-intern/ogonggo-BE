package com.ogonggo.userapi.feedback.business

import com.ogonggo.core.feedback.implement.FeedbackAppender
import com.ogonggo.core.feedback.implement.dto.FeedbackAppendDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserFeedbackService(
    private val feedbackAppender: FeedbackAppender,
) {

    /** 비로그인이면 userId가 없다. 생성된 의견의 식별자를 돌려준다. */
    @Transactional
    fun createFeedback(userId: Long?, command: CreateFeedbackCommand): Long {
        val feedback = feedbackAppender.append(
            FeedbackAppendDto(
                userId = userId,
                satisfaction = command.satisfaction,
                improvement = command.improvement,
            ),
        )
        return checkNotNull(feedback.id) { "개선 의견 식별자가 없습니다." }
    }
}

/** 두 문항 중 하나 이상은 값이 있다. 비운 문항은 null이다. */
data class CreateFeedbackCommand(
    val satisfaction: String?,
    val improvement: String?,
)
