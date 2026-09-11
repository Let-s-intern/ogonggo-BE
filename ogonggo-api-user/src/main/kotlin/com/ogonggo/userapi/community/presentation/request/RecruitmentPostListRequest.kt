package com.ogonggo.userapi.community.presentation.request

import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.implement.RecruitmentPostListFilter
import com.ogonggo.userapi.community.business.RecruitmentPostListQuery
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

class RecruitmentPostListRequest {
    @field:Min(1)
    var page: Int = 1

    @field:Min(1)
    @field:Max(100)
    var size: Int = 10

    var sort: RecruitmentPostSortType = RecruitmentPostSortType.LATEST
    var recruitmentTypes: List<RecruitmentType> = emptyList()
    var progressMethods: List<ProgressMethod> = emptyList()
    var recruitmentStatuses: List<RecruitmentStatus> = emptyList()
    var positions: List<RecruitmentPosition> = emptyList()

    fun toQuery(): RecruitmentPostListQuery = RecruitmentPostListQuery(
        page = page - 1,
        size = size,
        sortType = sort,
        filter = RecruitmentPostListFilter(
            recruitmentTypes = recruitmentTypes.toSet(),
            progressMethods = progressMethods.toSet(),
            recruitmentStatuses = recruitmentStatuses.toSet(),
            positions = positions.toSet(),
        ),
    )
}
