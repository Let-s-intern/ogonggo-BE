package com.ogonggo.adminapi.recruitmentpost.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPosition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostMetricDto
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostPage
import com.ogonggo.core.user.implement.dto.UserProfileDto
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminRecruitmentPostPageResult(
    val items: List<AdminRecruitmentPostSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(
            result: RecruitmentPostPage,
            metrics: Map<Long, RecruitmentPostMetricDto>,
            profiles: Map<Long, UserProfileDto>,
        ): AdminRecruitmentPostPageResult = AdminRecruitmentPostPageResult(
            items = result.posts.map { post ->
                AdminRecruitmentPostSummary.from(
                    post = post,
                    metric = metrics[post.requiredId()] ?: RecruitmentPostMetricDto.EMPTY,
                    authorProfile = profiles[post.authorUserId],
                )
            },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

/** 게시된 적 있는 모집글만 실으므로 게시 필수값은 모두 채워져 있다. */
data class AdminRecruitmentPostSummary(
    val id: Long,
    val title: String,
    val recruitmentType: RecruitmentPostType,
    val progressMethod: RecruitmentPostProgressMethod,
    val capacity: Int,
    val activityDurationMonths: Int,
    val positions: List<RecruitmentPostPosition>,
    val technologyStacks: List<String>,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
    val recruitmentStatus: RecruitmentPostRecruitmentStatus,
    val closedAt: LocalDateTime?,
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
    val visibility: AdminContentVisibility,
    val authorUserId: Long,
    val authorNickname: String?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(
            post: RecruitmentPost,
            metric: RecruitmentPostMetricDto,
            authorProfile: UserProfileDto?,
        ): AdminRecruitmentPostSummary = AdminRecruitmentPostSummary(
            id = post.requiredId(),
            title = post.title,
            recruitmentType = checkNotNull(post.recruitmentType),
            progressMethod = checkNotNull(post.progressMethod),
            capacity = checkNotNull(post.capacity),
            activityDurationMonths = checkNotNull(post.activityDurationMonths),
            positions = post.positions,
            technologyStacks = post.technologyStacks,
            recruitmentStartDate = checkNotNull(post.recruitmentStartDate),
            recruitmentEndDate = checkNotNull(post.recruitmentEndDate),
            recruitmentStatus = post.recruitmentStatus,
            closedAt = post.closedAt,
            viewCount = metric.viewCount,
            bookmarkCount = metric.bookmarkCount,
            commentCount = metric.commentCount,
            visibility = AdminContentVisibility.of(post.publicationStatus == RecruitmentPostPublicationStatus.PUBLISHED),
            authorUserId = post.authorUserId,
            authorNickname = authorProfile?.nickname,
            registeredAt = post.createdAt,
        )
    }
}

internal fun RecruitmentPost.requiredId(): Long = checkNotNull(id) { "모집글 식별자가 없습니다." }
