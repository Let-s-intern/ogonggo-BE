package com.ogonggo.userapi.user.presentation

import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.implement.dto.UserProfileJobInfoDto
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.user.business.LetsCareerSyncService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(
    controllers = [LetsCareerSyncController::class],
    properties = ["ogonggo.letscareer.internal-api-key=shared-internal-key"],
)
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class LetsCareerSyncControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var letsCareerSyncService: LetsCareerSyncService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `내부 API 키가 맞으면 렛츠커리어 사용자 식별자로 반영하고 결과를 돌려준다`() {
        Mockito.`when`(letsCareerSyncService.applyJobProfile(4821L, COMMAND, UPDATED_AT)).thenReturn(true)

        mockMvc.perform(
            put(PATH).header("X-Internal-Api-Key", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.applied").value(true))
    }

    @Test
    fun `키가 없거나 틀리면 401이고 사용자 토큰으로도 부를 수 없다`() {
        mockMvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            put(PATH).header("X-Internal-Api-Key", "wrong-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY),
        )
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            put(PATH).with(authentication(UsernamePasswordAuthenticationToken(17L, null, emptyList())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY),
        )
            .andExpect(status().isForbidden)

        Mockito.verifyNoInteractions(letsCareerSyncService)
    }

    companion object {
        private const val PATH = "/api/v1/internal/letscareer-users/4821/job-profile"
        private const val API_KEY = "shared-internal-key"
        private val UPDATED_AT: LocalDateTime = LocalDateTime.of(2026, 9, 29, 10, 0)
        private val COMMAND = UserProfileJobInfoDto("렛츠대학교", null, UserGrade.THIRD, "기획", null, null, null, null)
        private const val BODY =
            """{"university":"렛츠대학교","grade":"THIRD","wishField":"기획","updatedAt":"2026-09-29T10:00:00"}"""
    }
}
