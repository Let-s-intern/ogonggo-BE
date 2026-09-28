package com.ogonggo.userapi.challenge.presentation

import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.challenge.business.UserChallengeService
import com.ogonggo.userapi.challenge.implement.dto.RecommendedChallengeDto
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [UserChallengeController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserChallengeControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userChallengeService: UserChallengeService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `로그인 없이 추천 챌린지를 목록으로 조회한다`() {
        Mockito.`when`(userChallengeService.getRecommendedChallenges(null)).thenReturn(listOf(CHALLENGE))

        mockMvc.perform(get(PATH))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].challengeId").value(10))
            .andExpect(jsonPath("$.data[0].title").value("자소서 챌린지"))
            .andExpect(jsonPath("$.data[0].recruitmentEndAt").value("2026-10-05T23:59:59"))
    }

    @Test
    fun `로그인하면 그 사용자로 추천을 받는다`() {
        Mockito.`when`(userChallengeService.getRecommendedChallenges(USER_ID)).thenReturn(listOf(CHALLENGE))

        mockMvc.perform(get(PATH).with(authentication(UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].challengeId").value(10))
    }

    private companion object {
        const val PATH = "/api/v1/recommended-challenges"
        const val USER_ID = 17L
        val CHALLENGE = RecommendedChallengeDto(
            challengeId = 10L,
            title = "자소서 챌린지",
            shortDescription = "한 달 만에 자소서 완성",
            thumbnailUrl = "https://cdn.test/10.png",
            recruitmentStartAt = LocalDateTime.of(2026, 9, 20, 0, 0),
            recruitmentEndAt = LocalDateTime.of(2026, 10, 5, 23, 59, 59),
            programStartAt = LocalDateTime.of(2026, 10, 7, 19, 0),
            programEndAt = LocalDateTime.of(2026, 11, 4, 23, 59, 59),
        )
    }
}
