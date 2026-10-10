package com.ogonggo.adminapi.concern.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.implement.dto.ConcernMetricDto
import com.ogonggo.core.user.implement.dto.UserProfileDto
import java.time.LocalDateTime

data class AdminConcernPageResult(
    val items: List<AdminConcernSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class AdminConcernSummary(
    val id: Long,
    val category: ConcernCategory,
    val title: String,
    val viewCount: Long,
    val commentCount: Long,
    val hasOfficialComment: Boolean,
    val visibility: AdminContentVisibility,
    val authorUserId: Long,
    val authorNickname: String?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(
            concern: Concern,
            metric: ConcernMetricDto,
            hasOfficialComment: Boolean,
            authorProfile: UserProfileDto?,
        ): AdminConcernSummary = AdminConcernSummary(
            id = concern.requiredId(),
            category = concern.category,
            title = concern.title,
            viewCount = metric.viewCount,
            commentCount = metric.commentCount,
            hasOfficialComment = hasOfficialComment,
            visibility = concern.visibility(),
            authorUserId = concern.authorUserId,
            authorNickname = authorProfile?.nickname,
            registeredAt = concern.createdAt,
        )
    }
}

data class AdminConcernDetail(
    val id: Long,
    val category: ConcernCategory,
    val title: String,
    val content: String,
    val viewCount: Long,
    val commentCount: Long,
    val hasOfficialComment: Boolean,
    val visibility: AdminContentVisibility,
    val authorUserId: Long,
    val authorNickname: String?,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(
            concern: Concern,
            metric: ConcernMetricDto,
            hasOfficialComment: Boolean,
            authorProfile: UserProfileDto?,
        ): AdminConcernDetail = AdminConcernDetail(
            id = concern.requiredId(),
            category = concern.category,
            title = concern.title,
            content = concern.content,
            viewCount = metric.viewCount,
            commentCount = metric.commentCount,
            hasOfficialComment = hasOfficialComment,
            visibility = concern.visibility(),
            authorUserId = concern.authorUserId,
            authorNickname = authorProfile?.nickname,
            registeredAt = concern.createdAt,
            updatedAt = concern.updatedAt,
        )
    }
}

internal fun Concern.requiredId(): Long = checkNotNull(id) { "고민글 식별자가 없습니다." }

internal fun Concern.visibility(): AdminContentVisibility = AdminContentVisibility.of(published = !hidden)
