package com.ogonggo.userapi.auth.presentation

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import com.ogonggo.core.user.domain.CompanyEmployeeCountRange
import com.ogonggo.core.user.domain.CompanyManagerDuty
import com.ogonggo.core.user.domain.CompanyType
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.userapi.auth.business.AuthTokens
import com.ogonggo.userapi.auth.business.CompanyAuthService
import com.ogonggo.userapi.auth.business.CompanySignInCommand
import com.ogonggo.userapi.auth.business.CompanySignUpCommand
import com.ogonggo.userapi.auth.error.AuthErrorCode
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [CompanyAuthController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class CompanyAuthControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var companyAuthService: CompanyAuthService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `기업 회원가입은 오공고 세션 없이 호출하고 201로 응답한다`() {
        Mockito.`when`(companyAuthService.signUp(SIGN_UP_COMMAND)).thenReturn(TOKENS)

        mockMvc.perform(
            post("/api/v1/auth/company/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SIGN_UP_BODY),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.accessToken").value("og-access"))
            .andExpect(jsonPath("$.data.refreshToken").value("og-refresh"))
    }

    @Test
    fun `이미 쓰는 이메일이면 409 계약으로 응답한다`() {
        Mockito.`when`(companyAuthService.signUp(SIGN_UP_COMMAND))
            .thenThrow(ConflictException(UserErrorCode.EMAIL_ALREADY_EXISTS))

        mockMvc.perform(
            post("/api/v1/auth/company/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SIGN_UP_BODY),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
    }

    @Test
    fun `이메일 형식과 비밀번호 길이를 검증한다`() {
        listOf(
            """{"email":"not-an-email","password":"password1234","organizationName":"렛츠커리어","managerName":"김담당"}"""
                to "email",
            """{"email":"company@example.com","password":"short","organizationName":"렛츠커리어","managerName":"김담당"}"""
                to "password",
        ).forEach { (body, field) ->
            mockMvc.perform(
                post("/api/v1/auth/company/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(startsWith("[$field] ")))
        }

        Mockito.verifyNoInteractions(companyAuthService)
    }

    @Test
    fun `기업·담당자 정보를 함께 보내면 가입 명령에 그대로 담는다`() {
        Mockito.`when`(companyAuthService.signUp(FULL_SIGN_UP_COMMAND)).thenReturn(TOKENS)

        mockMvc.perform(
            post("/api/v1/auth/company/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FULL_SIGN_UP_BODY),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.accessToken").value("og-access"))
    }

    @Test
    fun `해외 기업은 사업자 번호 없이 가입한다`() {
        val command = SIGN_UP_COMMAND.copy(businessNumberType = CompanyBusinessNumberType.OVERSEAS)
        Mockito.`when`(companyAuthService.signUp(command)).thenReturn(TOKENS)

        mockMvc.perform(
            post("/api/v1/auth/company/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signUpBody(""""businessNumberType":"OVERSEAS"""")),
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `사업자 번호가 번호 종류와 맞지 않으면 400으로 응답한다`() {
        listOf(
            """"businessNumber":"1234567890"""" to "businessNumberType",
            """"businessNumberType":"BUSINESS_REGISTRATION","businessNumber":"1101111234567"""" to "businessNumber",
            """"businessNumberType":"CORPORATE_REGISTRATION","businessNumber":"1234567890"""" to "businessNumber",
            """"businessNumberType":"BUSINESS_REGISTRATION"""" to "businessNumber",
            """"businessNumberType":"OVERSEAS","businessNumber":"1234567890"""" to "businessNumber",
            """"businessNumberType":"BUSINESS_REGISTRATION","businessNumber":"123-45-67890"""" to "businessNumber",
            """"homepageUrl":"not a url"""" to "homepageUrl",
            """"managerPhone":"010 1234"""" to "managerPhone",
            """"address":" """" to "address",
        ).forEach { (fields, field) ->
            mockMvc.perform(
                post("/api/v1/auth/company/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(signUpBody(fields)),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(startsWith("[$field] ")))
        }

        Mockito.verifyNoInteractions(companyAuthService)
    }

    @Test
    fun `기업 로그인은 200으로 응답한다`() {
        Mockito.`when`(companyAuthService.signIn(SIGN_IN_COMMAND)).thenReturn(TOKENS)

        mockMvc.perform(
            post("/api/v1/auth/company/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SIGN_IN_BODY),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.accessToken").value("og-access"))
    }

    @Test
    fun `자격증명이 맞지 않으면 401 계약으로 응답한다`() {
        Mockito.`when`(companyAuthService.signIn(SIGN_IN_COMMAND))
            .thenThrow(UnauthorizedException(AuthErrorCode.INVALID_COMPANY_CREDENTIALS))

        mockMvc.perform(
            post("/api/v1/auth/company/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SIGN_IN_BODY),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_COMPANY_CREDENTIALS"))
    }

    companion object {
        private val TOKENS = AuthTokens(accessToken = "og-access", refreshToken = "og-refresh")
        private const val SIGN_UP_BODY =
            """{"email":"company@example.com","password":"password1234","organizationName":"렛츠커리어","managerName":"김담당"}"""
        private const val SIGN_IN_BODY =
            """{"email":"company@example.com","password":"password1234"}"""
        private val SIGN_UP_COMMAND = CompanySignUpCommand(
            email = "company@example.com",
            password = "password1234",
            organizationName = "렛츠커리어",
            managerName = "김담당",
        )

        /** 기존 가입 화면이 보내는 네 값 뒤에 [fields]를 덧붙인다. */
        private fun signUpBody(fields: String): String =
            SIGN_UP_BODY.removeSuffix("}") + "," + fields + "}"

        private val FULL_SIGN_UP_BODY = signUpBody(
            """"businessNumberType":"CORPORATE_REGISTRATION","businessNumber":"1101111234567",""" +
                """"companyType":"STARTUP","employeeCountRange":"RANGE_1000_PLUS","industry":"IT·정보통신",""" +
                """"address":"서울특별시 마포구 양화로 1","homepageUrl":"https://www.letscareer.co.kr",""" +
                """"managerTitle":"인사팀 매니저","managerDuty":"HR","managerPhone":"010-1234-5678",""" +
                """"recommender":"홍길동"""",
        )
        private val FULL_SIGN_UP_COMMAND = SIGN_UP_COMMAND.copy(
            businessNumberType = CompanyBusinessNumberType.CORPORATE_REGISTRATION,
            businessNumber = "1101111234567",
            companyType = CompanyType.STARTUP,
            employeeCountRange = CompanyEmployeeCountRange.RANGE_1000_PLUS,
            industry = "IT·정보통신",
            address = "서울특별시 마포구 양화로 1",
            homepageUrl = "https://www.letscareer.co.kr",
            managerTitle = "인사팀 매니저",
            managerDuty = CompanyManagerDuty.HR,
            managerPhone = "010-1234-5678",
            recommender = "홍길동",
        )
        private val SIGN_IN_COMMAND = CompanySignInCommand(
            email = "company@example.com",
            password = "password1234",
        )
    }
}
