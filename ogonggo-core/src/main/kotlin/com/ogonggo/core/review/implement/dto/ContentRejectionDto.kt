package com.ogonggo.core.review.implement.dto

import com.ogonggo.core.review.domain.ReviewContentType
import java.time.LocalDateTime

/** 반려 기록에 콘텐츠의 제목과 회사명을 붙여 읽은 값이다. 콘텐츠가 삭제됐어도 제목은 남는다. */
data class ContentRejectionDto(
    val contentType: ReviewContentType,
    val contentId: Long,
    val title: String,
    val companyName: String,
    val reason: String,
    val rejectedAt: LocalDateTime,
    val reasonUpdatedAt: LocalDateTime?,
    val contentExists: Boolean,
)

data class ContentRejectionPageDto(
    val rejections: List<ContentRejectionDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
