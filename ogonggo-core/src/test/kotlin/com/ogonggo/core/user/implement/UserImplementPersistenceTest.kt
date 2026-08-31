package com.ogonggo.core.user.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.persistence.CompanyProfileJpaRepository
import com.ogonggo.core.user.persistence.UserProfileJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    UserReaderImpl::class,
    UserAppenderImpl::class,
    UserProfileManagerImpl::class,
    CompanyProfileAppenderImpl::class,
)
internal class UserImplementPersistenceTest @Autowired constructor(
    private val userReader: UserReader,
    private val userAppender: UserAppender,
    private val userProfileManager: UserProfileManager,
    private val companyProfileAppender: CompanyProfileAppender,
    private val userProfileRepository: UserProfileJpaRepository,
    private val companyProfileRepository: CompanyProfileJpaRepository,
) {

    @Test
    fun `렛츠커리어 식별자로 가입하고 조회한다`() {
        val account = userAppender.append(UserAppendCommand(letsCareerUserId = 4821L, joinedAt = NOW))

        assertEquals(4821L, account.letsCareerUserId)
        assertEquals(UserStatus.ACTIVE, account.status)
        assertEquals(account.userId, userReader.readByLetsCareerUserId(4821L)?.userId)
    }

    @Test
    fun `가입하지 않은 렛츠커리어 사용자는 null을 반환한다`() {
        assertNull(userReader.readByLetsCareerUserId(4821L))
    }

    @Test
    fun `존재하지 않는 사용자 조회는 USER_NOT_FOUND로 실패한다`() {
        val exception = assertThrows(EntityNotFoundException::class.java) { userReader.read(9999L) }

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `같은 렛츠커리어 사용자를 두 번 가입시키면 재시도 가능한 충돌로 처리한다`() {
        userAppender.append(UserAppendCommand(letsCareerUserId = 4821L, joinedAt = NOW))

        val exception = assertThrows(ConflictException::class.java) {
            userAppender.append(UserAppendCommand(letsCareerUserId = 4821L, joinedAt = NOW))
        }

        assertEquals(UserErrorCode.USER_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `프로필이 없으면 생성하고 렛츠커리어 수정 일시가 바뀌면 갱신한다`() {
        val account = userAppender.append(UserAppendCommand(letsCareerUserId = 4821L, joinedAt = NOW))

        userProfileManager.sync(syncCommand(account.userId, name = "김렛츠", letsCareerUpdatedAt = NOW))
        val created = userProfileRepository.findByUserId(account.userId)
        assertNotNull(created)
        assertEquals("김렛츠", created?.name)

        userProfileManager.sync(
            syncCommand(account.userId, name = "김커리어", letsCareerUpdatedAt = NOW.plusDays(1)),
        )

        assertEquals("김커리어", userProfileRepository.findByUserId(account.userId)?.name)
    }

    @Test
    fun `렛츠커리어 수정 일시가 같으면 프로필을 갱신하지 않는다`() {
        val account = userAppender.append(UserAppendCommand(letsCareerUserId = 4821L, joinedAt = NOW))
        userProfileManager.sync(syncCommand(account.userId, name = "김렛츠", letsCareerUpdatedAt = NOW))

        userProfileManager.sync(syncCommand(account.userId, name = "바뀐이름", letsCareerUpdatedAt = NOW))

        val profile = userProfileRepository.findByUserId(account.userId)
        assertEquals("김렛츠", profile?.name)
        assertEquals(NOW, profile?.lastSyncedAt)
    }

    @Test
    fun `기업 계정을 만들고 기업 프로필을 함께 저장한다`() {
        val account = userAppender.appendCompany(
            CompanyAccountAppendCommand(
                email = "company@example.com",
                encodedPassword = "encoded-password",
                joinedAt = NOW,
            ),
        )
        companyProfileAppender.append(
            CompanyProfileAppendCommand(
                userId = account.userId,
                organizationName = "렛츠커리어",
                managerName = "김담당",
            ),
        )

        assertEquals(UserRole.COMPANY, account.role)
        assertNull(account.letsCareerUserId)
        assertEquals("company@example.com", account.email)
        assertNotNull(companyProfileRepository.findByUserId(account.userId))
    }

    @Test
    fun `같은 이메일로 두 번 가입하면 충돌로 처리한다`() {
        val command = CompanyAccountAppendCommand(
            email = "company@example.com",
            encodedPassword = "encoded-password",
            joinedAt = NOW,
        )
        userAppender.appendCompany(command)

        val exception = assertThrows(ConflictException::class.java) { userAppender.appendCompany(command) }

        assertEquals(UserErrorCode.EMAIL_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `이메일로 기업 계정의 자격증명을 조회한다`() {
        val account = userAppender.appendCompany(
            CompanyAccountAppendCommand(
                email = "company@example.com",
                encodedPassword = "encoded-password",
                joinedAt = NOW,
            ),
        )

        val credential = userReader.readCredentialByEmail("company@example.com")

        assertEquals(account.userId, credential?.userId)
        assertEquals("encoded-password", credential?.encodedPassword)
        assertEquals(UserRole.COMPANY, credential?.role)
    }

    @Test
    fun `자격증명이 없는 렛츠커리어 계정은 이메일로 조회되지 않는다`() {
        userAppender.append(UserAppendCommand(letsCareerUserId = 4821L, joinedAt = NOW))

        assertNull(userReader.readCredentialByEmail("company@example.com"))
    }

    @Test
    fun `같은 사용자의 기업 프로필을 두 번 만들면 충돌로 처리한다`() {
        val account = userAppender.appendCompany(
            CompanyAccountAppendCommand(
                email = "company@example.com",
                encodedPassword = "encoded-password",
                joinedAt = NOW,
            ),
        )
        val command = CompanyProfileAppendCommand(
            userId = account.userId,
            organizationName = "렛츠커리어",
            managerName = "김담당",
        )
        companyProfileAppender.append(command)

        val exception = assertThrows(ConflictException::class.java) { companyProfileAppender.append(command) }

        assertEquals(UserErrorCode.COMPANY_PROFILE_ALREADY_EXISTS, exception.errorCode)
    }

    private fun syncCommand(
        userId: Long,
        name: String,
        letsCareerUpdatedAt: LocalDateTime,
    ): UserProfileSyncCommand = UserProfileSyncCommand(
        userId = userId,
        name = name,
        email = "lets@career.co.kr",
        nickname = "렛츠",
        profileImageUrl = null,
        letsCareerUpdatedAt = letsCareerUpdatedAt,
        syncedAt = letsCareerUpdatedAt,
    )

    companion object {
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 27, 10, 0)
    }
}
