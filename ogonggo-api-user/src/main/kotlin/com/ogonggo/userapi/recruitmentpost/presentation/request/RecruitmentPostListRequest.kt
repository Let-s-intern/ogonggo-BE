package com.ogonggo.userapi.recruitmentpost.presentation.request

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPosition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostListFilter
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostListQuery
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

class RecruitmentPostListRequest {
    @field:Schema(defaultValue = "1", minimum = "1")
    @field:Min(1)
    var page: Int = 1

    @field:Schema(defaultValue = "10", minimum = "1", maximum = "100")
    @field:Min(1)
    @field:Max(100)
    var size: Int = 10

    var sort: RecruitmentPostSortType = RecruitmentPostSortType.LATEST
    var recruitmentTypes: List<RecruitmentPostType> = emptyList()
    var progressMethods: List<RecruitmentPostProgressMethod> = emptyList()
    var recruitmentStatuses: List<RecruitmentPostRecruitmentStatus> = emptyList()
    var positions: List<RecruitmentPostPosition> = emptyList()

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
