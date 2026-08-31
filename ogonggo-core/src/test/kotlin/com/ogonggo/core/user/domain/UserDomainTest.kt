package com.ogonggo.core.user.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserDomainTest {

    private val joinedAt = LocalDateTime.of(2026, 8, 25, 10, 0)

    @Test
    fun `사용자를 정지하고 다시 활성화할 수 있다`() {
        val user = User.ofLetsCareer(letsCareerUserId = 1L, joinedAt = joinedAt)

        user.suspend()
        assertEquals(UserStatus.SUSPENDED, user.status)

        user.activate()
        assertEquals(UserStatus.ACTIVE, user.status)
    }

    @Test
    fun `탈퇴는 멱등하며 최초 탈퇴 일시를 유지한다`() {
        val user = User.ofLetsCareer(letsCareerUserId = 1L, joinedAt = joinedAt)
        val firstWithdrawnAt = LocalDateTime.of(2026, 8, 26, 10, 0)

        user.withdraw(firstWithdrawnAt)
        user.withdraw(firstWithdrawnAt.plusDays(1))

        assertEquals(UserStatus.WITHDRAWN, user.status)
        assertEquals(firstWithdrawnAt, user.withdrawnAt)
    }

    @Test
    fun `탈퇴한 사용자는 정지하거나 활성화할 수 없다`() {
        val user = User.ofLetsCareer(letsCareerUserId = 1L, joinedAt = joinedAt)
        user.withdraw(LocalDateTime.of(2026, 8, 26, 10, 0))

        assertThrows(IllegalStateException::class.java) { user.suspend() }
        assertThrows(IllegalStateException::class.java) { user.activate() }
    }

    @Test
    fun `렛츠커리어 계정은 일반 회원이며 자격증명을 갖지 않는다`() {
        val user = User.ofLetsCareer(letsCareerUserId = 1L, joinedAt = joinedAt)

        assertEquals(UserRole.USER, user.role)
        assertNull(user.email)
        assertNull(user.password)
    }

    @Test
    fun `기업 계정은 가입 시점에 기업 회원이며 렛츠커리어 식별자를 갖지 않는다`() {
        val user = User.ofCompany(
            email = "company@example.com",
            encodedPassword = "encoded",
            joinedAt = joinedAt,
        )

        assertEquals(UserRole.COMPANY, user.role)
        assertEquals(UserStatus.ACTIVE, user.status)
        assertEquals("company@example.com", user.email)
        assertNull(user.letsCareerUserId)
    }

    @Test
    fun `기업 계정의 이메일과 비밀번호는 비어 있을 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            User.ofCompany(email = " ", encodedPassword = "encoded", joinedAt = joinedAt)
        }
        assertThrows(IllegalArgumentException::class.java) {
            User.ofCompany(email = "company@example.com", encodedPassword = " ", joinedAt = joinedAt)
        }
    }

    @Test
    fun `렛츠커리어 사용자 식별자는 양수여야 한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            User.ofLetsCareer(letsCareerUserId = 0L, joinedAt = joinedAt)
        }
    }

    @Test
    fun `렛츠커리어 프로필을 동기화한다`() {
        val initialSyncedAt = LocalDateTime.of(2026, 8, 25, 10, 0)
        val profile = UserProfile(userId = 1L, lastSyncedAt = initialSyncedAt)
        val updatedAt = LocalDateTime.of(2026, 8, 26, 9, 0)
        val syncedAt = LocalDateTime.of(2026, 8, 26, 10, 0)

        profile.sync(
            name = "홍길동",
            email = "user@example.com",
            nickname = "오공고",
            profileImageUrl = "https://example.com/profile.png",
            letsCareerUpdatedAt = updatedAt,
            syncedAt = syncedAt,
        )

        assertEquals("홍길동", profile.name)
        assertEquals("user@example.com", profile.email)
        assertEquals("오공고", profile.nickname)
        assertEquals("https://example.com/profile.png", profile.profileImageUrl)
        assertEquals(updatedAt, profile.letsCareerUpdatedAt)
        assertEquals(syncedAt, profile.lastSyncedAt)
    }
}
