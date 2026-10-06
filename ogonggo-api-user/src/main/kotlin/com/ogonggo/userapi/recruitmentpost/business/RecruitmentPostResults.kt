package com.ogonggo.userapi.recruitmentpost.business

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostContactMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPosition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostPageDto
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostListFilterDto
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostMetricDto
import com.ogonggo.core.user.implement.dto.UserProfileDto
import java.time.LocalDate

data class RecruitmentPostListQuery(
    val page: Int,
    val size: Int,
    val sortType: RecruitmentPostSortType,
    val filter: RecruitmentPostListFilterDto,
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
    val recruitmentType: RecruitmentPostType,
    val progressMethod: RecruitmentPostProgressMethod,
    val recruitmentStatus: RecruitmentPostRecruitmentStatus,
    val capacity: Int,
    val activityDurationMonths: Int,
    val technologyStacks: List<String>,
    val positions: List<RecruitmentPostPosition>,
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
            metric: RecruitmentPostMetricDto,
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
            positions = post.positions.toList(),
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
    val recruitmentType: RecruitmentPostType,
    val recruitmentStatus: RecruitmentPostRecruitmentStatus,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
    val progressMethod: RecruitmentPostProgressMethod,
    val capacity: Int,
    val activityDurationMonths: Int,
    val technologyStacks: List<String>,
    val positions: List<RecruitmentPostPosition>,
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
            metric: RecruitmentPostMetricDto,
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
    val method: RecruitmentPostContactMethod,
    val value: String,
)

internal fun RecruitmentPostPageDto.toResult(
    metrics: Map<Long, RecruitmentPostMetricDto>,
    authorsByUserId: Map<Long, RecruitmentPostAuthorResult>,
    bookmarkedPostIds: Set<Long> = emptySet(),
    applicationCounts: Map<Long, Long> = emptyMap(),
    includeBookmarkCount: Boolean = true,
): RecruitmentPostPageResult = RecruitmentPostPageResult(
    items = posts.map { post ->
        RecruitmentPostSummary.from(
            post,
            metrics[checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." }] ?: RecruitmentPostMetricDto.EMPTY,
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
