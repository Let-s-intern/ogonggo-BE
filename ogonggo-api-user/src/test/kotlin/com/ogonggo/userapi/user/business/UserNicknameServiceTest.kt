package com.ogonggo.userapi.user.business

import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserProfileManager
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.userapi.auth.error.AuthErrorCode
import com.ogonggo.userapi.user.implement.LetsCareerUserClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class UserNicknameServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val userProfileManager = Mockito.mock(UserProfileManager::class.java)
    private val letsCareerUserClient = Mockito.mock(LetsCareerUserClient::class.java)
    private val transactionTemplate = object : TransactionTemplate() {
        override fun <T> execute(action: TransactionCallback<T>): T? = action.doInTransaction(SimpleTransactionStatus())
    }
    private val clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE)
    private val service = UserNicknameService(
        userReader,
        userProfileManager,
        letsCareerUserClient,
        transactionTemplate,
        clock,
    )

    @Test
    fun `일반 회원은 렛츠커리어로 변경을 전달하고 오공고 프로필에도 반영한다`() {
        // given
        givenAccount(letsCareerUserId = LETSCAREER_USER_ID, role = UserRole.USER)

        // when
        service.changeMyNickname(USER_ID, "오공고")

        // then
        Mockito.verify(letsCareerUserClient).changeNickname(LETSCAREER_USER_ID, "오공고")
        Mockito.verify(userProfileManager).changeNickname(USER_ID, "오공고", NOW)
    }

    @Test
    fun `렛츠커리어에서 실패하면 오공고 닉네임을 바꾸지 않는다`() {
        // given
        givenAccount(letsCareerUserId = LETSCAREER_USER_ID, role = UserRole.USER)
        Mockito.`when`(letsCareerUserClient.changeNickname(LETSCAREER_USER_ID, "오공고"))
            .thenThrow(InternalServerException(AuthErrorCode.LETSCAREER_UNAVAILABLE))

        // when
        val exception = assertThrows(InternalServerException::class.java) {
            service.changeMyNickname(USER_ID, "오공고")
        }

        // then
        assertEquals(AuthErrorCode.LETSCAREER_UNAVAILABLE, exception.errorCode)
        Mockito.verifyNoInteractions(userProfileManager)
    }

    @Test
    fun `기업 회원은 GENERAL_MEMBER_REQUIRED로 막는다`() {
        // given
        givenAccount(letsCareerUserId = null, role = UserRole.COMPANY)

        // when
        val exception = assertThrows(ForbiddenException::class.java) {
            service.changeMyNickname(USER_ID, "오공고")
        }

        // then
        assertEquals(UserErrorCode.GENERAL_MEMBER_REQUIRED, exception.errorCode)
        Mockito.verifyNoInteractions(letsCareerUserClient, userProfileManager)
    }

    private fun givenAccount(letsCareerUserId: Long?, role: UserRole) {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(
            UserAccountDto(
                userId = USER_ID,
                letsCareerUserId = letsCareerUserId,
                email = null,
                status = UserStatus.ACTIVE,
                role = role,
                joinedAt = LocalDateTime.of(2026, 8, 1, 9, 0),
            ),
        )
    }

    companion object {
        private const val USER_ID = 17L
        private const val LETSCAREER_USER_ID = 4821L
        private val ZONE = ZoneId.of("Asia/Seoul")
        private val NOW = LocalDateTime.of(2026, 10, 12, 10, 0)
    }
}
