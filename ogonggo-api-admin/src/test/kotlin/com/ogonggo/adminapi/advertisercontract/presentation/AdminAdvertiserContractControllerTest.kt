package com.ogonggo.adminapi.advertisercontract.presentation

import com.ogonggo.adminapi.advertisercontract.business.AdminAdvertiserContractCreateCommand
import com.ogonggo.adminapi.advertisercontract.business.AdminAdvertiserContractResult
import com.ogonggo.adminapi.advertisercontract.business.AdminAdvertiserContractService
import com.ogonggo.adminapi.advertisercontract.business.AdminAdvertiserContractUpdateCommand
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.advertisercontract.domain.AdContractProduct
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@WebMvcTest(controllers = [AdminAdvertiserContractController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminAdvertiserContractControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminAdvertiserContractService: AdminAdvertiserContractService

    @Test
    fun `계약을 등록하면 201과 등록한 계약을 KST 시각과 날짜로 준다`() {
        // given
        val command = AdminAdvertiserContractCreateCommand(
            name = "11월 홈 배너",
            startAt = START,
            endAt = START.plusMonths(1),
            amount = 1_000_000,
            vatIncluded = true,
            paid = true,
            paidDate = LocalDate.of(2026, 11, 2),
            products = listOf(AdContractProduct.BANNER_HOME_TOP),
            memo = null,
        )
        Mockito.`when`(adminAdvertiserContractService.createContract(ADVERTISER_ID, command)).thenReturn(CONTRACT_ID)
        Mockito.`when`(adminAdvertiserContractService.getContract(CONTRACT_ID)).thenReturn(result())

        // when & then
        mockMvc.perform(
            admin(post("/api/v1/admin/advertisers/$ADVERTISER_ID/contracts"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"11월 홈 배너","startAt":"2026-11-01T00:00:00","endAt":"2026-12-01T00:00:00","amount":1000000,
                     "vatIncluded":true,"paid":true,"paidDate":"2026-11-02","products":["BANNER_HOME_TOP"],"memo":null}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value(CONTRACT_ID))
            .andExpect(jsonPath("$.data.startAt").value("2026-11-01T00:00:00"))
            .andExpect(jsonPath("$.data.paidDate").value("2026-11-02"))
            .andExpect(jsonPath("$.data.products[0]").value("BANNER_HOME_TOP"))
            .andExpect(jsonPath("$.data.bannerCount").value(0))
            .andExpect(jsonPath("$.data.adPostingCount").value(0))
            .andExpect(jsonPath("$.data.memo").isEmpty)
    }

    @Test
    fun `수정은 null을 보낸 칸은 지우고 보내지 않은 칸은 그대로 둔다`() {
        // given
        val command = AdminAdvertiserContractUpdateCommand(name = Optional.empty(), paid = false, memo = Optional.of("메모"))
        Mockito.`when`(adminAdvertiserContractService.getContract(CONTRACT_ID)).thenReturn(result())

        // when
        mockMvc.perform(
            admin(patch("/api/v1/admin/advertiser-contracts/$CONTRACT_ID"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":null,"paid":false,"memo":"메모"}"""),
        )
            .andExpect(status().isOk)

        // then
        Mockito.verify(adminAdvertiserContractService).updateContract(CONTRACT_ID, command)
    }

    @Test
    fun `삭제하면 지운 계약의 식별자를 준다`() {
        mockMvc.perform(admin(delete("/api/v1/admin/advertiser-contracts/$CONTRACT_ID")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(CONTRACT_ID))

        Mockito.verify(adminAdvertiserContractService).deleteContract(CONTRACT_ID)
    }

    private fun result() = AdminAdvertiserContractResult(
        id = CONTRACT_ID,
        advertiserId = ADVERTISER_ID,
        name = "11월 홈 배너",
        startAt = START,
        endAt = START.plusMonths(1),
        amount = 1_000_000,
        vatIncluded = true,
        paid = true,
        paidDate = LocalDate.of(2026, 11, 2),
        products = listOf(AdContractProduct.BANNER_HOME_TOP),
        memo = null,
        bannerCount = 0,
        adPostingCount = 0,
        createdAt = START,
        updatedAt = START,
    )

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private companion object {
        const val ADVERTISER_ID = 3L
        const val CONTRACT_ID = 11L
        val START: LocalDateTime = LocalDateTime.of(2026, 11, 1, 0, 0)
    }
}
