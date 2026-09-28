package com.ogonggo.userapi.challenge.business

import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.userapi.challenge.implement.LetsCareerChallengeClient
import com.ogonggo.userapi.challenge.implement.dto.RecommendedChallengeDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime

class UserChallengeServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val letsCareerChallengeClient = Mockito.mock(LetsCareerChallengeClient::class.java)
    private val service = UserChallengeService(userReader, letsCareerChallengeClient)

    init {
        Mockito.`when`(letsCareerChallengeClient.readRecommended(null)).thenReturn(listOf(challenge(1L)))
        Mockito.`when`(letsCareerChallengeClient.readRecommended(LETS_CAREER_USER_ID)).thenReturn(listOf(challenge(2L)))
    }

    @Test
    fun `일반 회원은 자기 렛츠커리어 계정에 맞춘 추천을 받는다`() {
        // given
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(account(UserRole.USER, LETS_CAREER_USER_ID))

        // when
        val challenges = service.getRecommendedChallenges(USER_ID)

        // then
        assertEquals(listOf(2L), challenges.map { it.challengeId })
    }

    @Test
    fun `렛츠커리어 계정이 없는 기업 회원은 비로그인과 같은 추천을 받는다`() {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(account(UserRole.COMPANY, letsCareerUserId = null))

        assertEquals(listOf(1L), service.getRecommendedChallenges(USER_ID).map { it.challengeId })
    }

    @Test
    fun `비로그인은 사용자를 조회하지 않고 추천을 받는다`() {
        assertEquals(listOf(1L), service.getRecommendedChallenges(null).map { it.challengeId })

        Mockito.verifyNoInteractions(userReader)
    }

    private fun account(role: UserRole, letsCareerUserId: Long?) = UserAccountDto(
        userId = USER_ID,
        letsCareerUserId = letsCareerUserId,
        email = null,
        status = UserStatus.ACTIVE,
        role = role,
        joinedAt = LocalDateTime.of(2026, 9, 1, 10, 0),
    )

    private fun challenge(challengeId: Long) = RecommendedChallengeDto(
        challengeId = challengeId,
        title = "챌린지 $challengeId",
        shortDescription = null,
        thumbnailUrl = null,
        recruitmentStartAt = null,
        recruitmentEndAt = null,
        programStartAt = null,
        programEndAt = null,
    )

    private companion object {
        const val USER_ID = 17L
        const val LETS_CAREER_USER_ID = 42L
    }
}
