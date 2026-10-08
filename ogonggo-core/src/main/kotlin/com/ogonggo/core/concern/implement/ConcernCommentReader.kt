package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.ConcernComment
import com.ogonggo.core.concern.error.ConcernCommentErrorCode
import com.ogonggo.core.concern.implement.dto.ConcernCommentPageDto
import com.ogonggo.core.concern.persistence.ConcernCommentJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.paging.validatePageRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ConcernCommentReader internal constructor(
    private val commentRepository: ConcernCommentJpaRepository,
) {

    /** 삭제된 답변도 읽는다. 남은 답글을 더 볼 때 쓴다. */
    fun readRoot(concernId: Long, commentId: Long): ConcernComment =
        commentRepository.findRootByIdAndConcernId(commentId, concernId)
            ?: throw EntityNotFoundException(ConcernCommentErrorCode.CONCERN_COMMENT_NOT_FOUND)

    fun readParent(concernId: Long, parentId: Long): ConcernComment =
        commentRepository.findActiveByIdAndConcernId(parentId, concernId)
            ?: throw EntityNotFoundException(ConcernCommentErrorCode.CONCERN_COMMENT_PARENT_NOT_FOUND)

    fun read(concernId: Long, commentId: Long): ConcernComment =
        commentRepository.findActiveByIdAndConcernId(commentId, concernId)
            ?: throw EntityNotFoundException(ConcernCommentErrorCode.CONCERN_COMMENT_NOT_FOUND)

    fun readForUpdate(concernId: Long, commentId: Long): ConcernComment =
        commentRepository.findActiveByIdAndConcernIdForUpdate(commentId, concernId)
            ?: throw EntityNotFoundException(ConcernCommentErrorCode.CONCERN_COMMENT_NOT_FOUND)

    fun readRootPage(concernId: Long, page: Int, size: Int): ConcernCommentPageDto {
        validatePageRequest(page, size)
        return commentRepository.findRootComments(concernId, PageRequest.of(page, size)).toPageDto()
    }

    fun readReplyPage(concernId: Long, parentId: Long, page: Int, size: Int): ConcernCommentPageDto {
        validatePageRequest(page, size)
        return commentRepository.findReplies(concernId, parentId, PageRequest.of(page, size)).toPageDto()
    }

    /** 답변마다 앞쪽 답글 [size]개와 전체 답글 수를 한 번에 읽는다. 답글이 없는 답변은 결과에 없다. */
    fun readReplyPreviews(
        concernId: Long,
        parentIds: Collection<Long>,
        size: Int,
    ): Map<Long, ConcernCommentPageDto> {
        if (parentIds.isEmpty()) return emptyMap()

        val replies = commentRepository.findRepliesByParentIds(concernId, parentIds, size)
        val replyCounts = commentRepository.countRepliesByParentIds(concernId, parentIds).associate { it.id to it.count }
        return replies
            .groupBy { checkNotNull(it.parentId) { "답글의 부모 댓글 식별자가 없습니다." } }
            .mapValues { (parentId, comments) ->
                val totalElements = replyCounts[parentId] ?: comments.size.toLong()
                ConcernCommentPageDto(
                    comments = comments,
                    page = 0,
                    size = size,
                    totalElements = totalElements,
                    totalPages = ((totalElements + size - 1) / size).toInt(),
                )
            }
    }

    /** 삭제되지 않은 관리자 답변이 하나라도 있는 고민글 식별자를 읽는다. */
    fun readConcernIdsWithOfficialComment(concernIds: Collection<Long>): Set<Long> {
        if (concernIds.isEmpty()) return emptySet()
        return commentRepository.findConcernIdsWithOfficialComment(concernIds).toSet()
    }

    private fun Page<ConcernComment>.toPageDto() = ConcernCommentPageDto(
        comments = content,
        page = number,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )
}
