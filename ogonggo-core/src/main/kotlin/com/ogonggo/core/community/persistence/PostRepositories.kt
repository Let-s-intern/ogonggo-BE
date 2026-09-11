package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.Post
import com.ogonggo.core.community.domain.PublicationStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface PostJpaRepository : JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    fun findByIdAndPublicationStatusAndDeletedAtIsNull(id: Long, publicationStatus: PublicationStatus): Post?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select post
        from Post post
        where post.id = :postId
          and post.authorUserId = :authorUserId
          and post.deletedAt is null
        """,
    )
    fun findOwnedByIdForUpdate(
        @Param("authorUserId") authorUserId: Long,
        @Param("postId") postId: Long,
    ): Post?

    /** 삭제는 멱등해야 하므로 이미 삭제된 본인 모집글도 조회한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select post
        from Post post
        where post.id = :postId
          and post.authorUserId = :authorUserId
        """,
    )
    fun findOwnedByIdForDelete(
        @Param("authorUserId") authorUserId: Long,
        @Param("postId") postId: Long,
    ): Post?
}
