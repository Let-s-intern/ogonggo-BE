package com.ogonggo.userapi.user.business

import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.LetsCareerNicknameOutboxManager
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.LetsCareerNicknameOutboxDto
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.core.user.implement.dto.UserProfileDto
import com.ogonggo.userapi.user.implement.LetsCareerNicknameReplaceCommand
import com.ogonggo.userapi.user.implement.LetsCareerUserClient
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.client.ResourceAccessException

class LetsCareerNicknameSendServiceTest {

    private val outboxManager = Mockito.mock(LetsCareerNicknameOutboxManager::class.java)
    private val userReader = Mockito.mock(UserReader::class.java)
    private val userProfileReader = Mockito.mock(UserProfileReader::class.java)
    private val letsCareerUserClient = Mockito.mock(LetsCareerUserClient::class.java)
    private val service = LetsCareerNicknameSendService(
        outboxManager,
        userReader,
        userProfileReader,
        letsCareerUserClient,
        TransactionTemplate(Mockito.mock(PlatformTransactionManager::class.java)),
        CLOCK,
    )

    @Test
    fun `보낼 때의 최신 닉네임과 고친 일시를 렛츠커리어 사용자 식별자로 보내고 보낸 것으로 표시한다`() {
        // given
        givenPending()
        givenProfile(nicknameUpdatedAt = UPDATED_AT)

        // when
        service.sendPending()

        // then
        Mockito.verify(letsCareerUserClient).replaceNickname(LETSCAREER_USER_ID, EXPECTED_COMMAND)
        Mockito.verify(outboxManager).markSent(OUTBOX, SENT_AT)
    }

    @Test
    fun `보내지 못하면 실패로 남겨 다음 주기에 다시 보낸다`() {
        // given
        givenPending()
        givenProfile(nicknameUpdatedAt = UPDATED_AT)
        Mockito.doThrow(ResourceAccessException("timeout"))
            .`when`(letsCareerUserClient).replaceNickname(LETSCAREER_USER_ID, EXPECTED_COMMAND)

        // when
        service.sendPending()

        // then
        Mockito.verify(outboxManager).markFailed(OUTBOX)
        Mockito.verify(outboxManager, Mockito.never()).markSent(OUTBOX, SENT_AT)
    }

    @Test
    fun `오공고에서 고친 닉네임이 없으면 호출하지 않고 보낸 것으로 표시한다`() {
        // given
        givenPending()
        givenProfile(nicknameUpdatedAt = null)

        // when
        service.sendPending()

        // then
        Mockito.verifyNoInteractions(letsCareerUserClient)
        Mockito.verify(outboxManager).markSent(OUTBOX, SENT_AT)
    }

    private fun givenPending() {
        Mockito.`when`(outboxManager.readPending()).thenReturn(listOf(OUTBOX))
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(
            UserAccountDto(
                userId = USER_ID,
                letsCareerUserId = LETSCAREER_USER_ID,
                email = null,
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                joinedAt = UPDATED_AT,
            ),
        )
    }

    private fun givenProfile(nicknameUpdatedAt: LocalDateTime?) {
        Mockito.`when`(userProfileReader.read(USER_ID)).thenReturn(
            UserProfileDto(
                name = null,
                email = null,
                phoneNum = null,
                letsCareerAuthProvider = null,
                notificationEmail = null,
                nickname = "오공고",
                profileImageUrl = null,
                university = null,
                major = null,
                grade = null,
                wishField = null,
                wishJob = null,
                wishIndustry = null,
                wishEmploymentType = null,
                wishCompany = null,
                nicknameUpdatedAt = nicknameUpdatedAt,
            ),
        )
    }

    companion object {
        private const val USER_ID = 17L
        private const val LETSCAREER_USER_ID = 4821L
        private val UPDATED_AT: LocalDateTime = LocalDateTime.of(2026, 10, 12, 10, 0)
        private val EXPECTED_COMMAND = LetsCareerNicknameReplaceCommand(nickname = "오공고", updatedAt = UPDATED_AT)
        private val OUTBOX = LetsCareerNicknameOutboxDto(userId = USER_ID, requestedAt = UPDATED_AT, attemptCount = 0)
        private val CLOCK: Clock = Clock.fixed(Instant.parse("2026-10-12T02:00:00Z"), ZoneId.of("Asia/Seoul"))
        private val SENT_AT: LocalDateTime = LocalDateTime.of(2026, 10, 12, 11, 0)
    }
}
