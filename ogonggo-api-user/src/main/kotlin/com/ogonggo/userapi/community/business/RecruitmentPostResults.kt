package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.implement.RecruitmentPostCursor
import com.ogonggo.core.community.implement.RecruitmentPostCursorPage
import com.ogonggo.core.community.implement.RecruitmentPostListFilter
import com.ogonggo.core.community.implement.PostMetricDto
import java.time.LocalDate

data class RecruitmentPostListQuery(
    val cursor: RecruitmentPostCursor?,
    val size: Int,
    val sortType: RecruitmentPostSortType,
    val filter: RecruitmentPostListFilter,
)

data class RecruitmentPostCursorPageResult(
    val items: List<RecruitmentPostSummary>,
    val hasNext: Boolean,
    val nextCursor: RecruitmentPostCursor?,
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
    val viewCount: Long = 0,
    val commentCount: Long = 0,
) {
    companion object {
        internal fun from(post: RecruitmentPost, metric: PostMetricDto): RecruitmentPostSummary = RecruitmentPostSummary(
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
            viewCount = metric.viewCount,
            commentCount = metric.commentCount,
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
) {
    companion object {
        internal fun from(post: RecruitmentPost, metric: PostMetricDto): RecruitmentPostDetailResult = RecruitmentPostDetailResult(
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
            viewCount = metric.viewCount,
            commentCount = metric.commentCount,
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

internal fun RecruitmentPostCursorPage.toResult(
    metrics: Map<Long, PostMetricDto>,
    queryKey: String,
): RecruitmentPostCursorPageResult = RecruitmentPostCursorPageResult(
    items = posts.map { post ->
        RecruitmentPostSummary.from(
            post,
            metrics[checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." }] ?: PostMetricDto.EMPTY,
        )
    },
    hasNext = hasNext,
    nextCursor = if (hasNext) posts.lastOrNull()?.let { post ->
        val postId = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." }
        val metric = metrics[postId] ?: PostMetricDto.EMPTY
        when (sortType) {
            RecruitmentPostSortType.LATEST -> RecruitmentPostCursor(
                queryKey = queryKey,
                sortType = sortType,
                id = postId,
            )
            RecruitmentPostSortType.DEADLINE -> RecruitmentPostCursor(
                queryKey = queryKey,
                sortType = sortType,
                deadline = post.recruitmentEndDate,
                id = postId,
            )
            RecruitmentPostSortType.VIEW_COUNT,
            RecruitmentPostSortType.COMMENT_COUNT,
            -> RecruitmentPostCursor(
                queryKey = queryKey,
                sortType = sortType,
                metricCount = if (sortType == RecruitmentPostSortType.VIEW_COUNT) metric.viewCount else metric.commentCount,
                id = postId,
            )
        }
    } else null,
)
