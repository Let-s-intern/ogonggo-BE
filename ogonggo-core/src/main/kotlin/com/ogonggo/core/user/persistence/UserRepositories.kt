package com.ogonggo.core.user.persistence

import com.ogonggo.core.user.domain.CompanyProfile
import com.ogonggo.core.user.domain.LetsCareerJobProfileOutbox
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.domain.UserProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

internal interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByLetsCareerUserId(letsCareerUserId: Long): User?
    fun findByEmail(email: String): User?
}

internal interface UserProfileJpaRepository : JpaRepository<UserProfile, Long> {
    fun findByUserId(userId: Long): UserProfile?
    fun findAllByUserIdIn(userIds: Collection<Long>): List<UserProfile>
}

internal interface CompanyProfileJpaRepository : JpaRepository<CompanyProfile, Long> {
    fun findByUserId(userId: Long): CompanyProfile?
}

internal interface LetsCareerJobProfileOutboxJpaRepository : JpaRepository<LetsCareerJobProfileOutbox, Long> {
    fun findByUserId(userId: Long): LetsCareerJobProfileOutbox?

    fun findTop100ByOrderByAttemptCountAscRequestedAtAsc(): List<LetsCareerJobProfileOutbox>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from LetsCareerJobProfileOutbox o where o.userId = :userId and o.requestedAt = :requestedAt")
    fun deleteSent(@Param("userId") userId: Long, @Param("requestedAt") requestedAt: LocalDateTime): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update LetsCareerJobProfileOutbox o set o.attemptCount = o.attemptCount + 1 where o.userId = :userId")
    fun increaseAttemptCount(@Param("userId") userId: Long): Int
}
