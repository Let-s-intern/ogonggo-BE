package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.paging.validatePageRequest
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostComment
import com.ogonggo.core.recruitmentpost.error.RecruitmentPostCommentErrorCode
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostCommentJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostCommentPageDto

@Component
class RecruitmentPostCommentReader internal constructor(
    private val commentRepository: RecruitmentPostCommentJpaRepository,
) {
    fun read(commentId: Long): RecruitmentPostComment =
        commentRepository.findActiveById(commentId)
            ?: throw EntityNotFoundException(
                RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_PARENT_NOT_FOUND,
            )

    fun readRoot(postId: Long, commentId: Long): RecruitmentPostComment =
        commentRepository.findRootByIdAndPostId(commentId, postId)
            ?: throw EntityNotFoundException(RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_NOT_FOUND)

    fun readInPost(postId: Long, commentId: Long): RecruitmentPostComment =
        commentRepository.findActiveByIdAndPostId(commentId, postId)
            ?: throw EntityNotFoundException(RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_NOT_FOUND)

    fun readForUpdate(commentId: Long): RecruitmentPostComment =
        commentRepository.findByIdForUpdate(commentId)
            ?: throw EntityNotFoundException(
                RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_PARENT_NOT_FOUND,
            )

    fun readInPostForUpdate(postId: Long, commentId: Long): RecruitmentPostComment =
        commentRepository.findByIdAndPostIdForUpdate(commentId, postId)
            ?: throw EntityNotFoundException(RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_NOT_FOUND)

    fun readRootPage(
        postId: Long,
        page: Int,
        size: Int,
    ): RecruitmentPostCommentPageDto {
        validatePageRequest(page, size)
        val result = commentRepository.findRootComments(postId, PageRequest.of(page, size))
        return RecruitmentPostCommentPageDto(
            comments = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    fun readReplyPage(
        postId: Long,
        parentId: Long,
        page: Int,
        size: Int,
    ): RecruitmentPostCommentPageDto {
        validatePageRequest(page, size)
        val result = commentRepository.findReplies(postId, parentId, PageRequest.of(page, size))
        return RecruitmentPostCommentPageDto(
            comments = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    fun readReplyPreviews(
        postId: Long,
        parentIds: Collection<Long>,
        size: Int,
    ): Map<Long, RecruitmentPostCommentPageDto> {
        if (parentIds.isEmpty()) return emptyMap()

        val replies = commentRepository.findRepliesByParentIds(postId, parentIds, size)
        val replyCounts = commentRepository.countRepliesByParentIds(postId, parentIds)
            .associate { it.parentId to it.count }
        return replies
            .groupBy { checkNotNull(it.parentId) { "대댓글 부모 식별자가 없습니다." } }
            .mapValues { (_, comments) ->
                val parentId = checkNotNull(comments.first().parentId)
                val totalElements = replyCounts[parentId] ?: comments.size.toLong()
                RecruitmentPostCommentPageDto(
                    comments = comments,
                    page = 0,
                    size = size,
                    totalElements = totalElements,
                    totalPages = pageCount(totalElements, size),
                )
            }
    }
}

private fun pageCount(totalElements: Long, size: Int): Int =
    if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
