package com.ogonggo.userapi.user.presentation

import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.dto.CompanyProfileUpdateDto
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.user.business.UserAccountService
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

@WebMvcTest(controllers = [CompanyProfileController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class CompanyProfileControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userAccountService: UserAccountService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `기업 정보 전체를 함께 받아 교체한다`() {
        mockMvc.perform(
            put(PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"organizationName":"오공고","managerName":"이담당",
                     "logoUrl":"https://cdn.example.com/logo.png",
                     "managerPhone":"010-1234-5678","notificationEmail":"hr@example.com"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)

        Mockito.verify(userAccountService).replaceMyCompanyProfile(
            USER_ID,
            CompanyProfileUpdateDto(
                organizationName = "오공고",
                managerName = "이담당",
                logoUrl = "https://cdn.example.com/logo.png",
                managerPhone = "010-1234-5678",
                notificationEmail = "hr@example.com",
            ),
        )
    }

    @Test
    fun `로고·연락처·수신 이메일을 빼면 비우는 값으로 교체한다`() {
        mockMvc.perform(
            put(PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"organizationName":"오공고","managerName":"이담당"}"""),
        )
            .andExpect(status().isOk)

        Mockito.verify(userAccountService).replaceMyCompanyProfile(USER_ID, COMMAND_WITHOUT_OPTIONALS)
    }

    @Test
    fun `필수 값이 빠지거나 형식에 맞지 않으면 400으로 막고 교체하지 않는다`() {
        listOf(
            """{"organizationName":"오공고"}""",
            """{"organizationName":"오공고","managerName":" "}""",
            """{"organizationName":"오공고","managerName":"이담당","logoUrl":"not-a-url"}""",
            """{"organizationName":"오공고","managerName":"이담당","logoUrl":""}""",
            """{"organizationName":"오공고","managerName":"이담당","managerPhone":"010-abcd"}""",
            """{"organizationName":"오공고","managerName":"이담당","managerPhone":""}""",
            """{"organizationName":"오공고","managerName":"이담당","notificationEmail":"hr"}""",
            """{"organizationName":"오공고","managerName":"이담당","notificationEmail":""}""",
        ).forEach { body ->
            mockMvc.perform(
                put(PATH).with(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.verifyNoInteractions(userAccountService)
    }

    @Test
    fun `기업 회원이 아니면 403 COMPANY_ROLE_REQUIRED로 응답한다`() {
        Mockito.`when`(
            userAccountService.replaceMyCompanyProfile(USER_ID, COMMAND_WITHOUT_OPTIONALS),
        ).thenThrow(ForbiddenException(UserErrorCode.COMPANY_ROLE_REQUIRED))

        mockMvc.perform(
            put(PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"organizationName":"오공고","managerName":"이담당"}"""),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("COMPANY_ROLE_REQUIRED"))
    }

    @Test
    fun `인증이 없으면 401로 응답한다`() {
        mockMvc.perform(
            put(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"organizationName":"오공고","managerName":"이담당"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    companion object {
        private const val PATH = "/api/v1/users/me/company-profile"
        private const val USER_ID = 17L
        private val COMMAND_WITHOUT_OPTIONALS = CompanyProfileUpdateDto(
            organizationName = "오공고",
            managerName = "이담당",
            logoUrl = null,
            managerPhone = null,
            notificationEmail = null,
        )
    }
}
