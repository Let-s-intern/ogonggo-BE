package com.ogonggo.core.concern.persistence

import com.ogonggo.core.concern.domain.ConcernComment
import com.ogonggo.core.concern.domain.ConcernCommentHelpfulVote
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

internal interface ConcernCommentJpaRepository : JpaRepository<ConcernComment, Long> {

    /** 먼저 단 답변을 앞에 둔다. 삭제된 답변은 남은 답글이 있을 때만 자리를 지킨다. */
    @Query(
        value = """
        SELECT comment
        FROM ConcernComment comment
        WHERE comment.concernId = :concernId
          AND comment.parentId IS NULL
          AND (
              comment.deletedAt IS NULL
              OR EXISTS (
                  SELECT reply.id FROM ConcernComment reply
                  WHERE reply.parentId = comment.id AND reply.deletedAt IS NULL
              )
          )
        ORDER BY comment.createdAt ASC, comment.id ASC
        """,
        countQuery = """
        SELECT COUNT(comment.id)
        FROM ConcernComment comment
        WHERE comment.concernId = :concernId
          AND comment.parentId IS NULL
          AND (
              comment.deletedAt IS NULL
              OR EXISTS (
                  SELECT reply.id FROM ConcernComment reply
                  WHERE reply.parentId = comment.id AND reply.deletedAt IS NULL
              )
          )
        """,
    )
    fun findRootComments(@Param("concernId") concernId: Long, pageable: Pageable): Page<ConcernComment>

    @Query(
        value = """
        SELECT comment
        FROM ConcernComment comment
        WHERE comment.concernId = :concernId
          AND comment.parentId = :parentId
          AND comment.deletedAt IS NULL
        ORDER BY comment.createdAt ASC, comment.id ASC
        """,
        countQuery = """
        SELECT COUNT(comment.id)
        FROM ConcernComment comment
        WHERE comment.concernId = :concernId
          AND comment.parentId = :parentId
          AND comment.deletedAt IS NULL
        """,
    )
    fun findReplies(
        @Param("concernId") concernId: Long,
        @Param("parentId") parentId: Long,
        pageable: Pageable,
    ): Page<ConcernComment>

    @Query(
        """
        SELECT id, concern_id, parent_id, user_id, content, official, created_at, updated_at, deleted_at
        FROM (
            SELECT comment.*, ROW_NUMBER() OVER (
                PARTITION BY comment.parent_id
                ORDER BY comment.created_at ASC, comment.id ASC
            ) AS reply_rank
            FROM concern_comments comment
            WHERE comment.concern_id = :concernId
              AND comment.parent_id IN (:parentIds)
              AND comment.deleted_at IS NULL
        ) ranked_comments
        WHERE ranked_comments.reply_rank <= :limit
        ORDER BY parent_id ASC, created_at ASC, id ASC
        """,
        nativeQuery = true,
    )
    fun findRepliesByParentIds(
        @Param("concernId") concernId: Long,
        @Param("parentIds") parentIds: Collection<Long>,
        @Param("limit") limit: Int,
    ): List<ConcernComment>

    @Query(
        """
        SELECT new com.ogonggo.core.concern.persistence.ConcernCountRow(comment.parentId, COUNT(comment.id))
        FROM ConcernComment comment
        WHERE comment.concernId = :concernId
          AND comment.parentId IN :parentIds
          AND comment.deletedAt IS NULL
        GROUP BY comment.parentId
        """,
    )
    fun countRepliesByParentIds(
        @Param("concernId") concernId: Long,
        @Param("parentIds") parentIds: Collection<Long>,
    ): List<ConcernCountRow>

    @Query(
        """
        SELECT DISTINCT comment.concernId
        FROM ConcernComment comment
        WHERE comment.concernId IN :concernIds
          AND comment.official = true
          AND comment.deletedAt IS NULL
        """,
    )
    fun findConcernIdsWithOfficialComment(@Param("concernIds") concernIds: Collection<Long>): List<Long>

    /** 답글 더보기는 삭제된 답변의 남은 답글도 보여 주므로 삭제 여부를 가리지 않는다. */
    @Query(
        """
        SELECT comment
        FROM ConcernComment comment
        WHERE comment.id = :commentId
          AND comment.concernId = :concernId
          AND comment.parentId IS NULL
        """,
    )
    fun findRootByIdAndConcernId(
        @Param("commentId") commentId: Long,
        @Param("concernId") concernId: Long,
    ): ConcernComment?

    @Query(
        """
        SELECT comment
        FROM ConcernComment comment
        WHERE comment.id = :commentId
          AND comment.concernId = :concernId
          AND comment.deletedAt IS NULL
        """,
    )
    fun findActiveByIdAndConcernId(
        @Param("commentId") commentId: Long,
        @Param("concernId") concernId: Long,
    ): ConcernComment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT comment
        FROM ConcernComment comment
        WHERE comment.id = :commentId
          AND comment.concernId = :concernId
          AND comment.deletedAt IS NULL
        """,
    )
    fun findActiveByIdAndConcernIdForUpdate(
        @Param("commentId") commentId: Long,
        @Param("concernId") concernId: Long,
    ): ConcernComment?
}

internal interface ConcernCommentHelpfulVoteJpaRepository : JpaRepository<ConcernCommentHelpfulVote, Long> {

    fun findByCommentIdAndUserId(commentId: Long, userId: Long): ConcernCommentHelpfulVote?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ConcernCommentHelpfulVote vote
        set vote.deletedAt = null,
            vote.updatedAt = :now
        where vote.commentId = :commentId
          and vote.userId = :userId
          and vote.deletedAt is not null
        """,
    )
    fun restore(
        @Param("commentId") commentId: Long,
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ConcernCommentHelpfulVote vote
        set vote.deletedAt = :now,
            vote.updatedAt = :now
        where vote.commentId = :commentId
          and vote.userId = :userId
          and vote.deletedAt is null
        """,
    )
    fun cancel(
        @Param("commentId") commentId: Long,
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime,
    ): Int

    @Query(
        """
        SELECT new com.ogonggo.core.concern.persistence.ConcernCountRow(vote.commentId, COUNT(vote.id))
        FROM ConcernCommentHelpfulVote vote
        WHERE vote.commentId IN :commentIds
          AND vote.deletedAt IS NULL
        GROUP BY vote.commentId
        """,
    )
    fun countActiveByCommentIds(@Param("commentIds") commentIds: Collection<Long>): List<ConcernCountRow>

    @Query(
        """
        SELECT vote.commentId
        FROM ConcernCommentHelpfulVote vote
        WHERE vote.userId = :userId
          AND vote.commentId IN :commentIds
          AND vote.deletedAt IS NULL
        """,
    )
    fun findActiveCommentIds(
        @Param("userId") userId: Long,
        @Param("commentIds") commentIds: Collection<Long>,
    ): List<Long>
}

/** 식별자별 개수 집계 결과다. 답글 수와 도움돼요 수에 함께 쓴다. */
data class ConcernCountRow(
    val id: Long,
    val count: Long,
)
