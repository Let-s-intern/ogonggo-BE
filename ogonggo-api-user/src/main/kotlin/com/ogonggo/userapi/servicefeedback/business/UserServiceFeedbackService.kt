package com.ogonggo.userapi.servicefeedback.business

import com.ogonggo.core.servicefeedback.implement.ServiceFeedbackAppender
import com.ogonggo.core.servicefeedback.implement.dto.ServiceFeedbackAppendDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserServiceFeedbackService(
    private val serviceFeedbackAppender: ServiceFeedbackAppender,
) {

    /** 비로그인이면 userId가 없다. 생성된 의견의 식별자를 돌려준다. */
    @Transactional
    fun createServiceFeedback(userId: Long?, command: CreateServiceFeedbackCommand): Long {
        val serviceFeedback = serviceFeedbackAppender.append(
            ServiceFeedbackAppendDto(
                userId = userId,
                satisfaction = command.satisfaction,
                improvement = command.improvement,
            ),
        )
        return checkNotNull(serviceFeedback.id) { "개선 의견 식별자가 없습니다." }
    }
}

/** 두 문항 중 하나 이상은 값이 있다. 비운 문항은 null이다. */
data class CreateServiceFeedbackCommand(
    val satisfaction: String?,
    val improvement: String?,
)
