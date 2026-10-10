package com.ogonggo.core.recruitmentpost.implement.dto

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationProgressStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostComment
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostContactMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostMetric
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPosition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostApplicationRow
import java.time.LocalDate
import java.time.LocalDateTime

data class RecruitmentPostAppendDto(
    val authorUserId: Long,
    val title: String,
    val recruitmentType: RecruitmentPostType,
    val capacity: Int,
    val progressMethod: RecruitmentPostProgressMethod,
    val activityDurationMonths: Int,
    val technologyStacks: List<String>,
    val summary: String,
    val content: String,
    val eligibilityAndSelectionProcess: String?,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
    val positions: List<RecruitmentPostPosition>,
    val contactMethod: RecruitmentPostContactMethod,
    val contactValue: String,
) {
    fun toEntity(): RecruitmentPost = RecruitmentPost(
        authorUserId = authorUserId,
        title = title,
        recruitmentType = recruitmentType,
        capacity = capacity,
        progressMethod = progressMethod,
        activityDurationMonths = activityDurationMonths,
        technologyStacks = technologyStacks,
        summary = summary,
        content = content,
        eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
        recruitmentStartDate = recruitmentStartDate,
        recruitmentEndDate = recruitmentEndDate,
        positions = positions,
        contactMethod = contactMethod,
        contactValue = contactValue,
    )
}

data class RecruitmentPostDraftAppendDto(
    val authorUserId: Long,
    val title: String,
    val recruitmentType: RecruitmentPostType?,
    val capacity: Int?,
    val progressMethod: RecruitmentPostProgressMethod?,
    val activityDurationMonths: Int?,
    val technologyStacks: List<String>,
    val summary: String?,
    val content: String?,
    val eligibilityAndSelectionProcess: String?,
    val recruitmentStartDate: LocalDate?,
    val recruitmentEndDate: LocalDate?,
    val positions: List<RecruitmentPostPosition>,
    val contactMethod: RecruitmentPostContactMethod?,
    val contactValue: String?,
) {
    fun toEntity(): RecruitmentPost = RecruitmentPost(
        authorUserId = authorUserId,
        title = title,
        recruitmentType = recruitmentType,
        capacity = capacity,
        progressMethod = progressMethod,
        activityDurationMonths = activityDurationMonths,
        technologyStacks = technologyStacks,
        summary = summary,
        content = content,
        eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
        recruitmentStartDate = recruitmentStartDate,
        recruitmentEndDate = recruitmentEndDate,
        positions = positions,
        contactMethod = contactMethod,
        contactValue = contactValue,
        publicationStatus = RecruitmentPostPublicationStatus.DRAFT,
    )
}

data class RecruitmentPostUpdateDto(
    val title: String,
    val recruitmentType: RecruitmentPostType?,
    val capacity: Int?,
    val progressMethod: RecruitmentPostProgressMethod?,
    val activityDurationMonths: Int?,
    val technologyStacks: List<String>,
    val summary: String?,
    val content: String?,
    val eligibilityAndSelectionProcess: String?,
    val recruitmentStartDate: LocalDate?,
    val recruitmentEndDate: LocalDate?,
    val positions: List<RecruitmentPostPosition>,
    val contactMethod: RecruitmentPostContactMethod?,
    val contactValue: String?,
)

data class RecruitmentPostListFilterDto(
    val recruitmentTypes: Set<RecruitmentPostType> = emptySet(),
    val progressMethods: Set<RecruitmentPostProgressMethod> = emptySet(),
    val recruitmentStatuses: Set<RecruitmentPostRecruitmentStatus> = emptySet(),
    val positions: Set<RecruitmentPostPosition> = emptySet(),
)

data class RecruitmentPostPageDto(
    val posts: List<RecruitmentPost>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class RecruitmentPostManagementPageDto(
    val posts: List<RecruitmentPost>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class RecruitmentPostBookmarkItemDto(
    val post: RecruitmentPost,
)

data class RecruitmentPostBookmarkPageDto(
    val items: List<RecruitmentPostBookmarkItemDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class RecruitmentPostApplicationPageDto(
    val items: List<RecruitmentPostApplicationItemDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val countsByRecruitmentType: Map<RecruitmentPostType, Long> = emptyMap(),
)

data class RecruitmentPostApplicationItemDto(
    val postId: Long,
    val title: String,
    val recruitmentType: RecruitmentPostType,
    val recruitmentStatus: RecruitmentPostRecruitmentStatus,
    val recruitmentEndDate: LocalDate,
    val progressMethod: RecruitmentPostProgressMethod = RecruitmentPostProgressMethod.ONLINE,
    val activityDurationMonths: Int = 0,
    val applicationStatus: RecruitmentPostApplicationProgressStatus = RecruitmentPostApplicationProgressStatus.PREPARING,
    val lastClickedAt: LocalDateTime,
    val authorUserId: Long,
) {
    companion object {
        internal fun from(row: RecruitmentPostApplicationRow) =
            RecruitmentPostApplicationItemDto(
                postId = row.postId,
                title = row.title,
                recruitmentType = row.recruitmentType,
                recruitmentStatus = row.recruitmentStatus,
                recruitmentEndDate = row.recruitmentEndDate,
                progressMethod = row.progressMethod,
                activityDurationMonths = row.activityDurationMonths,
                applicationStatus = row.applicationStatus,
                lastClickedAt = row.lastClickedAt,
                authorUserId = row.authorUserId,
            )
    }
}

data class RecruitmentPostCommentPageDto(
    val comments: List<RecruitmentPostComment>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class RecruitmentPostCommentAppendDto(
    val postId: Long,
    val parentId: Long?,
    val userId: Long,
    val content: String,
) {
    fun toEntity(): RecruitmentPostComment = RecruitmentPostComment.create(
        postId = postId,
        parentId = parentId,
        userId = userId,
        content = content,
    )
}

data class RecruitmentPostCommentReportAppendDto(
    val commentId: Long,
    val userId: Long,
    val reason: String?,
)

data class RecruitmentPostMetricDto(
    val viewCount: Long,
    val commentCount: Long,
    val bookmarkCount: Long = 0,
) {
    companion object {
        val EMPTY = RecruitmentPostMetricDto(viewCount = 0, commentCount = 0, bookmarkCount = 0)

        internal fun from(metric: RecruitmentPostMetric): RecruitmentPostMetricDto = RecruitmentPostMetricDto(
            viewCount = metric.viewCount,
            commentCount = metric.commentCount,
            bookmarkCount = metric.bookmarkCount,
        )
    }
}
