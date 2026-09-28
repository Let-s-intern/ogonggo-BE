package com.ogonggo.core.feedback.implement

import com.ogonggo.core.feedback.implement.dto.FeedbackPageDto
import com.ogonggo.core.feedback.persistence.FeedbackJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class FeedbackReader internal constructor(
    private val feedbackRepository: FeedbackJpaRepository,
) {

    /** 최근에 남긴 의견부터 준다. */
    fun readPage(page: Int, size: Int): FeedbackPageDto {
        require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
        require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
        val result = feedbackRepository.findAllByOrderByIdDesc(PageRequest.of(page, size))
        return FeedbackPageDto(
            feedbacks = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}
