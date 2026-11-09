package com.ogonggo.userapi.advertisementcontract.presentation

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import com.ogonggo.userapi.advertisementcontract.business.AdvertisementContractService
import com.ogonggo.userapi.advertisementcontract.business.CreateAdvertisementContractCommand
import com.ogonggo.userapi.auth.error.AuthErrorCode
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
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

@WebMvcTest(controllers = [AdvertisementContractController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class AdvertisementContractControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var advertisementContractService: AdvertisementContractService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    @DisplayName("비회원이 광고 신청을 등록하고 계약 ID를 받는다")
    fun `비회원이 광고 신청을 등록하고 계약 ID를 받는다`() {
        // given
        Mockito.`when`(advertisementContractService.createContract(null, VALID_COMMAND)).thenReturn(CONTRACT_ID)

        // when
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.id").value(CONTRACT_ID))

        Mockito.verify(advertisementContractService).createContract(null, VALID_COMMAND)
    }

    @Test
    @DisplayName("유효한 선택 토큰은 회원 ID와 함께 신청한다")
    fun `유효한 선택 토큰은 회원 ID와 함께 신청한다`() {
        // given
        Mockito.`when`(ogonggoTokenProvider.parseAccessToken(ACCESS_TOKEN)).thenReturn(USER_ID)
        Mockito.`when`(advertisementContractService.createContract(USER_ID, VALID_COMMAND)).thenReturn(CONTRACT_ID)

        // when
        mockMvc.perform(
            post(PATH)
                .header("Authorization", "Bearer $ACCESS_TOKEN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value(CONTRACT_ID))

        Mockito.verify(advertisementContractService).createContract(USER_ID, VALID_COMMAND)
    }

    @Test
    @DisplayName("유효하지 않은 선택 토큰은 비회원 신청으로 처리한다")
    fun `유효하지 않은 선택 토큰은 비회원 신청으로 처리한다`() {
        // given
        Mockito.`when`(ogonggoTokenProvider.parseAccessToken(ACCESS_TOKEN))
            .thenThrow(UnauthorizedException(AuthErrorCode.INVALID_TOKEN))
        Mockito.`when`(advertisementContractService.createContract(null, VALID_COMMAND)).thenReturn(CONTRACT_ID)

        // when
        mockMvc.perform(
            post(PATH)
                .header("Authorization", "Bearer $ACCESS_TOKEN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value(CONTRACT_ID))

        Mockito.verify(advertisementContractService).createContract(null, VALID_COMMAND)
    }

    @Test
    @DisplayName("사업자 번호와 카카오톡 CRM 카테고리를 함께 신청한다")
    fun `사업자 번호와 카카오톡 CRM 카테고리를 함께 신청한다`() {
        // given
        val body = VALID_BODY
            .replace("\"categories\": [\"BANNER\", \"INSTAGRAM_1\"]", "\"categories\": [\"KAKAO_CRM\"]")
            .replace(BUSINESS_NUMBER_ANCHOR, "$BUSINESS_NUMBER_ANCHOR\n  \"businessNumberType\": \"BUSINESS_REGISTRATION\",\n  \"businessNumber\": \"1234567890\",")
        val command = VALID_COMMAND.copy(
            categories = setOf(AdvertisementProductCategory.KAKAO_CRM),
            businessNumberType = CompanyBusinessNumberType.BUSINESS_REGISTRATION,
            businessNumber = "1234567890",
        )
        Mockito.`when`(advertisementContractService.createContract(null, command)).thenReturn(CONTRACT_ID)

        // when
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value(CONTRACT_ID))

        Mockito.verify(advertisementContractService).createContract(null, command)
    }

    @Test
    @DisplayName("사업자 번호는 기업 회원가입과 같이 종류와 자릿수가 맞지 않으면 400으로 응답한다")
    fun `사업자 번호는 기업 회원가입과 같이 종류와 자릿수가 맞지 않으면 400으로 응답한다`() {
        // given
        val invalidPairs = listOf(
            "\"businessNumber\": \"1234567890\"",
            "\"businessNumberType\": \"BUSINESS_REGISTRATION\"",
            "\"businessNumberType\": \"BUSINESS_REGISTRATION\", \"businessNumber\": \"123456789\"",
            "\"businessNumberType\": \"CORPORATE_REGISTRATION\", \"businessNumber\": \"1234567890\"",
            "\"businessNumberType\": \"OVERSEAS\", \"businessNumber\": \"1234567890\"",
            "\"businessNumberType\": \"BUSINESS_REGISTRATION\", \"businessNumber\": \"123-45-67890\"",
        )

        // when
        invalidPairs.forEachIndexed { index, pair ->
            val body = VALID_BODY.replace(BUSINESS_NUMBER_ANCHOR, "$BUSINESS_NUMBER_ANCHOR $pair,")
            val response = mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn()
                .response
            // then
            assertEquals(400, response.status, "invalid request #$index returned ${response.status}: ${response.contentAsString}")
            org.hamcrest.MatcherAssert.assertThat(
                response.contentAsString,
                org.hamcrest.Matchers.containsString("\"code\":\"BAD_REQUEST\""),
            )
        }

        Mockito.verifyNoInteractions(advertisementContractService)
    }

    @Test
    @DisplayName("이메일 URL 필수값과 카테고리 검증 실패는 400으로 응답한다")
    fun `이메일 URL 필수값과 카테고리 검증 실패는 400으로 응답한다`() {
        // given
        val invalidRequests = listOf(
            VALID_BODY.replace("marketing@example.com", "not-an-email"),
            VALID_BODY.replace("https://example.com/recruit/backend", "ftp://example.com/recruit/backend"),
            VALID_BODY.replace("\"categories\": [\"BANNER\", \"INSTAGRAM_1\"]", "\"categories\": []"),
            VALID_BODY.replace("\"companyName\": \"샘플기업\"", "\"companyName\": \" \""),
            VALID_BODY.replace("\"managerName\": \"김담당\",", ""),
        )

        // when
        invalidRequests.forEachIndexed { index, body ->
            val response = mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn()
                .response
            // then
            assertEquals(400, response.status, "invalid request #$index returned ${response.status}: ${response.contentAsString}")
            org.hamcrest.MatcherAssert.assertThat(
                response.contentAsString,
                org.hamcrest.Matchers.containsString("\"code\":\"BAD_REQUEST\""),
            )
        }

        Mockito.verifyNoInteractions(advertisementContractService)
    }

    private companion object {
        const val PATH = "/api/v1/advertisement-contracts"
        const val ACCESS_TOKEN = "user-access-token"
        const val USER_ID = 42L
        const val CONTRACT_ID = 1001L
        const val BUSINESS_NUMBER_ANCHOR = "\"phoneNumber\": \"02-1234-5678\","
        val VALID_BODY = """
            {
              "promotionTarget": "JOB",
              "companyName": "샘플기업",
              "recruitmentTitle": "백엔드 인턴 채용",
              "recruitmentUrl": "https://example.com/recruit/backend",
              "recruitmentDeadline": "2026-12-31",
              "purpose": "FIT_APPLICANTS",
              "categories": ["BANNER", "INSTAGRAM_1"],
              "scheduleRequest": "11월 중 1주 집행을 희망합니다.",
              "advertisementRequest": "인턴 지원자를 대상으로 알려주세요.",
              "managerName": "김담당",
              "email": "marketing@example.com",
              "phoneNumber": "02-1234-5678",
              "invoiceMethod": "TAX_INVOICE",
              "creativeMethod": "REQUEST_PRODUCTION"
            }
        """.trimIndent()
        val VALID_COMMAND = CreateAdvertisementContractCommand(
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            companyName = "샘플기업",
            recruitmentTitle = "백엔드 인턴 채용",
            recruitmentUrl = "https://example.com/recruit/backend",
            recruitmentDeadline = java.time.LocalDate.of(2026, 12, 31),
            purpose = AdvertisementContractPurpose.FIT_APPLICANTS,
            categories = setOf(AdvertisementProductCategory.BANNER, AdvertisementProductCategory.INSTAGRAM_1),
            scheduleRequest = "11월 중 1주 집행을 희망합니다.",
            advertisementRequest = "인턴 지원자를 대상으로 알려주세요.",
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "02-1234-5678",
            invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
            creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
        )
    }
}
