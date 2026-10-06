package com.ogonggo.core.servicefeedback.implement

import com.ogonggo.core.paging.validatePageRequest
import com.ogonggo.core.servicefeedback.implement.dto.ServiceFeedbackPageDto
import com.ogonggo.core.servicefeedback.persistence.ServiceFeedbackJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ServiceFeedbackReader internal constructor(
    private val serviceFeedbackRepository: ServiceFeedbackJpaRepository,
) {

    /** 최근에 남긴 의견부터 준다. */
    fun readPage(page: Int, size: Int): ServiceFeedbackPageDto {
        validatePageRequest(page, size)
        val result = serviceFeedbackRepository.findAllByOrderByIdDesc(PageRequest.of(page, size))
        return ServiceFeedbackPageDto(
            serviceFeedbacks = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}
