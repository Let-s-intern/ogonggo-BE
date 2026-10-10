package com.ogonggo.core.concern.implement.dto

import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernComment
import com.ogonggo.core.concern.domain.ConcernMetric

data class ConcernAppendDto(
    val authorUserId: Long,
    val category: ConcernCategory,
    val title: String,
    val content: String,
) {
    internal fun toEntity(): Concern = Concern(
        authorUserId = authorUserId,
        category = category,
        title = title,
        content = content,
    )
}

data class ConcernUpdateDto(
    val category: ConcernCategory,
    val title: String,
    val content: String,
)

data class ConcernPageDto(
    val concerns: List<Concern>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class ConcernMetricDto(
    val viewCount: Long,
    val commentCount: Long,
) {
    companion object {
        val EMPTY = ConcernMetricDto(viewCount = 0, commentCount = 0)

        internal fun from(metric: ConcernMetric): ConcernMetricDto = ConcernMetricDto(
            viewCount = metric.viewCount,
            commentCount = metric.commentCount,
        )
    }
}

data class ConcernCommentAppendDto(
    val concernId: Long,
    val parentId: Long?,
    val userId: Long,
    val content: String,
    val official: Boolean,
) {
    internal fun toEntity(): ConcernComment = ConcernComment(
        concernId = concernId,
        parentId = parentId,
        userId = userId,
        content = content,
        official = official,
    )
}

data class ConcernCommentPageDto(
    val comments: List<ConcernComment>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
