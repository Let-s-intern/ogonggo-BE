package com.ogonggo.adminapi.advertisementcontract.presentation

import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractDetailResult
import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractPageResult
import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractProductResult
import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractService
import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractSummaryResult
import com.ogonggo.adminapi.advertisementcontract.business.AdminBusinessAccountIssueResult
import com.ogonggo.adminapi.advertisementcontract.business.AdminBusinessAccountService
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractFormAnswer
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSearchCondition
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.error.AdvertisementProductErrorCode
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [AdminAdvertisementContractController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminAdvertisementContractControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminAdvertisementContractService: AdminAdvertisementContractService

    @MockBean
    private lateinit var adminBusinessAccountService: AdminBusinessAccountService

    @Test
    fun `관리자 토큰이 없으면 계약 목록과 상세를 조회할 수 없다`() {
        mockMvc.perform(get(PATH))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").value("리소스 접근 권한이 없습니다."))
        mockMvc.perform(get("$PATH/$CONTRACT_ID"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        mockMvc.perform(
            patch("$PATH/$CONTRACT_ID/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"APPROVED"}"""),
        ).andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        mockMvc.perform(
            patch("$PATH/$CONTRACT_ID/payment-amount")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"paymentAmount":0}"""),
        ).andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        mockMvc.perform(
            post("$PATH/$CONTRACT_ID/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":11}"""),
        ).andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        mockMvc.perform(delete("$PATH/$CONTRACT_ID/products/3001"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        Mockito.verifyNoInteractions(adminAdvertisementContractService)
    }

    @Test
    fun `관리자 권한이 없으면 계약 운영 API는 403이다`() {
        mockMvc.perform(
            patch("$PATH/$CONTRACT_ID/status")
                .with(user("member"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"APPROVED"}"""),
        ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("리소스 접근 권한이 없습니다."))

        Mockito.verifyNoInteractions(adminAdvertisementContractService)
    }

    @Test
    fun `기본 페이지는 1부터 응답하고 core에는 0 기반으로 전달한다`() {
        val result = AdminAdvertisementContractPageResult(
            items = listOf(summary()),
            page = 0,
            size = 20,
            totalElements = 1,
            totalPages = 1,
        )
        Mockito.`when`(adminAdvertisementContractService.getContracts(AdvertisementContractSearchCondition(), 0, 20))
            .thenReturn(result)

        mockMvc.perform(admin(get(PATH)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(CONTRACT_ID))
            .andExpect(jsonPath("$.data.items[0].categories[0]").value("BANNER"))
            .andExpect(jsonPath("$.data.items[0].budgetAmount").value(5_000_000))
            .andExpect(jsonPath("$.data.items[0].formAnswers").doesNotExist())
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))

        Mockito.verify(adminAdvertisementContractService).getContracts(AdvertisementContractSearchCondition(), 0, 20)
    }

    @Test
    fun `계약 목록 필터를 core 조건으로 전달하고 반복 status와 날짜 범위를 적용한다`() {
        val condition = AdvertisementContractSearchCondition(
            source = AdvertisementContractSource.APPLICATION_FORM,
            statuses = setOf(AdvertisementContractStatus.REQUESTED, AdvertisementContractStatus.NEGOTIATING),
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            category = AdvertisementProductCategory.BANNER,
            companyName = " 샘플기업 ",
            createdFrom = LocalDate.of(2026, 11, 1),
            createdTo = LocalDate.of(2026, 11, 30),
        )
        Mockito.`when`(adminAdvertisementContractService.getContracts(condition, 1, 50))
            .thenReturn(AdminAdvertisementContractPageResult(emptyList(), 1, 50, 0, 0))

        mockMvc.perform(
            admin(
                get(PATH)
                    .queryParam("source", "APPLICATION_FORM")
                    .queryParam("status", "REQUESTED", "NEGOTIATING")
                    .queryParam("promotionTarget", "JOB")
                    .queryParam("category", "BANNER")
                    .queryParam("companyName", " 샘플기업 ")
                    .queryParam("createdFrom", "2026-11-01")
                    .queryParam("createdTo", "2026-11-30")
                    .queryParam("page", "2")
                    .queryParam("size", "50"),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isEmpty)
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(2))

        Mockito.verify(adminAdvertisementContractService).getContracts(condition, 1, 50)
    }

    @Test
    fun `계약 목록은 페이지 크기와 회사명 최대 길이를 허용한다`() {
        val companyName = "x".repeat(100)
        val condition = AdvertisementContractSearchCondition(companyName = companyName)
        Mockito.`when`(adminAdvertisementContractService.getContracts(condition, 0, 100))
            .thenReturn(AdminAdvertisementContractPageResult(emptyList(), 0, 100, 0, 0))

        mockMvc.perform(admin(get(PATH).queryParam("companyName", companyName).queryParam("size", "100")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isEmpty)
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(100))

        Mockito.verify(adminAdvertisementContractService).getContracts(condition, 0, 100)
    }

    @Test
    fun `상세에는 희망 카테고리와 삭제 상품 표시를 포함한 상품을 반환한다`() {
        val detail = AdminAdvertisementContractDetailResult(
            id = CONTRACT_ID,
            source = AdvertisementContractSource.APPLICATION_FORM,
            status = AdvertisementContractStatus.REQUESTED,
            userId = null,
            targetId = null,
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            companyName = "샘플기업",
            recruitmentTitle = "백엔드 인턴 채용",
            recruitmentUrl = "https://example.com/recruit/backend",
            recruitmentDeadline = LocalDate.of(2026, 12, 31),
            purpose = AdvertisementContractPurpose.FIT_APPLICANTS,
            budgetAmount = 800_000,
            categories = listOf(AdvertisementProductCategory.BANNER, AdvertisementProductCategory.INSTAGRAM_1),
            scheduleRequest = null,
            advertisementRequest = "인턴 지원자를 대상으로 알려주세요.",
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "02-1234-5678",
            businessNumberType = CompanyBusinessNumberType.BUSINESS_REGISTRATION,
            businessNumber = "1234567890",
            invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
            creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
            formAnswers = listOf(
                AdvertisementContractFormAnswer(key = "desiredProduct", label = "희망 상품", value = "메인 배너"),
                AdvertisementContractFormAnswer(key = "recruitmentType", label = "모집 형태", value = "인턴"),
            ),
            paymentAmount = null,
            registeredAt = REGISTERED_AT,
            updatedAt = REGISTERED_AT,
            products = listOf(
                AdminAdvertisementContractProductResult(
                    contractProductId = 3001,
                    productId = 11,
                    productName = "채용공고 홈 상단 배너",
                    category = AdvertisementProductCategory.BANNER,
                    productDeleted = true,
                    addedAt = REGISTERED_AT.plusHours(1),
                ),
            ),
        )
        Mockito.`when`(adminAdvertisementContractService.getContract(CONTRACT_ID)).thenReturn(detail)

        mockMvc.perform(admin(get("$PATH/$CONTRACT_ID")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.source").value("APPLICATION_FORM"))
            .andExpect(jsonPath("$.data.userId").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data.categories[0]").value("BANNER"))
            .andExpect(jsonPath("$.data.budgetAmount").value(800_000))
            .andExpect(jsonPath("$.data.formAnswers.length()").value(2))
            .andExpect(jsonPath("$.data.formAnswers[0].label").value("희망 상품"))
            .andExpect(jsonPath("$.data.formAnswers[1].key").value("recruitmentType"))
            .andExpect(jsonPath("$.data.formAnswers[1].value").value("인턴"))
            .andExpect(jsonPath("$.data.businessNumberType").value("BUSINESS_REGISTRATION"))
            .andExpect(jsonPath("$.data.businessNumber").value("1234567890"))
            .andExpect(jsonPath("$.data.products[0].contractProductId").value(3001))
            .andExpect(jsonPath("$.data.products[0].productDeleted").value(true))

        Mockito.verify(adminAdvertisementContractService).getContract(CONTRACT_ID)
    }

    @Test
    fun `페이지 기간과 ID 검증 실패는 400이고 없는 상세는 404다`() {
        mockMvc.perform(admin(get(PATH).queryParam("page", "0")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(admin(get(PATH).queryParam("size", "101")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(admin(get(PATH).queryParam("companyName", "x".repeat(101))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(
            admin(get(PATH).queryParam("createdFrom", "2026-12-01").queryParam("createdTo", "2026-11-01")),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(admin(get("$PATH/0")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.`when`(adminAdvertisementContractService.getContract(CONTRACT_ID))
            .thenThrow(EntityNotFoundException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_NOT_FOUND))
        mockMvc.perform(admin(get("$PATH/$CONTRACT_ID")))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ADVERTISEMENT_CONTRACT_NOT_FOUND"))

        Mockito.verify(adminAdvertisementContractService).getContract(CONTRACT_ID)
        Mockito.verifyNoMoreInteractions(adminAdvertisementContractService)
    }

    @Test
    fun `신청 폼 계약 상태를 변경하고 응답 데이터는 null이다`() {
        mockMvc.perform(
            admin(
                patch("$PATH/$CONTRACT_ID/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":"APPROVED"}"""),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))

        Mockito.verify(adminAdvertisementContractService).changeStatus(CONTRACT_ID, AdvertisementContractStatus.APPROVED)
    }

    @Test
    fun `상태 변경은 잘못된 입력에 400, 비즈니스 공고 계약에 409를 반환한다`() {
        mockMvc.perform(
            admin(
                patch("$PATH/0/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":"APPROVED"}"""),
            ),
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(
            admin(
                patch("$PATH/$CONTRACT_ID/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ),
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(
            admin(
                patch("$PATH/$CONTRACT_ID/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":"UNKNOWN"}"""),
            ),
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(
            admin(
                patch("$PATH/$CONTRACT_ID/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":2}"""),
            ),
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.doThrow(ConflictException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_FOLLOWS_REVIEW))
            .`when`(adminAdvertisementContractService)
            .changeStatus(CONTRACT_ID, AdvertisementContractStatus.APPROVED)
        mockMvc.perform(
            admin(
                patch("$PATH/$CONTRACT_ID/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":"APPROVED"}"""),
            ),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("ADVERTISEMENT_CONTRACT_FOLLOWS_REVIEW"))

        Mockito.verify(adminAdvertisementContractService).changeStatus(CONTRACT_ID, AdvertisementContractStatus.APPROVED)
    }

    @Test
    fun `결제 금액은 0 또는 null로 저장하고 키 누락과 음수는 거부한다`() {
        val url = "$PATH/$CONTRACT_ID/payment-amount"

        mockMvc.perform(
            admin(patch(url).contentType(MediaType.APPLICATION_JSON).content("""{"paymentAmount":0}""")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
        mockMvc.perform(
            admin(patch(url).contentType(MediaType.APPLICATION_JSON).content("""{"paymentAmount":null}""")),
        ).andExpect(status().isOk)
        mockMvc.perform(
            admin(patch(url).contentType(MediaType.APPLICATION_JSON).content("{}")),
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(
            admin(patch(url).contentType(MediaType.APPLICATION_JSON).content("""{"paymentAmount":-1}""")),
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(
            admin(patch(url).contentType(MediaType.APPLICATION_JSON).content("""{"paymentAmount":1.5}""")),
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(
            admin(patch(url).contentType(MediaType.APPLICATION_JSON).content("""{"paymentAmount":2147483648}""")),
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verify(adminAdvertisementContractService).changePaymentAmount(CONTRACT_ID, 0)
        Mockito.verify(adminAdvertisementContractService).changePaymentAmount(CONTRACT_ID, null)
        Mockito.verifyNoMoreInteractions(adminAdvertisementContractService)
    }

    @Test
    fun `계약에 상품을 연결하면 연결 ID를 201로 반환한다`() {
        Mockito.`when`(adminAdvertisementContractService.addProduct(CONTRACT_ID, 11)).thenReturn(3001)

        mockMvc.perform(
            admin(
                post("$PATH/$CONTRACT_ID/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"productId":11}"""),
            ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.id").value(3001))

        Mockito.verify(adminAdvertisementContractService).addProduct(CONTRACT_ID, 11)
    }

    @Test
    fun `계약 상품 연결은 양수 상품 ID를 요구하고 없는 계약은 404다`() {
        val url = "$PATH/$CONTRACT_ID/products"
        listOf(
            "{}",
            """{"productId":0}""",
            """{"productId":11.9}""",
            """{"productId":"11"}""",
            """{"productId":-1}""",
        ).forEach { body ->
            mockMvc.perform(admin(post(url).contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.`when`(adminAdvertisementContractService.addProduct(CONTRACT_ID, 11))
            .thenThrow(EntityNotFoundException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_NOT_FOUND))
        mockMvc.perform(admin(post(url).contentType(MediaType.APPLICATION_JSON).content("""{"productId":11}""")))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ADVERTISEMENT_CONTRACT_NOT_FOUND"))

        Mockito.doThrow(EntityNotFoundException(AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_NOT_FOUND))
            .`when`(adminAdvertisementContractService)
            .addProduct(CONTRACT_ID, 12)
        mockMvc.perform(admin(post(url).contentType(MediaType.APPLICATION_JSON).content("""{"productId":12}""")))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ADVERTISEMENT_PRODUCT_NOT_FOUND"))

        Mockito.verify(adminAdvertisementContractService).addProduct(CONTRACT_ID, 11)
        Mockito.verify(adminAdvertisementContractService).addProduct(CONTRACT_ID, 12)
    }

    @Test
    fun `계약 상품 제거는 연결 ID를 받아 200으로 응답하고 계약에 속하지 않은 연결은 404다`() {
        mockMvc.perform(admin(delete("$PATH/$CONTRACT_ID/products/3001")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
        mockMvc.perform(admin(delete("$PATH/0/products/3001"))).andExpect(status().isBadRequest)

        Mockito.doThrow(EntityNotFoundException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND))
            .`when`(adminAdvertisementContractService)
            .removeProduct(CONTRACT_ID, 3002)
        mockMvc.perform(admin(delete("$PATH/$CONTRACT_ID/products/3002")))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND"))

        Mockito.verify(adminAdvertisementContractService).removeProduct(CONTRACT_ID, 3001)
        Mockito.verify(adminAdvertisementContractService).removeProduct(CONTRACT_ID, 3002)
    }

    @Test
    fun `비즈니스 계정 발급은 관리자만 하고 발급 결과를 돌려준다`() {
        mockMvc.perform(post("$PATH/$CONTRACT_ID/business-account"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        mockMvc.perform(post("$PATH/$CONTRACT_ID/business-account").with(user("member")))
            .andExpect(status().isForbidden)
        Mockito.verifyNoInteractions(adminBusinessAccountService)

        Mockito.`when`(adminBusinessAccountService.issue(CONTRACT_ID))
            .thenReturn(
                AdminBusinessAccountIssueResult(
                    userId = 501,
                    created = true,
                    loginEmail = "ad@example.com",
                    temporaryPassword = "Ab3dEf7hJk9m",
                    linkedContractCount = 2,
                ),
            )

        mockMvc.perform(admin(post("$PATH/$CONTRACT_ID/business-account")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.userId").value(501))
            .andExpect(jsonPath("$.data.created").value(true))
            .andExpect(jsonPath("$.data.loginEmail").value("ad@example.com"))
            .andExpect(jsonPath("$.data.temporaryPassword").value("Ab3dEf7hJk9m"))
            .andExpect(jsonPath("$.data.linkedContractCount").value(2))
    }

    @Test
    fun `발급할 수 없는 계약이면 409와 오류 코드를 돌려준다`() {
        Mockito.`when`(adminBusinessAccountService.issue(CONTRACT_ID))
            .thenThrow(ConflictException(AdvertisementContractErrorCode.BUSINESS_ACCOUNT_NOT_ISSUABLE))

        mockMvc.perform(admin(post("$PATH/$CONTRACT_ID/business-account")))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("BUSINESS_ACCOUNT_NOT_ISSUABLE"))
    }

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private fun summary() = AdminAdvertisementContractSummaryResult(
        id = CONTRACT_ID,
        source = AdvertisementContractSource.APPLICATION_FORM,
        status = AdvertisementContractStatus.REQUESTED,
        promotionTarget = AdvertisementContractPromotionTarget.JOB,
        targetId = null,
        companyName = "샘플기업",
        recruitmentTitle = "백엔드 인턴 채용",
        recruitmentDeadline = LocalDate.of(2026, 12, 31),
        budgetAmount = 5_000_000,
        categories = listOf(AdvertisementProductCategory.BANNER),
        paymentAmount = null,
        registeredAt = REGISTERED_AT,
        updatedAt = REGISTERED_AT,
    )

    private companion object {
        const val PATH = "/api/v1/admin/advertisement-contracts"
        const val CONTRACT_ID = 1001L
        val REGISTERED_AT = LocalDateTime.parse("2026-11-07T17:30:00")
    }
}
