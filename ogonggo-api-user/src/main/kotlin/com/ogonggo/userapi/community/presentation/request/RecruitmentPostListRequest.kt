package com.ogonggo.userapi.community.presentation.request

import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.implement.RecruitmentPostListFilter
import com.ogonggo.core.community.implement.RecruitmentPostCursor
import com.ogonggo.core.community.implement.cursorKey
import com.ogonggo.userapi.community.business.RecruitmentPostListQuery
import com.ogonggo.userapi.error.InvalidRequestParameterException
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

class RecruitmentPostListRequest {
    var cursor: String? = null

    @field:Min(1)
    @field:Max(100)
    var size: Int = 10

    var sort: RecruitmentPostSortType = RecruitmentPostSortType.LATEST
    var recruitmentTypes: List<RecruitmentType> = emptyList()
    var progressMethods: List<ProgressMethod> = emptyList()
    var recruitmentStatuses: List<RecruitmentStatus> = emptyList()
    var positions: List<RecruitmentPosition> = emptyList()

    fun toQuery(cursor: RecruitmentPostCursor?): RecruitmentPostListQuery {
        val filter = RecruitmentPostListFilter(
            recruitmentTypes = recruitmentTypes.toSet(),
            progressMethods = progressMethods.toSet(),
            recruitmentStatuses = recruitmentStatuses.toSet(),
            positions = positions.toSet(),
        )
        if (cursor != null && (cursor.sortType != sort || cursor.queryKey != filter.cursorKey(sort))) {
            throw InvalidRequestParameterException("cursor", "현재 정렬·필터와 일치하지 않는 모집글 커서입니다.")
        }
        return RecruitmentPostListQuery(
            cursor = cursor,
            size = size,
            sortType = sort,
            filter = filter,
        )
    }
}
