package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.RecruitmentPostBookmark
import com.ogonggo.core.community.domain.PostMetric
import com.ogonggo.core.community.domain.PublicationStatus
import org.springframework.data.domain.Pageable
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

internal interface RecruitmentPostJpaRepository : JpaRepository<RecruitmentPost, Long>, JpaSpecificationExecutor<RecruitmentPost> {
    fun findByIdAndPublicationStatusAndDeletedAtIsNull(id: Long, publicationStatus: PublicationStatus): RecruitmentPost?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select post
        from RecruitmentPost post
        where post.id = :postId
          and post.publicationStatus = :publicationStatus
          and post.deletedAt is null
        """,
    )
    fun findPublishedByIdForUpdate(
        @Param("postId") postId: Long,
        @Param("publicationStatus") publicationStatus: PublicationStatus,
    ): RecruitmentPost?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select post
        from RecruitmentPost post
        where post.id = :postId
          and post.authorUserId = :authorUserId
          and post.deletedAt is null
        """,
    )
    fun findOwnedByIdForUpdate(
        @Param("authorUserId") authorUserId: Long,
        @Param("postId") postId: Long,
    ): RecruitmentPost?

    /** 삭제는 멱등해야 하므로 이미 삭제된 본인 모집글도 조회한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select post
        from RecruitmentPost post
        where post.id = :postId
          and post.authorUserId = :authorUserId
        """,
    )
    fun findOwnedByIdForDelete(
        @Param("authorUserId") authorUserId: Long,
        @Param("postId") postId: Long,
    ): RecruitmentPost?
}

internal interface PostMetricJpaRepository : JpaRepository<PostMetric, Long> {
    fun findByPostId(postId: Long): PostMetric?

    fun findAllByPostIdIn(postIds: Collection<Long>): List<PostMetric>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PostMetric metric
        set metric.viewCount = metric.viewCount + :amount,
            metric.updatedAt = :now
        where metric.postId = :postId
        """,
    )
    fun increaseViewCount(
        @Param("postId") postId: Long,
        @Param("amount") amount: Long,
        @Param("now") now: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PostMetric metric
        set metric.commentCount = metric.commentCount + 1,
            metric.updatedAt = :now
        where metric.postId = :postId
        """,
    )
    fun increaseCommentCount(@Param("postId") postId: Long, @Param("now") now: LocalDateTime): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PostMetric metric
        set metric.commentCount = metric.commentCount - :amount,
            metric.updatedAt = :now
        where metric.postId = :postId
          and metric.commentCount >= :amount
        """,
    )
    fun decreaseCommentCount(
        @Param("postId") postId: Long,
        @Param("amount") amount: Int,
        @Param("now") now: LocalDateTime,
    ): Int
}

internal interface RecruitmentPostBookmarkJpaRepository : JpaRepository<RecruitmentPostBookmark, Long> {
    fun findByPostIdAndUserId(postId: Long, userId: Long): RecruitmentPostBookmark?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update RecruitmentPostBookmark bookmark
        set bookmark.deletedAt = null,
            bookmark.updatedAt = :now
        where bookmark.postId = :postId
          and bookmark.userId = :userId
          and bookmark.deletedAt is not null
        """,
    )
    fun restore(
        @Param("postId") postId: Long,
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update RecruitmentPostBookmark bookmark
        set bookmark.deletedAt = :now,
            bookmark.updatedAt = :now
        where bookmark.postId = :postId
          and bookmark.userId = :userId
          and bookmark.deletedAt is null
        """,
    )
    fun softDelete(
        @Param("postId") postId: Long,
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime,
    ): Int

    @Query(
        """
        select new com.ogonggo.core.community.persistence.RecruitmentPostBookmarkCursorRow(
            post,
            bookmark.updatedAt,
            bookmark.id
        )
        from RecruitmentPostBookmark bookmark
        join RecruitmentPost post on bookmark.postId = post.id
        where bookmark.userId = :userId
          and bookmark.deletedAt is null
          and post.publicationStatus = :publicationStatus
          and post.deletedAt is null
          and (
              :cursorUpdatedAt is null
              or bookmark.updatedAt < :cursorUpdatedAt
              or (bookmark.updatedAt = :cursorUpdatedAt and bookmark.id < :cursorId)
          )
        order by bookmark.updatedAt desc, bookmark.id desc
        """,
    )
    fun findBookmarkedPublishedCursorPage(
        @Param("userId") userId: Long,
        @Param("publicationStatus") publicationStatus: PublicationStatus,
        @Param("cursorUpdatedAt") cursorUpdatedAt: LocalDateTime?,
        @Param("cursorId") cursorId: Long?,
        pageable: Pageable,
    ): List<RecruitmentPostBookmarkCursorRow>

    @Query(
        """
        select bookmark.postId
        from RecruitmentPostBookmark bookmark
        where bookmark.userId = :userId
          and bookmark.postId in :postIds
          and bookmark.deletedAt is null
        """,
    )
    fun findActivePostIds(
        @Param("userId") userId: Long,
        @Param("postIds") postIds: Collection<Long>,
    ): Set<Long>
}

data class RecruitmentPostBookmarkCursorRow(
    val post: RecruitmentPost,
    val updatedAt: LocalDateTime,
    val bookmarkId: Long,
)
