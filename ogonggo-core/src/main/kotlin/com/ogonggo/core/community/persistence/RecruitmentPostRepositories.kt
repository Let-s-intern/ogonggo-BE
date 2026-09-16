package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.PostMetric
import com.ogonggo.core.community.domain.PublicationStatus
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
