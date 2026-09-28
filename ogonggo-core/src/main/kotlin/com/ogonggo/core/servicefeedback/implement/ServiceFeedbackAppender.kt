package com.ogonggo.core.servicefeedback.implement

import com.ogonggo.core.servicefeedback.domain.ServiceFeedback
import com.ogonggo.core.servicefeedback.implement.dto.ServiceFeedbackAppendDto
import com.ogonggo.core.servicefeedback.persistence.ServiceFeedbackJpaRepository
import org.springframework.stereotype.Component

@Component
class ServiceFeedbackAppender internal constructor(
    private val serviceFeedbackRepository: ServiceFeedbackJpaRepository,
) {

    fun append(dto: ServiceFeedbackAppendDto): ServiceFeedback =
        serviceFeedbackRepository.save(
            ServiceFeedback(
                userId = dto.userId,
                satisfaction = dto.satisfaction,
                improvement = dto.improvement,
            ),
        )
}
