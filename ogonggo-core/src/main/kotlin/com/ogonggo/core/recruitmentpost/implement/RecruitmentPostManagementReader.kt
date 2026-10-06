package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.paging.validatePageRequest
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostQueryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostManagementPageDto

@Component
class RecruitmentPostManagementReader internal constructor(
    private val queryRepository: RecruitmentPostQueryRepository,
) {

    fun readPage(
        ownerUserId: Long,
        status: RecruitmentPostManagementStatus,
        recruitmentStatus: RecruitmentPostRecruitmentStatus?,
        applicationStatus: RecruitmentPostApplicationStatus?,
        recruitmentType: RecruitmentPostType?,
        keyword: String?,
        page: Int,
        size: Int,
        sort: RecruitmentPostManagementSortType,
    ): RecruitmentPostManagementPageDto {
        validatePageRequest(page, size)

        val result = queryRepository.findOwnedPage(
            ownerUserId = ownerUserId,
            status = status,
            recruitmentStatus = recruitmentStatus,
            applicationStatus = applicationStatus,
            recruitmentType = recruitmentType,
            keyword = keyword,
            pageable = PageRequest.of(page, size),
            sort = sort,
        )
        return RecruitmentPostManagementPageDto(
            posts = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}
