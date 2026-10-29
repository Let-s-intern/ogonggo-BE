package com.ogonggo.userapi.user.presentation

import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import com.ogonggo.core.user.domain.CompanyEmployeeCountRange
import com.ogonggo.core.user.domain.CompanyManagerDuty
import com.ogonggo.core.user.domain.CompanyType
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.dto.CompanyManagerInfoUpdateDto
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.user.business.CompanyBasicInfoCommand
import com.ogonggo.userapi.user.business.UserAccountService
import org.hamcrest.Matchers.startsWith
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
    fun `기관명과 로고 이미지 식별자를 받아 기본 정보를 교체하고 로고를 빼면 지운다`() {
        mockMvc.perform(
            put(BASIC_INFO_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"organizationName":"오공고","logoImageId":"logo-image"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)

        mockMvc.perform(
            put(BASIC_INFO_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"organizationName":"오공고"}"""),
        )
            .andExpect(status().isOk)

        Mockito.verify(userAccountService).replaceMyCompanyBasicInfo(
            USER_ID,
            CompanyBasicInfoCommand(organizationName = "오공고", logoImageId = "logo-image"),
        )
        Mockito.verify(userAccountService).replaceMyCompanyBasicInfo(USER_ID, BASIC_INFO_WITHOUT_LOGO)
    }

    @Test
    fun `기관명이 빠지거나 로고 이미지 식별자가 공백이면 400으로 막고 교체하지 않는다`() {
        listOf(
            """{"logoImageId":"logo-image"}""",
            """{"organizationName":" "}""",
            """{"organizationName":"오공고","logoImageId":""}""",
        ).forEach { body ->
            mockMvc.perform(
                put(BASIC_INFO_PATH).with(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.verifyNoInteractions(userAccountService)
    }

    @Test
    fun `담당자 정보를 함께 받아 교체하고 선택 값을 빼면 비운다`() {
        mockMvc.perform(
            put(MANAGER_INFO_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"managerName":"이담당","managerPhone":"010-1234-5678","notificationEmail":"hr@example.com"}""",
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)

        mockMvc.perform(
            put(MANAGER_INFO_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"managerName":"이담당"}"""),
        )
            .andExpect(status().isOk)

        Mockito.verify(userAccountService).replaceMyCompanyManagerInfo(
            USER_ID,
            CompanyManagerInfoUpdateDto(
                managerName = "이담당",
                managerPhone = "010-1234-5678",
                notificationEmail = "hr@example.com",
            ),
        )
        Mockito.verify(userAccountService).replaceMyCompanyManagerInfo(USER_ID, MANAGER_INFO_WITHOUT_OPTIONALS)
    }

    @Test
    fun `담당자 정보가 빠지거나 형식에 맞지 않으면 400으로 막고 교체하지 않는다`() {
        listOf(
            """{}""",
            """{"managerName":" "}""",
            """{"managerName":"이담당","managerPhone":"010-abcd"}""",
            """{"managerName":"이담당","managerPhone":""}""",
            """{"managerName":"이담당","notificationEmail":"hr"}""",
            """{"managerName":"이담당","notificationEmail":""}""",
        ).forEach { body ->
            mockMvc.perform(
                put(MANAGER_INFO_PATH).with(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.verifyNoInteractions(userAccountService)
    }

    @Test
    fun `가입 때 받은 기업 정보를 함께 받아 넘기고 빼면 null로 넘겨 저장된 값을 그대로 두게 한다`() {
        mockMvc.perform(
            put(BASIC_INFO_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"organizationName":"오공고","businessNumberType":"BUSINESS_REGISTRATION","businessNumber":"1234567890",
                     "companyType":"STARTUP","employeeCountRange":"RANGE_300_999","industry":"IT·정보통신",
                     "address":"서울특별시 마포구 양화로 1","homepageUrl":"https://www.letscareer.co.kr"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
        mockMvc.perform(
            put(MANAGER_INFO_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"managerName":"이담당","managerTitle":"인사팀 매니저","managerDuty":"HR","recommender":"홍길동"}"""),
        )
            .andExpect(status().isOk)

        Mockito.verify(userAccountService).replaceMyCompanyBasicInfo(
            USER_ID,
            CompanyBasicInfoCommand(
                organizationName = "오공고",
                logoImageId = null,
                businessNumberType = CompanyBusinessNumberType.BUSINESS_REGISTRATION,
                businessNumber = "1234567890",
                companyType = CompanyType.STARTUP,
                employeeCountRange = CompanyEmployeeCountRange.RANGE_300_999,
                industry = "IT·정보통신",
                address = "서울특별시 마포구 양화로 1",
                homepageUrl = "https://www.letscareer.co.kr",
            ),
        )
        Mockito.verify(userAccountService).replaceMyCompanyManagerInfo(
            USER_ID,
            CompanyManagerInfoUpdateDto(
                managerName = "이담당",
                managerPhone = null,
                notificationEmail = null,
                managerTitle = "인사팀 매니저",
                managerDuty = CompanyManagerDuty.HR,
                recommender = "홍길동",
            ),
        )
    }

    @Test
    fun `사업자 번호가 종류와 맞지 않거나 가입 정보 글자 값이 공백이면 어느 필드인지 400으로 알리고 교체하지 않는다`() {
        mapOf(
            """{"organizationName":"오공고","businessNumber":"1234567890"}""" to "[businessNumberType]",
            """{"organizationName":"오공고","businessNumberType":"BUSINESS_REGISTRATION","businessNumber":"123456789"}""" to
                "[businessNumber]",
            """{"organizationName":"오공고","businessNumberType":"OVERSEAS","businessNumber":"1234567890"}""" to
                "[businessNumber]",
            """{"organizationName":"오공고","businessNumberType":"BUSINESS_REGISTRATION","businessNumber":"123-45-6789"}""" to
                "[businessNumber]",
            """{"organizationName":"오공고","homepageUrl":"letscareer"}""" to "[homepageUrl]",
            """{"organizationName":"오공고","industry":" "}""" to "[industry]",
        ).forEach { (body, field) ->
            mockMvc.perform(
                put(BASIC_INFO_PATH).with(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(startsWith(field)))
        }
        mockMvc.perform(
            put(MANAGER_INFO_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"managerName":"이담당","recommender":" "}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(startsWith("[recommender]")))

        Mockito.verifyNoInteractions(userAccountService)
    }

    @Test
    fun `기업 회원이 아니면 403 COMPANY_ROLE_REQUIRED로 응답한다`() {
        Mockito.`when`(
            userAccountService.replaceMyCompanyBasicInfo(USER_ID, BASIC_INFO_WITHOUT_LOGO),
        ).thenThrow(ForbiddenException(UserErrorCode.COMPANY_ROLE_REQUIRED))

        mockMvc.perform(
            put(BASIC_INFO_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"organizationName":"오공고"}"""),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("COMPANY_ROLE_REQUIRED"))
    }

    @Test
    fun `인증이 없으면 401로 응답한다`() {
        mockMvc.perform(
            put(BASIC_INFO_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"organizationName":"오공고"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        mockMvc.perform(
            put(MANAGER_INFO_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"managerName":"이담당"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    companion object {
        private const val BASIC_INFO_PATH = "/api/v1/users/me/company-profile/basic-info"
        private const val MANAGER_INFO_PATH = "/api/v1/users/me/company-profile/manager-info"
        private const val USER_ID = 17L
        private val BASIC_INFO_WITHOUT_LOGO = CompanyBasicInfoCommand(organizationName = "오공고", logoImageId = null)
        private val MANAGER_INFO_WITHOUT_OPTIONALS = CompanyManagerInfoUpdateDto(
            managerName = "이담당",
            managerPhone = null,
            notificationEmail = null,
        )
    }
}
