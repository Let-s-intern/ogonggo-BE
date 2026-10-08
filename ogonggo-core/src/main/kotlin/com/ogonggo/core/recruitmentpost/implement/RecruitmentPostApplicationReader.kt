package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.paging.validatePageRequest
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationProgressStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationSortType
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostApplicationJpaRepository
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostApplicationQueryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostApplicationItemDto
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostApplicationPageDto

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
    ): RecruitmentPostApplicationPageDto {
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
        return RecruitmentPostApplicationPageDto(
            items = result.content.map(RecruitmentPostApplicationItemDto::from),
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
