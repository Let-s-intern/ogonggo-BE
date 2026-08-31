package com.ogonggo.core.user.persistence

import com.ogonggo.core.user.domain.CompanyProfile
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.domain.UserProfile
import org.springframework.data.jpa.repository.JpaRepository

internal interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByLetsCareerUserId(letsCareerUserId: Long): User?
    fun findByEmail(email: String): User?
}

internal interface UserProfileJpaRepository : JpaRepository<UserProfile, Long> {
    fun findByUserId(userId: Long): UserProfile?
}

internal interface CompanyProfileJpaRepository : JpaRepository<CompanyProfile, Long> {
    fun findByUserId(userId: Long): CompanyProfile?
}
