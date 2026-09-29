package com.ogonggo.userapi.user.business

import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserManager
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.core.user.implement.dto.UserCredentialDto
import com.ogonggo.userapi.user.implement.LetsCareerUserClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

class UserPasswordServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val userManager = Mockito.mock(UserManager::class.java)
    private val letsCareerUserClient = Mockito.mock(LetsCareerUserClient::class.java)
    private val passwordEncoder = Mockito.mock(PasswordEncoder::class.java)
    private val transactionTemplate = object : TransactionTemplate() {
        override fun <T> execute(action: TransactionCallback<T>): T? = action.doInTransaction(SimpleTransactionStatus())
    }
    private val service = UserPasswordService(
        userReader,
        userManager,
        letsCareerUserClient,
        passwordEncoder,
        transactionTemplate,
    )

    @Test
    fun `일반 회원은 렛츠커리어로 변경을 전달하고 오공고에는 저장하지 않는다`() {
        // given
        givenAccount(letsCareerUserId = LETSCAREER_USER_ID, role = UserRole.USER)

        // when
        service.changeMyPassword(USER_ID, COMMAND)

        // then
        Mockito.verify(letsCareerUserClient).changePassword(LETSCAREER_USER_ID, "old-password!", "new-password!")
        Mockito.verifyNoInteractions(userManager, passwordEncoder)
    }

    @Test
    fun `기업 회원은 기존 비밀번호가 맞으면 오공고에서 새 값으로 바꾼다`() {
        // given
        givenAccount(letsCareerUserId = null, role = UserRole.COMPANY)
        givenCredential()
        Mockito.`when`(passwordEncoder.matches("old-password!", "encoded-old")).thenReturn(true)
        Mockito.`when`(passwordEncoder.encode("new-password!")).thenReturn("encoded-new")

        // when
        service.changeMyPassword(USER_ID, COMMAND)

        // then
        Mockito.verify(userManager).changePassword(USER_ID, "encoded-new")
        Mockito.verifyNoInteractions(letsCareerUserClient)
    }

    @Test
    fun `기업 회원의 기존 비밀번호가 틀리면 CURRENT_PASSWORD_MISMATCH로 막고 저장하지 않는다`() {
        // given
        givenAccount(letsCareerUserId = null, role = UserRole.COMPANY)
        givenCredential()
        Mockito.`when`(passwordEncoder.matches("old-password!", "encoded-old")).thenReturn(false)

        // when
        val exception = assertThrows(InvalidValueException::class.java) {
            service.changeMyPassword(USER_ID, COMMAND)
        }

        // then
        assertEquals(UserErrorCode.CURRENT_PASSWORD_MISMATCH, exception.errorCode)
        Mockito.verifyNoInteractions(userManager)
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

    private fun givenCredential() {
        Mockito.`when`(userReader.readCredential(USER_ID)).thenReturn(
            UserCredentialDto(
                userId = USER_ID,
                encodedPassword = "encoded-old",
                status = UserStatus.ACTIVE,
                role = UserRole.COMPANY,
            ),
        )
    }

    companion object {
        private const val USER_ID = 17L
        private const val LETSCAREER_USER_ID = 4821L
        private val COMMAND = PasswordChangeCommand(currentPassword = "old-password!", newPassword = "new-password!")
    }
}
