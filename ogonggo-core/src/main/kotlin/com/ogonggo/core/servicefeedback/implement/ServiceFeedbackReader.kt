package com.ogonggo.core.servicefeedback.implement

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
        require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
        require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
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
