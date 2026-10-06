package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostQueryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

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
    ): RecruitmentPostManagementPage {
        require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
        require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }

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
        return RecruitmentPostManagementPage(
            posts = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

data class RecruitmentPostManagementPage(
    val posts: List<RecruitmentPost>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
