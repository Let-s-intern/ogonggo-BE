package com.ogonggo.adminapi.advertisementproduct.presentation

import com.ogonggo.adminapi.advertisementproduct.business.AdminAdvertisementProductSaveCommand
import com.ogonggo.adminapi.advertisementproduct.business.AdminAdvertisementProductResult
import com.ogonggo.adminapi.advertisementproduct.business.AdminAdvertisementProductService
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.error.AdvertisementProductErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.error.InvalidValueException
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [AdminAdvertisementProductController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminAdvertisementProductControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminAdvertisementProductService: AdminAdvertisementProductService

    @Test
    fun `관리자 토큰이 없으면 상품을 조회하거나 변경할 수 없다`() {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized)
        mockMvc.perform(createRequest(VALID_BODY)).andExpect(status().isUnauthorized)
        mockMvc.perform(replaceRequest(PRODUCT_ID, VALID_BODY)).andExpect(status().isUnauthorized)
        mockMvc.perform(delete("$PATH/$PRODUCT_ID")).andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminAdvertisementProductService)
    }

    @Test
    fun `관리자는 상품 목록을 관리자 응답 필드와 함께 조회한다`() {
        val registeredAt = LocalDateTime.parse("2026-11-07T17:00:00")
        Mockito.`when`(adminAdvertisementProductService.getProducts()).thenReturn(
            listOf(
                AdminAdvertisementProductResult(
                    id = PRODUCT_ID,
                    category = AdvertisementProductCategory.BANNER,
                    position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
                    name = "채용공고 홈 상단 배너",
                    price = 500_000,
                    registeredAt = registeredAt,
                    updatedAt = registeredAt,
                ),
            ),
        )

        mockMvc.perform(admin(get(PATH)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(PRODUCT_ID))
            .andExpect(jsonPath("$.data[0].category").value("BANNER"))
            .andExpect(jsonPath("$.data[0].position").value("JOB_HOME_TOP_BANNER"))
            .andExpect(jsonPath("$.data[0].price").value(500_000))
            .andExpect(jsonPath("$.data[0].registeredAt").value("2026-11-07T17:00:00"))
            .andExpect(jsonPath("$.data[0].updatedAt").value("2026-11-07T17:00:00"))
    }

    @Test
    fun `상품을 등록하면 201과 새 상품 ID를 반환한다`() {
        val command = validCommand()
        Mockito.`when`(adminAdvertisementProductService.createProduct(command)).thenReturn(PRODUCT_ID)

        mockMvc.perform(admin(createRequest(VALID_BODY)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value(PRODUCT_ID))

        Mockito.verify(adminAdvertisementProductService).createProduct(command)
    }

    @Test
    fun `상품 전체 수정에서 nullable 필드를 누락하면 값을 해제한다`() {
        val command = AdminAdvertisementProductSaveCommand(
            category = AdvertisementProductCategory.BANNER,
            position = null,
            name = "채용공고 홈 상단 배너",
            price = null,
        )

        mockMvc.perform(admin(replaceRequest(PRODUCT_ID, """{"category":"BANNER","name":"채용공고 홈 상단 배너"}""")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))

        Mockito.verify(adminAdvertisementProductService).replaceProduct(PRODUCT_ID, command)
    }

    @Test
    fun `상품 카테고리 변경과 자리 해제 요청을 전달한다`() {
        // given
        val command = AdminAdvertisementProductSaveCommand(
            category = AdvertisementProductCategory.JOB_POSTING,
            position = null,
            name = "채용공고 광고 상품",
            price = 500_000,
        )
        val body = """{"category":"JOB_POSTING","position":null,"name":"채용공고 광고 상품","price":500000}"""

        // when
        mockMvc.perform(admin(replaceRequest(PRODUCT_ID, body)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))

        // then
        Mockito.verify(adminAdvertisementProductService).replaceProduct(PRODUCT_ID, command)
    }

    @Test
    fun `상품 삭제는 200과 null data를 반환한다`() {
        mockMvc.perform(admin(delete("$PATH/$PRODUCT_ID")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))

        Mockito.verify(adminAdvertisementProductService).deleteProduct(PRODUCT_ID)
    }

    @Test
    fun `상품 요청 형식이 잘못되면 서비스를 호출하지 않고 400으로 응답한다`() {
        listOf(
            """{"category":"BANNER","name":" "}""",
            """{"category":"BANNER","name":"${"a".repeat(101)}"}""",
            """{"category":"BANNER","name":"배너","price":-1}""",
            """{"category":"NO_SUCH_CATEGORY","name":"배너"}""",
        ).forEach { body ->
            mockMvc.perform(admin(createRequest(body)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }
        mockMvc.perform(admin(replaceRequest(0L, VALID_BODY)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(adminAdvertisementProductService)
    }

    @Test
    fun `category에 속하지 않는 position은 core 오류 코드로 400에 매핑한다`() {
        val command = AdminAdvertisementProductSaveCommand(
            category = AdvertisementProductCategory.JOB_POSTING,
            position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
            name = "배너",
            price = null,
        )
        Mockito.doThrow(InvalidValueException(AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_POSITION_MISMATCH))
            .`when`(adminAdvertisementProductService).createProduct(command)

        mockMvc.perform(
            admin(createRequest("""{"category":"JOB_POSTING","position":"JOB_HOME_TOP_BANNER","name":"배너"}""")),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("ADVERTISEMENT_PRODUCT_POSITION_MISMATCH"))
    }

    @Test
    fun `없는 상품 또는 삭제된 상품은 core 오류 코드로 404에 매핑한다`() {
        Mockito.doThrow(EntityNotFoundException(AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_NOT_FOUND))
            .`when`(adminAdvertisementProductService).deleteProduct(PRODUCT_ID)

        mockMvc.perform(admin(delete("$PATH/$PRODUCT_ID")))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ADVERTISEMENT_PRODUCT_NOT_FOUND"))
    }

    private fun createRequest(body: String): MockHttpServletRequestBuilder =
        post(PATH).contentType(MediaType.APPLICATION_JSON).content(body)

    private fun replaceRequest(productId: Long, body: String): MockHttpServletRequestBuilder =
        put("$PATH/$productId").contentType(MediaType.APPLICATION_JSON).content(body)

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private fun validCommand() = AdminAdvertisementProductSaveCommand(
        category = AdvertisementProductCategory.BANNER,
        position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
        name = "채용공고 홈 상단 배너",
        price = 500_000,
    )

    private companion object {
        const val PATH = "/api/v1/admin/advertisement-products"
        const val PRODUCT_ID = 11L
        const val VALID_BODY = """{"category":"BANNER","position":"JOB_HOME_TOP_BANNER","name":"채용공고 홈 상단 배너","price":500000}"""
    }
}
