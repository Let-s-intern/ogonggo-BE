package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.domain.Post
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
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

data class RecruitmentPostDetailResult(
    val id: Long,
    val author: RecruitmentPostAuthorResult,
    val title: String,
    val recruitmentType: RecruitmentType,
    val recruitmentStatus: RecruitmentStatus,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
    val progressMethod: ProgressMethod,
    val capacity: Int,
    val activityDurationMonths: Int,
    val technologyStacks: List<String>,
    val positions: List<RecruitmentPosition>,
    val contact: RecruitmentPostContactResult,
    val summary: String,
    val content: String,
    val eligibilityAndSelectionProcess: String?,
) {
    companion object {
        internal fun from(post: Post): RecruitmentPostDetailResult = RecruitmentPostDetailResult(
            id = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." },
            author = RecruitmentPostAuthorResult(userId = post.authorUserId),
            title = post.title,
            recruitmentType = post.recruitmentType,
            recruitmentStatus = post.recruitmentStatus,
            recruitmentStartDate = post.recruitmentStartDate,
            recruitmentEndDate = post.recruitmentEndDate,
            progressMethod = post.progressMethod,
            capacity = post.capacity,
            activityDurationMonths = post.activityDurationMonths,
            technologyStacks = post.technologyStacks.toList(),
            positions = post.positions.toList(),
            contact = RecruitmentPostContactResult(
                method = post.contactMethod,
                value = post.contactValue,
            ),
            summary = post.summary,
            content = post.content,
            eligibilityAndSelectionProcess = post.eligibilityAndSelectionProcess,
        )
    }
}

data class RecruitmentPostAuthorResult(
    val userId: Long,
)

data class RecruitmentPostContactResult(
    val method: ContactMethod,
    val value: String,
)

internal fun RecruitmentPostPage.toResult(): RecruitmentPostPageResult = RecruitmentPostPageResult(
    items = posts.map(RecruitmentPostSummary::from),
    page = page,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
)
