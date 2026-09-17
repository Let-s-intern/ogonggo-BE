package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.RecruitmentPostComment
import com.ogonggo.core.community.domain.RecruitmentPostCommentReport
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import java.time.LocalDateTime

internal interface RecruitmentPostCommentJpaRepository : JpaRepository<RecruitmentPostComment, Long> {

    @Query(
        value = """
        SELECT comment
        FROM RecruitmentPostComment comment
        WHERE comment.postId = :postId
          AND comment.parentId IS NULL
          AND comment.deletedAt IS NULL
        ORDER BY comment.createdAt DESC, comment.id DESC
        """,
        countQuery = """
        SELECT COUNT(comment.id)
        FROM RecruitmentPostComment comment
        WHERE comment.postId = :postId
          AND comment.parentId IS NULL
          AND comment.deletedAt IS NULL
        """,
    )
    fun findRootComments(
        @Param("postId") postId: Long,
        pageable: Pageable,
    ): Page<RecruitmentPostComment>

    @Query(
        value = """
        SELECT comment
        FROM RecruitmentPostComment comment
        WHERE comment.postId = :postId
          AND comment.parentId = :parentId
          AND comment.deletedAt IS NULL
        ORDER BY comment.createdAt ASC, comment.id ASC
        """,
        countQuery = """
        SELECT COUNT(comment.id)
        FROM RecruitmentPostComment comment
        WHERE comment.postId = :postId
          AND comment.parentId = :parentId
          AND comment.deletedAt IS NULL
        """,
    )
    fun findReplies(
        @Param("postId") postId: Long,
        @Param("parentId") parentId: Long,
        pageable: Pageable,
    ): Page<RecruitmentPostComment>

    @Query(
        """
        SELECT id, post_id, parent_id, user_id, content, created_at, updated_at, deleted_at
        FROM (
            SELECT comment.*, ROW_NUMBER() OVER (
                PARTITION BY comment.parent_id
                ORDER BY comment.created_at ASC, comment.id ASC
            ) AS reply_rank
            FROM recruitment_post_comments comment
            WHERE comment.post_id = :postId
              AND comment.parent_id IN (:parentIds)
              AND comment.deleted_at IS NULL
        ) ranked_comments
        WHERE ranked_comments.reply_rank <= :limit
        ORDER BY parent_id ASC, created_at ASC, id ASC
        """,
        nativeQuery = true,
    )
    fun findRepliesByParentIds(
        @Param("postId") postId: Long,
        @Param("parentIds") parentIds: Collection<Long>,
        @Param("limit") limit: Int,
    ): List<RecruitmentPostComment>

    @Query(
        """
        SELECT new com.ogonggo.core.community.persistence.RecruitmentPostCommentCountRow(
            comment.parentId,
            COUNT(comment.id)
        )
        FROM RecruitmentPostComment comment
        WHERE comment.postId = :postId
          AND comment.parentId IN :parentIds
          AND comment.deletedAt IS NULL
        GROUP BY comment.parentId
        """,
    )
    fun countRepliesByParentIds(
        @Param("postId") postId: Long,
        @Param("parentIds") parentIds: Collection<Long>,
    ): List<RecruitmentPostCommentCountRow>

    @Modifying(flushAutomatically = true)
    @Query(
        """
        update RecruitmentPostComment comment
        set comment.deletedAt = :deletedAt,
            comment.updatedAt = :deletedAt
        where comment.parentId = :parentId
          and comment.deletedAt is null
        """,
    )
    fun softDeleteActiveReplies(
        @Param("parentId") parentId: Long,
        @Param("deletedAt") deletedAt: LocalDateTime,
    ): Int

    @Query(
        """
        select comment
        from RecruitmentPostComment comment
        where comment.id = :commentId
          and comment.postId = :postId
          and comment.parentId is null
          and comment.deletedAt is null
        """,
    )
    fun findActiveRootByIdAndPostId(
        @Param("commentId") commentId: Long,
        @Param("postId") postId: Long,
    ): RecruitmentPostComment?

    @Query(
        """
        select comment
        from RecruitmentPostComment comment
        where comment.id = :commentId
          and comment.postId = :postId
          and comment.deletedAt is null
        """,
    )
    fun findActiveByIdAndPostId(
        @Param("commentId") commentId: Long,
        @Param("postId") postId: Long,
    ): RecruitmentPostComment?

    @Query("select comment from RecruitmentPostComment comment where comment.id = :commentId and comment.deletedAt is null")
    fun findActiveById(@Param("commentId") commentId: Long): RecruitmentPostComment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select comment from RecruitmentPostComment comment where comment.id = :commentId and comment.deletedAt is null")
    fun findByIdForUpdate(@Param("commentId") commentId: Long): RecruitmentPostComment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select comment
        from RecruitmentPostComment comment
        where comment.id = :commentId
          and comment.postId = :postId
          and comment.deletedAt is null
        """,
    )
    fun findByIdAndPostIdForUpdate(
        @Param("commentId") commentId: Long,
        @Param("postId") postId: Long,
    ): RecruitmentPostComment?
}

internal interface RecruitmentPostCommentReportJpaRepository : JpaRepository<RecruitmentPostCommentReport, Long>

data class RecruitmentPostCommentCountRow(
    val parentId: Long,
    val count: Long,
)
