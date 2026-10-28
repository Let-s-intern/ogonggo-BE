package com.ogonggo.userapi.advertisement.presentation

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryCompanyNameSource
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflow
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflowChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryMemberType
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryPromotionChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType
import com.ogonggo.core.advertisementinquiry.implement.AdvertisementInquiryAppender
import com.ogonggo.core.advertisementinquiry.implement.dto.AdvertisementInquiryAppendDto
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryNotification
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryService
import com.ogonggo.userapi.advertisement.implement.AdvertisementInquiryMailer
import com.ogonggo.userapi.advertisement.implement.AdvertisementInquiryNotifier
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.ResourceAccessException

/**
 * 회원 구분을 서버가 정하는 규칙과 알림 실패에도 저장이 남는 흐름을 HTTP 계약과 함께 확인하려고
 * 실제 서비스를 붙이고 저장·회원 조회·알림만 대역으로 둔다.
 */
@WebMvcTest(controllers = [AdvertisementInquiryController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class, AdvertisementInquiryService::class)
class AdvertisementInquiryControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userReader: UserReader

    @MockBean
    private lateinit var advertisementInquiryAppender: AdvertisementInquiryAppender

    @MockBean
    private lateinit var advertisementInquiryNotifier: AdvertisementInquiryNotifier

    @MockBean
    private lateinit var advertisementInquiryMailer: AdvertisementInquiryMailer

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    private val appended = mutableListOf<AdvertisementInquiryAppendDto>()

    @BeforeEach
    fun setUp() {
        Mockito.`when`(advertisementInquiryAppender.append(anyAppendDto())).thenAnswer { invocation ->
            appended += invocation.getArgument<AdvertisementInquiryAppendDto>(0)
            INQUIRY_ID
        }
        Mockito.`when`(userReader.readRoles(setOf(COMPANY_USER_ID))).thenReturn(mapOf(COMPANY_USER_ID to UserRole.COMPANY))
        Mockito.`when`(userReader.readRoles(setOf(GENERAL_USER_ID))).thenReturn(mapOf(GENERAL_USER_ID to UserRole.MENTOR))
    }

    @Test
    fun `비회원 문의는 회원 없이 비회원으로 저장하고 201과 식별자를 준다`() {
        mockMvc.perform(request())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.id").value(INQUIRY_ID))

        val saved = appended.single()
        assertEquals(AdvertisementInquiryMemberType.GUEST, saved.memberType)
        assertNull(saved.userId)
        assertEquals("launch_free_promo", saved.campaignCode)
        Mockito.verifyNoInteractions(userReader)
    }

    @Test
    fun `기업 회원 토큰이면 회원과 함께 기업 회원으로 저장한다`() {
        mockMvc.perform(request().with(loggedIn(COMPANY_USER_ID)))
            .andExpect(status().isCreated)

        val saved = appended.single()
        assertEquals(AdvertisementInquiryMemberType.COMPANY, saved.memberType)
        assertEquals(COMPANY_USER_ID, saved.userId)
    }

    @Test
    fun `일반 회원 토큰이면 회원과 함께 일반 회원으로 저장한다`() {
        mockMvc.perform(request().with(loggedIn(GENERAL_USER_ID)))
            .andExpect(status().isCreated)

        val saved = appended.single()
        assertEquals(AdvertisementInquiryMemberType.USER, saved.memberType)
        assertEquals(GENERAL_USER_ID, saved.userId)
    }

    @Test
    fun `클라이언트가 보낸 회원 구분과 회원 식별자는 무시한다`() {
        val forged = body().replaceFirst("{", """{"memberType":"COMPANY","userId":$COMPANY_USER_ID,""")

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(forged))
            .andExpect(status().isCreated)

        val saved = appended.single()
        assertEquals(AdvertisementInquiryMemberType.GUEST, saved.memberType)
        assertNull(saved.userId)
    }

    @Test
    fun `슬랙 알림에 실패해도 저장한 문의로 201을 주고 확인 메일은 보낸다`() {
        Mockito.doThrow(ResourceAccessException("슬랙 연결 실패"))
            .`when`(advertisementInquiryNotifier).notify(NOTIFICATION)

        mockMvc.perform(request())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value(INQUIRY_ID))

        assertEquals(1, appended.size)
        Mockito.verify(advertisementInquiryMailer).sendConfirmation(NOTIFICATION)
    }

    @Test
    fun `새 필드와 유입 원본을 저장하고 유입 경로는 서버가 판정한다`() {
        mockMvc.perform(request())
            .andExpect(status().isCreated)

        val saved = appended.single()
        assertEquals("매니저", saved.managerTitle)
        assertEquals("마케팅 인턴", saved.hiringJobName)
        assertEquals("https://example.com/jobs/1", saved.jobPostingUrl)
        assertEquals(true, saved.marketingConsent)
        assertEquals("coldmail", saved.inflow.utmSource)
        assertEquals("templateA", saved.inflow.utmContent)
        assertEquals("biz.ogonggo.co.kr", saved.inflow.entryHost)
        assertEquals(AdvertisementInquiryInflowChannel.COLDMAIL, saved.inflow.channel)
    }

    @Test
    fun `새 필드 없이 보내던 폼도 받고 마케팅 동의는 하지 않은 것으로 저장한다`() {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(LEGACY_BODY))
            .andExpect(status().isCreated)

        val saved = appended.single()
        assertNull(saved.managerTitle)
        assertFalse(saved.marketingConsent)
        assertEquals(AdvertisementInquiryInflowChannel.DIRECT, saved.inflow.channel)
    }

    @Test
    fun `필수값과 이메일 형식, 길이를 검증한다`() {
        listOf(
            body(companyName = "") to "companyName",
            body(email = "not-an-email") to "email",
            body(promotionAnswer = " ") to "promotionAnswer",
            body(utmSource = "a".repeat(101)) to "utmSource",
        ).forEach { (requestBody, field) ->
            mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(startsWith("[$field] ")))
        }

        Mockito.verifyNoInteractions(advertisementInquiryAppender)
    }

    @Test
    fun `정의되지 않은 홍보 채널과 신청 유형은 400으로 응답한다`() {
        listOf(body(promotionChannel = "BLOG"), body(inquiryType = "PAID_PROMOTION")).forEach { requestBody ->
            mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.verifyNoInteractions(advertisementInquiryAppender)
    }

    private fun request(): MockHttpServletRequestBuilder =
        post(PATH).contentType(MediaType.APPLICATION_JSON).content(body())

    private fun loggedIn(userId: Long) =
        authentication(UsernamePasswordAuthenticationToken(userId, null, emptyList()))

    /** Kotlin non-null 파라미터에 넘길 수 있도록 매처가 돌려주는 null 대신 자리 값을 돌려준다. */
    private fun anyAppendDto(): AdvertisementInquiryAppendDto {
        Mockito.any(AdvertisementInquiryAppendDto::class.java)
        return SAMPLE_APPEND_DTO
    }

    companion object {
        private const val PATH = "/api/v1/advertisement-inquiries"
        private const val INQUIRY_ID = 11L
        private const val COMPANY_USER_ID = 7L
        private const val GENERAL_USER_ID = 8L
        private const val PROMOTION_ANSWER = "사람인과 잡코리아에 올리고 있습니다."

        private fun body(
            companyName: String = "렛츠커리어",
            email: String = "manager@ogonggo.co.kr",
            inquiryType: String = "FREE_PROMOTION",
            promotionChannel: String = "OPEN_CHAT_MARKETING",
            promotionAnswer: String = PROMOTION_ANSWER,
            utmSource: String = "coldmail",
        ): String = """
            {
              "companyName": "$companyName",
              "managerName": "김담당",
              "managerTitle": "매니저",
              "email": "$email",
              "phoneNumber": "010-1234-5678",
              "inquiryType": "$inquiryType",
              "promotionChannel": "$promotionChannel",
              "hiringJobName": "마케팅 인턴",
              "jobPostingUrl": "https://example.com/jobs/1",
              "promotionAnswer": "$promotionAnswer",
              "marketingConsent": true,
              "utmSource": "$utmSource",
              "utmMedium": "email",
              "utmCampaign": "free_promo",
              "utmContent": "templateA",
              "referrer": "",
              "entryHost": "biz.ogonggo.co.kr",
              "landingPath": "/biz?utm_source=coldmail&entry=biz"
            }
        """.trimIndent()

        private val LEGACY_BODY = """
            {
              "companyName": "렛츠커리어",
              "managerName": "김담당",
              "email": "manager@ogonggo.co.kr",
              "phoneNumber": "010-1234-5678",
              "inquiryType": "FREE_PROMOTION",
              "promotionChannel": "OPEN_CHAT_MARKETING",
              "promotionAnswer": "$PROMOTION_ANSWER"
            }
        """.trimIndent()

        private val NOTIFICATION = AdvertisementInquiryNotification(
            companyName = "렛츠커리어",
            managerName = "김담당",
            email = "manager@ogonggo.co.kr",
            phoneNumber = "010-1234-5678",
            inquiryType = AdvertisementInquiryType.FREE_PROMOTION,
            promotionChannel = AdvertisementInquiryPromotionChannel.OPEN_CHAT_MARKETING,
            promotionAnswer = PROMOTION_ANSWER,
            memberType = AdvertisementInquiryMemberType.GUEST,
            inflowChannel = AdvertisementInquiryInflowChannel.COLDMAIL,
            utmSource = "coldmail",
            utmCampaign = "free_promo",
            utmContent = "templateA",
            referrer = "",
        )

        private val SAMPLE_APPEND_DTO = AdvertisementInquiryAppendDto(
            inquiryType = AdvertisementInquiryType.FREE_PROMOTION,
            campaignCode = null,
            companyName = "",
            companyNameSource = AdvertisementInquiryCompanyNameSource.MANUAL,
            managerName = "",
            managerTitle = null,
            email = "",
            phoneNumber = "",
            promotionChannel = AdvertisementInquiryPromotionChannel.INSTAGRAM,
            hiringJobName = null,
            jobPostingUrl = null,
            promotionAnswer = "",
            marketingConsent = false,
            userId = null,
            memberType = AdvertisementInquiryMemberType.GUEST,
            inflow = AdvertisementInquiryInflow(
                utmSource = null,
                utmMedium = null,
                utmCampaign = null,
                utmContent = null,
                referrer = null,
                entryHost = null,
                landingPath = null,
                channel = AdvertisementInquiryInflowChannel.DIRECT,
            ),
        )
    }
}
