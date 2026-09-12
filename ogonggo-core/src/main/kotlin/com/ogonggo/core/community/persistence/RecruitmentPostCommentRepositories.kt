package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.RecruitmentPostComment
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface RecruitmentPostCommentJpaRepository : JpaRepository<RecruitmentPostComment, Long> {

    @Query(
        """
        SELECT comment
        FROM RecruitmentPostComment comment
        WHERE comment.post.id = :postId
          AND comment.parent IS NULL
        ORDER BY comment.createdAt DESC, comment.id DESC
        """,
    )
    fun findRootComments(
        @Param("postId") postId: Long,
        pageable: Pageable,
    ): List<RecruitmentPostComment>

    @Query(
        """
        SELECT comment
        FROM RecruitmentPostComment comment
        WHERE comment.post.id = :postId
          AND comment.parent IS NULL
          AND (
              comment.createdAt < :createdAt
              OR (comment.createdAt = :createdAt AND comment.id < :commentId)
          )
        ORDER BY comment.createdAt DESC, comment.id DESC
        """,
    )
    fun findRootCommentsAfter(
        @Param("postId") postId: Long,
        @Param("createdAt") createdAt: LocalDateTime,
        @Param("commentId") commentId: Long,
        pageable: Pageable,
    ): List<RecruitmentPostComment>

    @Query(
        """
        SELECT comment
        FROM RecruitmentPostComment comment
        WHERE comment.post.id = :postId
          AND comment.parent.id = :parentId
        ORDER BY comment.createdAt ASC, comment.id ASC
        """,
    )
    fun findReplies(
        @Param("postId") postId: Long,
        @Param("parentId") parentId: Long,
        pageable: Pageable,
    ): List<RecruitmentPostComment>

    @Query(
        """
        SELECT comment
        FROM RecruitmentPostComment comment
        WHERE comment.post.id = :postId
          AND comment.parent.id = :parentId
          AND (
              comment.createdAt > :createdAt
              OR (comment.createdAt = :createdAt AND comment.id > :commentId)
          )
        ORDER BY comment.createdAt ASC, comment.id ASC
        """,
    )
    fun findRepliesAfter(
        @Param("postId") postId: Long,
        @Param("parentId") parentId: Long,
        @Param("createdAt") createdAt: LocalDateTime,
        @Param("commentId") commentId: Long,
        pageable: Pageable,
    ): List<RecruitmentPostComment>

    @Query(
        """
        SELECT id, post_id, parent_id, user_id, content, created_at, updated_at
        FROM (
            SELECT comment.*, ROW_NUMBER() OVER (
                PARTITION BY comment.parent_id
                ORDER BY comment.created_at ASC, comment.id ASC
            ) AS row_number
            FROM recruitment_post_comments comment
            WHERE comment.post_id = :postId
              AND comment.parent_id IN (:parentIds)
        ) ranked_comments
        WHERE ranked_comments.row_number <= :limit
        ORDER BY parent_id ASC, created_at ASC, id ASC
        """,
        nativeQuery = true,
    )
    fun findRepliesByParentIds(
        @Param("postId") postId: Long,
        @Param("parentIds") parentIds: Collection<Long>,
        @Param("limit") limit: Int,
    ): List<RecruitmentPostComment>

    @Modifying(flushAutomatically = true)
    @Query("delete from RecruitmentPostComment comment where comment.parent.id = :parentId")
    fun deleteAllByParentId(@Param("parentId") parentId: Long): Int

    fun findByIdAndPost_IdAndParentIsNull(commentId: Long, postId: Long): RecruitmentPostComment?

    fun findByIdAndPost_Id(commentId: Long, postId: Long): RecruitmentPostComment?
}
