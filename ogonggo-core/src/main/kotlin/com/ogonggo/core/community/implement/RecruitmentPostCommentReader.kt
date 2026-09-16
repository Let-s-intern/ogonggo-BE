package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPostComment
import com.ogonggo.core.community.error.RecruitmentPostCommentErrorCode
import com.ogonggo.core.community.persistence.RecruitmentPostCommentJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RecruitmentPostCommentReader internal constructor(
    private val commentRepository: RecruitmentPostCommentJpaRepository,
) {
    fun read(commentId: Long): RecruitmentPostComment =
        commentRepository.findByIdOrNull(commentId)
            ?: throw EntityNotFoundException(
                RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_PARENT_NOT_FOUND,
            )

    fun readRoot(postId: Long, commentId: Long): RecruitmentPostComment =
        commentRepository.findByIdAndPostIdAndParentIdIsNull(commentId, postId)
            ?: throw EntityNotFoundException(RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_NOT_FOUND)

    fun readInPost(postId: Long, commentId: Long): RecruitmentPostComment =
        commentRepository.findByIdAndPostId(commentId, postId)
            ?: throw EntityNotFoundException(RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_NOT_FOUND)

    fun readRootPage(
        postId: Long,
        cursor: RecruitmentPostCommentCursor?,
        size: Int,
    ): RecruitmentPostCommentPage = readPage(
        size = size,
        cursor = cursor,
        firstPage = { pageable -> commentRepository.findRootComments(postId, pageable) },
        nextPage = { nextCursor, pageable ->
            commentRepository.findRootCommentsAfter(
                postId = postId,
                createdAt = nextCursor.createdAt,
                commentId = nextCursor.id,
                pageable = pageable,
            )
        },
    )

    fun readReplyPage(
        postId: Long,
        parentId: Long,
        cursor: RecruitmentPostCommentCursor?,
        size: Int,
    ): RecruitmentPostCommentPage = readPage(
        size = size,
        cursor = cursor,
        firstPage = { pageable -> commentRepository.findReplies(postId, parentId, pageable) },
        nextPage = { nextCursor, pageable ->
            commentRepository.findRepliesAfter(
                postId = postId,
                parentId = parentId,
                createdAt = nextCursor.createdAt,
                commentId = nextCursor.id,
                pageable = pageable,
            )
        },
    )

    fun readReplyPreviews(
        postId: Long,
        parentIds: Collection<Long>,
        size: Int,
    ): Map<Long, RecruitmentPostCommentPage> {
        if (parentIds.isEmpty()) return emptyMap()

        val replies = commentRepository.findRepliesByParentIds(postId, parentIds, size + 1)
        return replies
            .groupBy { checkNotNull(it.parentId) { "대댓글 부모 식별자가 없습니다." } }
            .mapValues { (_, comments) ->
                val hasNext = comments.size > size
                val items = comments.take(size)
                RecruitmentPostCommentPage(
                    comments = items,
                    nextCursor = items.lastOrNull()?.let { comment ->
                        if (hasNext) comment.toCursor() else null
                    },
                    hasNext = hasNext,
                )
            }
    }

    private fun readPage(
        size: Int,
        cursor: RecruitmentPostCommentCursor?,
        firstPage: (PageRequest) -> List<RecruitmentPostComment>,
        nextPage: (RecruitmentPostCommentCursor, PageRequest) -> List<RecruitmentPostComment>,
    ): RecruitmentPostCommentPage {
        require(size > 0) { "댓글 조회 크기는 양수여야 합니다." }

        val comments = if (cursor == null) {
            firstPage(PageRequest.of(0, size + 1))
        } else {
            nextPage(cursor, PageRequest.of(0, size + 1))
        }
        val pageComments = comments.take(size)
        val hasNext = comments.size > size

        return RecruitmentPostCommentPage(
            comments = pageComments,
            nextCursor = if (hasNext) pageComments.lastOrNull()?.toCursor() else null,
            hasNext = hasNext,
        )
    }
}

data class RecruitmentPostCommentCursor(
    val createdAt: LocalDateTime,
    val id: Long,
) {
    init {
        require(id > 0) { "댓글 커서의 식별자는 양수여야 합니다." }
    }
}

data class RecruitmentPostCommentPage(
    val comments: List<RecruitmentPostComment>,
    val nextCursor: RecruitmentPostCommentCursor?,
    val hasNext: Boolean,
)

private fun RecruitmentPostComment.toCursor(): RecruitmentPostCommentCursor {
    val commentId = checkNotNull(id) { "댓글 식별자가 없습니다." }
    return RecruitmentPostCommentCursor(createdAt = createdAt, id = commentId)
}
