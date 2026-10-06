package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationProgressStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationSortType
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostApplicationJpaRepository
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostApplicationQueryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class RecruitmentPostApplicationReader internal constructor(
    private val applicationQueryRepository: RecruitmentPostApplicationQueryRepository,
    private val applicationRepository: RecruitmentPostApplicationJpaRepository,
) {

    /** 삭제되지 않은 지원 이력의 상태를 읽는다. 이력이 없으면 null이다. */
    fun readActiveStatus(postId: Long, userId: Long): RecruitmentPostApplicationProgressStatus? =
        applicationRepository.findByPostIdAndUserIdAndDeletedAtIsNull(postId, userId)?.applicationStatus

    fun readPage(
        userId: Long,
        recruitmentStatus: RecruitmentPostRecruitmentStatus?,
        recruitmentType: RecruitmentPostType?,
        keyword: String?,
        page: Int,
        size: Int,
        applicationStatus: RecruitmentPostApplicationProgressStatus? = null,
        sort: RecruitmentPostApplicationSortType = RecruitmentPostApplicationSortType.LATEST,
    ): RecruitmentPostApplicationPage {
        validatePageRequest(page, size)
        val result = applicationQueryRepository.findPage(
            userId = userId,
            publicationStatus = RecruitmentPostPublicationStatus.PUBLISHED,
            recruitmentStatus = recruitmentStatus,
            applicationStatus = applicationStatus,
            recruitmentType = recruitmentType,
            keyword = keyword,
            sort = sort,
            pageable = PageRequest.of(page, size),
        )
        val queriedCountsByRecruitmentType = applicationQueryRepository.countByRecruitmentType(
            userId = userId,
            publicationStatus = RecruitmentPostPublicationStatus.PUBLISHED,
            recruitmentStatus = recruitmentStatus,
            keyword = keyword,
            applicationStatus = applicationStatus,
        )
        val countsByRecruitmentType = mapOf(
            RecruitmentPostType.SIDE_PROJECT to (queriedCountsByRecruitmentType[RecruitmentPostType.SIDE_PROJECT] ?: 0L),
            RecruitmentPostType.STUDY to (queriedCountsByRecruitmentType[RecruitmentPostType.STUDY] ?: 0L),
        )
        return RecruitmentPostApplicationPage(
            items = result.content.map(RecruitmentPostApplicationItem::from),
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            countsByRecruitmentType = countsByRecruitmentType,
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
    val countsByRecruitmentType: Map<RecruitmentPostType, Long> = emptyMap(),
)

data class RecruitmentPostApplicationItem(
    val postId: Long,
    val title: String,
    val recruitmentType: RecruitmentPostType,
    val recruitmentStatus: RecruitmentPostRecruitmentStatus,
    val recruitmentEndDate: LocalDate,
    val progressMethod: RecruitmentPostProgressMethod = RecruitmentPostProgressMethod.ONLINE,
    val activityDurationMonths: Int = 0,
    val applicationStatus: RecruitmentPostApplicationProgressStatus = RecruitmentPostApplicationProgressStatus.PREPARING,
    val lastClickedAt: LocalDateTime,
    val authorUserId: Long,
) {
    companion object {
        internal fun from(row: com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostApplicationRow) =
            RecruitmentPostApplicationItem(
                postId = row.postId,
                title = row.title,
                recruitmentType = row.recruitmentType,
                recruitmentStatus = row.recruitmentStatus,
                recruitmentEndDate = row.recruitmentEndDate,
                progressMethod = row.progressMethod,
                activityDurationMonths = row.activityDurationMonths,
                applicationStatus = row.applicationStatus,
                lastClickedAt = row.lastClickedAt,
                authorUserId = row.authorUserId,
            )
    }
}

private fun validatePageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
