package com.ogonggo.adminapi.adposting.presentation

import com.ogonggo.adminapi.adposting.business.AdminAdPostingAdvertiserResult
import com.ogonggo.adminapi.adposting.business.AdminAdPostingContractResult
import com.ogonggo.adminapi.adposting.business.AdminAdPostingCreateCommand
import com.ogonggo.adminapi.adposting.business.AdminAdPostingOrderCommand
import com.ogonggo.adminapi.adposting.business.AdminAdPostingResult
import com.ogonggo.adminapi.adposting.business.AdminAdPostingService
import com.ogonggo.adminapi.adposting.business.AdminAdPostingUpdateCommand
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.domain.AdPostingTargetType
import com.ogonggo.core.advertiser.domain.AdvertiserType
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.Optional

@WebMvcTest(controllers = [AdminAdPostingController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminAdPostingControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminAdPostingService: AdminAdPostingService

    @Test
    fun `자리별 목록은 대상·광고주·계약 요약을 싣고 열린 기간은 null로 준다`() {
        // given
        Mockito.`when`(adminAdPostingService.getAdPostings(AdPostingPlacement.JOB_LIST, null)).thenReturn(listOf(result()))

        // when & then
        mockMvc.perform(admin(get(PATH).param("placement", "JOB_LIST")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(AD_POSTING_ID))
            .andExpect(jsonPath("$.data[0].targetType").value("JOB"))
            .andExpect(jsonPath("$.data[0].targetTitle").value("백엔드 개발자"))
            .andExpect(jsonPath("$.data[0].targetAvailable").value(true))
            .andExpect(jsonPath("$.data[0].startAt").value("2026-11-01T00:00:00"))
            .andExpect(jsonPath("$.data[0].endAt").isEmpty)
            .andExpect(jsonPath("$.data[0].advertiser.type").value("NON_MEMBER"))
            .andExpect(jsonPath("$.data[0].contract.endAt").value("2026-12-01T00:00:00"))
    }

    @Test
    fun `목록의 자리와 광고주 id가 잘못되면 화면 문구로 400을 준다`() {
        mockMvc.perform(admin(get(PATH).param("placement", "HOME")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(PLACEMENT_MESSAGE))
        mockMvc.perform(admin(get(PATH).param("advertiserId", "abc")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("advertiserId: 광고주 id 를 숫자로 보내 주세요."))
    }

    @Test
    fun `광고주로 거르면 자리 없이도 부른다`() {
        // given
        Mockito.`when`(adminAdPostingService.getAdPostings(null, ADVERTISER_ID)).thenReturn(listOf(result()))

        // when & then
        mockMvc.perform(admin(get(PATH).param("advertiserId", ADVERTISER_ID.toString())))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].advertiserId").value(ADVERTISER_ID))
    }

    @Test
    fun `등록하면 201과 등록한 광고 공고를 준다`() {
        // given
        val command = AdminAdPostingCreateCommand(
            placement = AdPostingPlacement.JOB_LIST,
            targetId = 13,
            advertiserId = ADVERTISER_ID,
            contractId = null,
            startAt = START,
            endAt = null,
            enabled = true,
        )
        Mockito.`when`(adminAdPostingService.createAdPosting(command)).thenReturn(AD_POSTING_ID)
        Mockito.`when`(adminAdPostingService.getAdPosting(AD_POSTING_ID)).thenReturn(result())

        // when & then
        mockMvc.perform(
            admin(post(PATH))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"placement":"JOB_LIST","targetId":13,"advertiserId":3,"contractId":null,
                     "startAt":"2026-11-01T00:00:00","endAt":null,"enabled":true}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.displayOrder").value(1))
    }

    @Test
    fun `수정은 null을 보낸 칸은 지우고 보내지 않은 칸은 그대로 둔다`() {
        // given
        Mockito.`when`(adminAdPostingService.getAdPosting(AD_POSTING_ID)).thenReturn(result())

        // when
        mockMvc.perform(
            admin(patch("$PATH/$AD_POSTING_ID"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"contractId":null,"enabled":false}"""),
        )
            .andExpect(status().isOk)

        // then
        Mockito.verify(adminAdPostingService)
            .updateAdPosting(AD_POSTING_ID, AdminAdPostingUpdateCommand(contractId = Optional.empty(), enabled = false))
    }

    @Test
    fun `순서를 바꾸면 그 자리의 새 순서를 준다`() {
        // given
        val command = AdminAdPostingOrderCommand(AdPostingPlacement.JOB_PICK, listOf(2, 1))
        Mockito.`when`(adminAdPostingService.replaceOrder(command)).thenReturn(AdPostingPlacement.JOB_PICK)
        Mockito.`when`(adminAdPostingService.getAdPostings(AdPostingPlacement.JOB_PICK, null)).thenReturn(listOf(result()))

        // when & then
        mockMvc.perform(
            admin(put("$PATH/order"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"placement":"JOB_PICK","adPostingIds":[2,1]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(AD_POSTING_ID))
    }

    @Test
    fun `삭제하면 지운 광고 공고의 식별자를 준다`() {
        mockMvc.perform(admin(delete("$PATH/$AD_POSTING_ID")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(AD_POSTING_ID))

        Mockito.verify(adminAdPostingService).deleteAdPosting(AD_POSTING_ID)
    }

    @Test
    fun `관리자가 아니면 부를 수 없다`() {
        mockMvc.perform(get(PATH).param("placement", "JOB_LIST"))
            .andExpect(status().isUnauthorized)
    }

    private fun result() = AdminAdPostingResult(
        id = AD_POSTING_ID,
        placement = AdPostingPlacement.JOB_LIST,
        targetType = AdPostingTargetType.JOB,
        targetId = 13,
        targetTitle = "백엔드 개발자",
        targetCompanyName = "고위드",
        targetAvailable = true,
        startAt = START,
        endAt = null,
        enabled = true,
        displayOrder = 1,
        advertiserId = ADVERTISER_ID,
        advertiser = AdminAdPostingAdvertiserResult(ADVERTISER_ID, "고위드", AdvertiserType.NON_MEMBER),
        contractId = 21,
        contract = AdminAdPostingContractResult(21, null, START, START.plusMonths(1)),
        createdAt = START,
        updatedAt = START,
    )

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private companion object {
        const val PATH = "/api/v1/admin/ad-postings"
        const val AD_POSTING_ID = 5L
        const val ADVERTISER_ID = 3L
        const val PLACEMENT_MESSAGE =
            "placement: JOB_LIST, JOB_PICK, BOOTCAMP_LIST, JOB_DETAIL_SIDE, CALENDAR, MYPAGE_SCRAP 중 하나를 보내 주세요."
        val START: LocalDateTime = LocalDateTime.of(2026, 11, 1, 0, 0)
    }
}
