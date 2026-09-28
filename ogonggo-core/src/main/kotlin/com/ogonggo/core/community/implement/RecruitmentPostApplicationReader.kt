package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.domain.RecruitmentApplicationProgressStatus
import com.ogonggo.core.community.domain.RecruitmentApplicationSortType
import com.ogonggo.core.community.persistence.RecruitmentPostApplicationJpaRepository
import com.ogonggo.core.community.persistence.RecruitmentPostApplicationQueryRepository
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
    fun readActiveStatus(postId: Long, userId: Long): RecruitmentApplicationProgressStatus? =
        applicationRepository.findByPostIdAndUserIdAndDeletedAtIsNull(postId, userId)?.applicationStatus

    fun readPage(
        userId: Long,
        recruitmentStatus: RecruitmentStatus?,
        recruitmentType: RecruitmentType?,
        keyword: String?,
        page: Int,
        size: Int,
        applicationStatus: RecruitmentApplicationProgressStatus? = null,
        sort: RecruitmentApplicationSortType = RecruitmentApplicationSortType.LATEST,
    ): RecruitmentPostApplicationPage {
        validatePageRequest(page, size)
        val result = applicationQueryRepository.findPage(
            userId = userId,
            publicationStatus = PublicationStatus.PUBLISHED,
            recruitmentStatus = recruitmentStatus,
            applicationStatus = applicationStatus,
            recruitmentType = recruitmentType,
            keyword = keyword,
            sort = sort,
            pageable = PageRequest.of(page, size),
        )
        val queriedCountsByRecruitmentType = applicationQueryRepository.countByRecruitmentType(
            userId = userId,
            publicationStatus = PublicationStatus.PUBLISHED,
            recruitmentStatus = recruitmentStatus,
            keyword = keyword,
            applicationStatus = applicationStatus,
        )
        val countsByRecruitmentType = mapOf(
            RecruitmentType.SIDE_PROJECT to (queriedCountsByRecruitmentType[RecruitmentType.SIDE_PROJECT] ?: 0L),
            RecruitmentType.STUDY to (queriedCountsByRecruitmentType[RecruitmentType.STUDY] ?: 0L),
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
    val countsByRecruitmentType: Map<RecruitmentType, Long> = emptyMap(),
)

data class RecruitmentPostApplicationItem(
    val postId: Long,
    val title: String,
    val recruitmentType: RecruitmentType,
    val recruitmentStatus: RecruitmentStatus,
    val recruitmentEndDate: LocalDate,
    val progressMethod: ProgressMethod = ProgressMethod.ONLINE,
    val activityDurationMonths: Int = 0,
    val applicationStatus: RecruitmentApplicationProgressStatus = RecruitmentApplicationProgressStatus.PREPARING,
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
