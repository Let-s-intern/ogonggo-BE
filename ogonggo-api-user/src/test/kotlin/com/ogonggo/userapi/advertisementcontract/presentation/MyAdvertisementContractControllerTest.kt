package com.ogonggo.userapi.advertisementcontract.presentation

import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementContractDetailResult
import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementContractPageResult
import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementContractProductResult
import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementContractService
import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementContractSummaryResult
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
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
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [MyAdvertisementContractController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class MyAdvertisementContractControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var myAdvertisementContractService: MyAdvertisementContractService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `내 광고 신청 목록을 1 기반 페이지와 상태 필터로 조회한다`() {
        // given
        val statuses = setOf(AdvertisementContractStatus.REQUESTED, AdvertisementContractStatus.NEGOTIATING)
        Mockito.`when`(myAdvertisementContractService.getContracts(USER_ID, statuses, 1, 5)).thenReturn(
            MyAdvertisementContractPageResult(items = listOf(SUMMARY), page = 1, size = 5, totalElements = 6, totalPages = 2),
        )

        // when
        mockMvc.perform(
            get(PATH)
                .param("page", "2")
                .param("size", "5")
                .param("status", "REQUESTED")
                .param("status", "NEGOTIATING")
                .with(authenticatedUser()),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(CONTRACT_ID))
            .andExpect(jsonPath("$.data.items[0].status").value("REQUESTED"))
            .andExpect(jsonPath("$.data.items[0].promotionTarget").value("JOB"))
            .andExpect(jsonPath("$.data.items[0].companyName").value("샘플기업"))
            .andExpect(jsonPath("$.data.items[0].recruitmentTitle").value("백엔드 인턴 채용"))
            .andExpect(jsonPath("$.data.items[0].recruitmentDeadline").value("2026-12-31"))
            .andExpect(jsonPath("$.data.items[0].categories[0]").value("BANNER"))
            .andExpect(jsonPath("$.data.items[0].paymentAmount").isEmpty)
            .andExpect(jsonPath("$.data.items[0].registeredAt").value("2026-11-08T10:00:00"))
            .andExpect(jsonPath("$.data.items[0].userId").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].source").doesNotExist())
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(2))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(6))
    }

    @Test
    fun `목록 기본 페이지는 1이고 크기는 10이며 상태를 보내지 않으면 거르지 않는다`() {
        // given
        Mockito.`when`(myAdvertisementContractService.getContracts(USER_ID, emptySet(), 0, 10)).thenReturn(
            MyAdvertisementContractPageResult(items = emptyList(), page = 0, size = 10, totalElements = 0, totalPages = 0),
        )

        // when
        mockMvc.perform(get(PATH).with(authenticatedUser()))
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isEmpty)
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(10))
    }

    @Test
    fun `목록의 페이지 범위나 상태 값이 잘못되면 400이다`() {
        listOf(
            get(PATH).param("page", "0"),
            get(PATH).param("size", "101"),
            get(PATH).param("status", "UNKNOWN"),
        ).forEach { request ->
            mockMvc.perform(request.with(authenticatedUser()))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.verifyNoInteractions(myAdvertisementContractService)
    }

    @Test
    fun `내 광고 신청 상세는 폼 입력값과 상태와 붙인 상품을 준다`() {
        // given
        Mockito.`when`(myAdvertisementContractService.getContract(USER_ID, CONTRACT_ID)).thenReturn(DETAIL)

        // when
        mockMvc.perform(get("$PATH/{contractId}", CONTRACT_ID).with(authenticatedUser()))
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(CONTRACT_ID))
            .andExpect(jsonPath("$.data.status").value("NEGOTIATING"))
            .andExpect(jsonPath("$.data.recruitmentUrl").value("https://example.com/recruit/backend"))
            .andExpect(jsonPath("$.data.purpose").value("FIT_APPLICANTS"))
            .andExpect(jsonPath("$.data.budgetAmount").value(1_500_000))
            .andExpect(jsonPath("$.data.categories[1]").value("INSTAGRAM_1"))
            .andExpect(jsonPath("$.data.scheduleRequest").value("12월 첫째 주 진행 희망"))
            .andExpect(jsonPath("$.data.advertisementRequest").isEmpty)
            .andExpect(jsonPath("$.data.managerName").value("김담당"))
            .andExpect(jsonPath("$.data.email").value("marketing@example.com"))
            .andExpect(jsonPath("$.data.phoneNumber").value("02-1234-5678"))
            .andExpect(jsonPath("$.data.businessNumberType").value("BUSINESS_REGISTRATION"))
            .andExpect(jsonPath("$.data.businessNumber").value("1234567890"))
            .andExpect(jsonPath("$.data.invoiceMethod").value("TAX_INVOICE"))
            .andExpect(jsonPath("$.data.creativeMethod").value("REQUEST_PRODUCTION"))
            .andExpect(jsonPath("$.data.paymentAmount").value(300000))
            .andExpect(jsonPath("$.data.registeredAt").value("2026-11-08T10:00:00"))
            .andExpect(jsonPath("$.data.products[0].contractProductId").value(31))
            .andExpect(jsonPath("$.data.products[0].productName").value("채용공고 홈 상단 배너"))
            .andExpect(jsonPath("$.data.products[0].category").value("BANNER"))
            .andExpect(jsonPath("$.data.products[0].position").value("JOB_HOME_TOP_BANNER"))
            .andExpect(jsonPath("$.data.products[0].productDeleted").value(false))
            .andExpect(jsonPath("$.data.userId").doesNotExist())
            .andExpect(jsonPath("$.data.source").doesNotExist())
            .andExpect(jsonPath("$.data.targetId").doesNotExist())
    }

    @Test
    fun `내 신청이 아니거나 없는 신청은 광고 계약 404다`() {
        // given
        Mockito.`when`(myAdvertisementContractService.getContract(USER_ID, CONTRACT_ID))
            .thenThrow(EntityNotFoundException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_NOT_FOUND))

        // when
        mockMvc.perform(get("$PATH/{contractId}", CONTRACT_ID).with(authenticatedUser()))
            // then
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ADVERTISEMENT_CONTRACT_NOT_FOUND"))
    }

    @Test
    fun `상세의 계약 식별자가 양수가 아니면 400이다`() {
        mockMvc.perform(get("$PATH/0").with(authenticatedUser()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(myAdvertisementContractService)
    }

    @Test
    fun `로그인하지 않으면 목록과 상세 모두 401이다`() {
        listOf(get(PATH), get("$PATH/{contractId}", CONTRACT_ID)).forEach { request ->
            mockMvc.perform(request)
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        }

        Mockito.verifyNoInteractions(myAdvertisementContractService)
    }

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    private companion object {
        const val PATH = "/api/v1/users/me/advertisement-contracts"
        const val USER_ID = 42L
        const val CONTRACT_ID = 1001L
        val REGISTERED_AT: LocalDateTime = LocalDateTime.of(2026, 11, 8, 10, 0)
        val SUMMARY = MyAdvertisementContractSummaryResult(
            id = CONTRACT_ID,
            status = AdvertisementContractStatus.REQUESTED,
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            companyName = "샘플기업",
            recruitmentTitle = "백엔드 인턴 채용",
            recruitmentDeadline = LocalDate.of(2026, 12, 31),
            categories = listOf(AdvertisementProductCategory.BANNER),
            paymentAmount = null,
            registeredAt = REGISTERED_AT,
        )
        val DETAIL = MyAdvertisementContractDetailResult(
            id = CONTRACT_ID,
            status = AdvertisementContractStatus.NEGOTIATING,
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            companyName = "샘플기업",
            recruitmentTitle = "백엔드 인턴 채용",
            recruitmentUrl = "https://example.com/recruit/backend",
            recruitmentDeadline = LocalDate.of(2026, 12, 31),
            purpose = AdvertisementContractPurpose.FIT_APPLICANTS,
            budgetAmount = 1_500_000,
            categories = listOf(AdvertisementProductCategory.BANNER, AdvertisementProductCategory.INSTAGRAM_1),
            scheduleRequest = "12월 첫째 주 진행 희망",
            advertisementRequest = null,
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "02-1234-5678",
            businessNumberType = CompanyBusinessNumberType.BUSINESS_REGISTRATION,
            businessNumber = "1234567890",
            invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
            creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
            paymentAmount = 300000,
            registeredAt = REGISTERED_AT,
            products = listOf(
                MyAdvertisementContractProductResult(
                    contractProductId = 31L,
                    productName = "채용공고 홈 상단 배너",
                    category = AdvertisementProductCategory.BANNER,
                    position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
                    productDeleted = false,
                ),
            ),
        )
    }
}
