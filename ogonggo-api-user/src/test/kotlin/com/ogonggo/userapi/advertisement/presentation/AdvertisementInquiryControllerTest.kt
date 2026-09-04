package com.ogonggo.userapi.advertisement.presentation

import com.ogonggo.core.error.InternalServerException
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryService
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryType
import com.ogonggo.userapi.advertisement.business.CreateAdvertisementInquiryCommand
import com.ogonggo.userapi.advertisement.error.AdvertisementErrorCode
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

@WebMvcTest(controllers = [AdvertisementInquiryController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class AdvertisementInquiryControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var advertisementInquiryService: AdvertisementInquiryService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `광고 문의는 오공고 세션 없이 접수하고 200과 빈 데이터로 응답한다`() {
        // 저장하는 리소스가 없어 201이 아니라 200으로 응답한다.
        mockMvc.perform(
            post("/api/v1/advertisement-inquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").doesNotExist())

        Mockito.verify(advertisementInquiryService).createInquiry(COMMAND)
    }

    @Test
    fun `슬랙 전달에 실패하면 503 계약으로 응답한다`() {
        Mockito.doThrow(InternalServerException(AdvertisementErrorCode.ADVERTISEMENT_INQUIRY_NOTIFICATION_FAILED))
            .`when`(advertisementInquiryService).createInquiry(COMMAND)

        mockMvc.perform(
            post("/api/v1/advertisement-inquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        )
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("ADVERTISEMENT_INQUIRY_NOTIFICATION_FAILED"))
    }

    @Test
    fun `필수값과 이메일 형식을 검증한다`() {
        listOf(
            body(companyName = "") to "companyName",
            body(email = "not-an-email") to "email",
            body(promotionAnswer = " ") to "promotionAnswer",
        ).forEach { (requestBody, field) ->
            mockMvc.perform(
                post("/api/v1/advertisement-inquiries")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(startsWith("[$field] ")))
        }

        Mockito.verifyNoInteractions(advertisementInquiryService)
    }

    @Test
    fun `정의되지 않은 신청 유형은 400으로 응답한다`() {
        mockMvc.perform(
            post("/api/v1/advertisement-inquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(inquiryType = "PAID_PROMOTION")),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(advertisementInquiryService)
    }

    companion object {
        private const val PROMOTION_ANSWER = "사람인과 잡코리아에 올리고 있습니다."

        private fun body(
            companyName: String = "렛츠커리어",
            managerName: String = "김담당",
            email: String = "manager@ogonggo.co.kr",
            phoneNumber: String = "010-1234-5678",
            inquiryType: String = "FREE_PROMOTION",
            promotionAnswer: String = PROMOTION_ANSWER,
        ): String = """
            {
              "companyName": "$companyName",
              "managerName": "$managerName",
              "email": "$email",
              "phoneNumber": "$phoneNumber",
              "inquiryType": "$inquiryType",
              "promotionAnswer": "$promotionAnswer"
            }
        """.trimIndent()

        private val REQUEST_BODY = body()

        private val COMMAND = CreateAdvertisementInquiryCommand(
            companyName = "렛츠커리어",
            managerName = "김담당",
            email = "manager@ogonggo.co.kr",
            phoneNumber = "010-1234-5678",
            inquiryType = AdvertisementInquiryType.FREE_PROMOTION,
            promotionAnswer = PROMOTION_ANSWER,
        )
    }
}
