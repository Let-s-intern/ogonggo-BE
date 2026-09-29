package com.ogonggo.userapi.user.implement

import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.LetsCareerJobProfileOutboxManager
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.LetsCareerJobProfileOutboxDto
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.core.user.implement.dto.UserProfileDto
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.web.client.ResourceAccessException
import java.time.LocalDateTime

class LetsCareerJobProfileSyncSchedulerTest {

    private val outboxManager = Mockito.mock(LetsCareerJobProfileOutboxManager::class.java)
    private val userReader = Mockito.mock(UserReader::class.java)
    private val userProfileReader = Mockito.mock(UserProfileReader::class.java)
    private val letsCareerUserClient = Mockito.mock(LetsCareerUserClient::class.java)
    private val scheduler = LetsCareerJobProfileSyncScheduler(
        outboxManager,
        userReader,
        userProfileReader,
        letsCareerUserClient,
        SchedulerExecutionObserver(SimpleMeterRegistry()),
    )

    @Test
    fun `보낼 때의 최신 값과 고친 일시를 렛츠커리어 사용자 식별자로 보내고 성공하면 지운다`() {
        // given
        givenPending()
        givenProfile(jobInfoUpdatedAt = UPDATED_AT)

        // when
        scheduler.sendPending()

        // then
        Mockito.verify(letsCareerUserClient).replaceJobProfile(LETSCAREER_USER_ID, EXPECTED_COMMAND)
        Mockito.verify(outboxManager).markSent(OUTBOX)
    }

    @Test
    fun `보내지 못하면 지우지 않고 실패로 남겨 다음 주기에 다시 보낸다`() {
        // given
        givenPending()
        givenProfile(jobInfoUpdatedAt = UPDATED_AT)
        Mockito.doThrow(ResourceAccessException("timeout"))
            .`when`(letsCareerUserClient).replaceJobProfile(LETSCAREER_USER_ID, EXPECTED_COMMAND)

        // when
        scheduler.sendPending()

        // then
        Mockito.verify(outboxManager).markFailed(OUTBOX)
        Mockito.verify(outboxManager, Mockito.never()).markSent(OUTBOX)
    }

    @Test
    fun `보낼 값이 없으면 호출하지 않고 행을 지운다`() {
        // given
        givenPending()
        givenProfile(jobInfoUpdatedAt = null)

        // when
        scheduler.sendPending()

        // then
        Mockito.verifyNoInteractions(letsCareerUserClient)
        Mockito.verify(outboxManager).markSent(OUTBOX)
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

    private fun givenProfile(jobInfoUpdatedAt: LocalDateTime?) {
        Mockito.`when`(userProfileReader.read(USER_ID)).thenReturn(
            UserProfileDto(
                name = null,
                email = null,
                phoneNum = null,
                letsCareerAuthProvider = null,
                notificationEmail = null,
                nickname = null,
                profileImageUrl = null,
                university = "오공고대학교",
                major = null,
                grade = null,
                wishField = "개발",
                wishJob = null,
                wishIndustry = null,
                wishEmploymentType = null,
                wishCompany = null,
                jobInfoUpdatedAt = jobInfoUpdatedAt,
            ),
        )
    }

    companion object {
        private const val USER_ID = 17L
        private const val LETSCAREER_USER_ID = 4821L
        private val UPDATED_AT: LocalDateTime = LocalDateTime.of(2026, 9, 29, 10, 0)
        private val EXPECTED_COMMAND = LetsCareerJobProfileReplaceCommand(
            university = "오공고대학교",
            major = null,
            grade = null,
            wishField = "개발",
            wishJob = null,
            wishIndustry = null,
            wishEmploymentType = null,
            wishCompany = null,
            updatedAt = UPDATED_AT,
        )
        private val OUTBOX = LetsCareerJobProfileOutboxDto(userId = USER_ID, requestedAt = UPDATED_AT, attemptCount = 0)
    }
}
