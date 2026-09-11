package com.ogonggo.core.community.persistence

import com.ogonggo.core.community.domain.Post
import com.ogonggo.core.community.domain.PublicationStatus
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType

internal interface PostJpaRepository : JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    fun findByIdAndPublicationStatus(id: Long, publicationStatus: PublicationStatus): Post?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select post
        from Post post
        where post.id = :postId
          and post.authorUserId = :authorUserId
        """,
    )
    fun findOwnedByIdForUpdate(
        @Param("authorUserId") authorUserId: Long,
        @Param("postId") postId: Long,
    ): Post?

}
