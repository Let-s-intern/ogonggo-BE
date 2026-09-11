package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.domain.Post
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.implement.RecruitmentPostPage
import com.ogonggo.core.community.implement.RecruitmentPostListFilter
import java.time.LocalDate

data class RecruitmentPostListQuery(
    val page: Int,
    val size: Int,
    val sortType: RecruitmentPostSortType,
    val filter: RecruitmentPostListFilter,
)

data class RecruitmentPostPageResult(
    val items: List<RecruitmentPostSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class RecruitmentPostSummary(
    val id: Long,
    val title: String,
    val recruitmentType: RecruitmentType,
    val progressMethod: ProgressMethod,
    val recruitmentStatus: RecruitmentStatus,
    val capacity: Int,
    val activityDurationMonths: Int,
    val technologyStacks: List<String>,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
) {
    companion object {
        internal fun from(post: Post): RecruitmentPostSummary = RecruitmentPostSummary(
            id = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." },
            title = post.title,
            recruitmentType = post.recruitmentType,
            progressMethod = post.progressMethod,
            recruitmentStatus = post.recruitmentStatus,
            capacity = post.capacity,
            activityDurationMonths = post.activityDurationMonths,
            technologyStacks = post.technologyStacks.toList(),
            recruitmentStartDate = post.recruitmentStartDate,
            recruitmentEndDate = post.recruitmentEndDate,
        )
    }
}

internal fun RecruitmentPostPage.toResult(): RecruitmentPostPageResult = RecruitmentPostPageResult(
    items = posts.map(RecruitmentPostSummary::from),
    page = page,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
)
