package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.implement.RecruitmentPostPage
import com.ogonggo.core.community.implement.RecruitmentPostListFilter
import com.ogonggo.core.community.implement.PostMetricDto
import com.ogonggo.core.user.implement.dto.UserProfileDto
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
    val author: RecruitmentPostAuthorResult,
    val title: String,
    val recruitmentType: RecruitmentType,
    val progressMethod: ProgressMethod,
    val recruitmentStatus: RecruitmentStatus,
    val capacity: Int,
    val activityDurationMonths: Int,
    val technologyStacks: List<String>,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
    val viewCount: Long = 0,
    val commentCount: Long = 0,
    val applicationCount: Long = 0,
    val bookmarkCount: Long = 0,
    val bookmarked: Boolean = false,
) {
    companion object {
        internal fun from(
            post: RecruitmentPost,
            metric: PostMetricDto,
            author: RecruitmentPostAuthorResult,
            bookmarked: Boolean = false,
            applicationCount: Long = 0,
        ): RecruitmentPostSummary = RecruitmentPostSummary(
            id = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." },
            author = author,
            title = post.title,
            recruitmentType = checkNotNull(post.recruitmentType),
            progressMethod = checkNotNull(post.progressMethod),
            recruitmentStatus = post.recruitmentStatus,
            capacity = checkNotNull(post.capacity),
            activityDurationMonths = checkNotNull(post.activityDurationMonths),
            technologyStacks = post.technologyStacks.toList(),
            recruitmentStartDate = checkNotNull(post.recruitmentStartDate),
            recruitmentEndDate = checkNotNull(post.recruitmentEndDate),
            viewCount = metric.viewCount,
            commentCount = metric.commentCount,
            applicationCount = applicationCount,
            bookmarkCount = metric.bookmarkCount,
            bookmarked = bookmarked,
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
    val viewCount: Long = 0,
    val commentCount: Long = 0,
    val bookmarkCount: Long = 0,
    val bookmarked: Boolean = false,
) {
    companion object {
        internal fun from(
            post: RecruitmentPost,
            metric: PostMetricDto,
            author: RecruitmentPostAuthorResult,
            bookmarked: Boolean = false,
        ): RecruitmentPostDetailResult = RecruitmentPostDetailResult(
            id = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." },
            author = author,
            title = post.title,
            recruitmentType = checkNotNull(post.recruitmentType),
            recruitmentStatus = post.recruitmentStatus,
            recruitmentStartDate = checkNotNull(post.recruitmentStartDate),
            recruitmentEndDate = checkNotNull(post.recruitmentEndDate),
            progressMethod = checkNotNull(post.progressMethod),
            capacity = checkNotNull(post.capacity),
            activityDurationMonths = checkNotNull(post.activityDurationMonths),
            technologyStacks = post.technologyStacks.toList(),
            positions = post.positions.toList(),
            contact = RecruitmentPostContactResult(
                method = checkNotNull(post.contactMethod),
                value = checkNotNull(post.contactValue),
            ),
            summary = checkNotNull(post.summary),
            content = checkNotNull(post.content),
            eligibilityAndSelectionProcess = post.eligibilityAndSelectionProcess,
            viewCount = metric.viewCount,
            commentCount = metric.commentCount,
            bookmarkCount = metric.bookmarkCount,
            bookmarked = bookmarked,
        )
    }
}

data class RecruitmentPostAuthorResult(
    val userId: Long,
    val nickname: String?,
    val profileImageUrl: String?,
) {
    companion object {
        internal fun from(userId: Long, profile: UserProfileDto?): RecruitmentPostAuthorResult =
            RecruitmentPostAuthorResult(
                userId = userId,
                nickname = profile?.nickname,
                profileImageUrl = profile?.profileImageUrl,
            )
    }
}

data class RecruitmentPostContactResult(
    val method: ContactMethod,
    val value: String,
)

internal fun RecruitmentPostPage.toResult(
    metrics: Map<Long, PostMetricDto>,
    authorsByUserId: Map<Long, RecruitmentPostAuthorResult>,
    bookmarkedPostIds: Set<Long> = emptySet(),
    applicationCounts: Map<Long, Long> = emptyMap(),
    includeBookmarkCount: Boolean = true,
): RecruitmentPostPageResult = RecruitmentPostPageResult(
    items = posts.map { post ->
        RecruitmentPostSummary.from(
            post,
            metrics[checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." }] ?: PostMetricDto.EMPTY,
            authorsByUserId[post.authorUserId] ?: RecruitmentPostAuthorResult.from(post.authorUserId, null),
            bookmarked = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." } in bookmarkedPostIds,
            applicationCount = applicationCounts[checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." }] ?: 0L,
        ).copy(
            bookmarkCount = if (includeBookmarkCount) {
                metrics[checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." }]?.bookmarkCount ?: 0L
            } else {
                0L
            },
        )
    },
    page = page,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
)
