package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.persistence.RecruitmentPostApplicationQueryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class RecruitmentPostApplicationReader internal constructor(
    private val applicationQueryRepository: RecruitmentPostApplicationQueryRepository,
) {

    fun readPage(
        userId: Long,
        recruitmentStatus: RecruitmentStatus?,
        recruitmentType: RecruitmentType?,
        keyword: String?,
        page: Int,
        size: Int,
    ): RecruitmentPostApplicationPage {
        validatePageRequest(page, size)
        val result = applicationQueryRepository.findPage(
            userId = userId,
            publicationStatus = PublicationStatus.PUBLISHED,
            recruitmentStatus = recruitmentStatus,
            recruitmentType = recruitmentType,
            keyword = keyword,
            pageable = PageRequest.of(page, size),
        )
        return RecruitmentPostApplicationPage(
            items = result.content.map(RecruitmentPostApplicationItem::from),
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    fun countByPostIds(postIds: Collection<Long>): Map<Long, Long> =
        applicationQueryRepository.countByPostIds(postIds)
}

data class RecruitmentPostApplicationPage(
    val items: List<RecruitmentPostApplicationItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class RecruitmentPostApplicationItem(
    val postId: Long,
    val title: String,
    val recruitmentType: RecruitmentType,
    val recruitmentStatus: RecruitmentStatus,
    val recruitmentEndDate: LocalDate,
    val lastClickedAt: LocalDateTime,
    val authorUserId: Long,
) {
    companion object {
        internal fun from(row: com.ogonggo.core.community.persistence.RecruitmentPostApplicationRow) =
            RecruitmentPostApplicationItem(
                postId = row.postId,
                title = row.title,
                recruitmentType = row.recruitmentType,
                recruitmentStatus = row.recruitmentStatus,
                recruitmentEndDate = row.recruitmentEndDate,
                lastClickedAt = row.lastClickedAt,
                authorUserId = row.authorUserId,
            )
    }
}

private fun validatePageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
