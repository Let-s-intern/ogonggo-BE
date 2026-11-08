package com.ogonggo.userapi.advertisementproduct.presentation

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.userapi.advertisementproduct.business.AdvertisementProductPageResult
import com.ogonggo.userapi.advertisementproduct.business.AdvertisementProductResult
import com.ogonggo.userapi.advertisementproduct.business.AdvertisementProductService
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AdvertisementProductController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class AdvertisementProductControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var advertisementProductService: AdvertisementProductService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `비회원이 삭제되지 않은 상품 목록을 조회하고 신청 화면 필드만 받는다`() {
        Mockito.`when`(advertisementProductService.getProducts(0, 10)).thenReturn(
            AdvertisementProductPageResult(
                items = listOf(
                AdvertisementProductResult(
                    id = PRODUCT_ID,
                    category = AdvertisementProductCategory.BANNER,
                    position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
                    name = "채용공고 홈 상단 배너",
                    price = 500_000,
                ),
                AdvertisementProductResult(
                    id = 12L,
                    category = AdvertisementProductCategory.INSTAGRAM_1,
                    position = null,
                    name = "인스타그램 1회",
                    price = null,
                ),
                ),
                page = 0,
                size = 10,
                totalElements = 2,
                totalPages = 1,
            ),
        )

        mockMvc.perform(get(PATH))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.items[0].id").value(PRODUCT_ID))
            .andExpect(jsonPath("$.data.items[0].category").value("BANNER"))
            .andExpect(jsonPath("$.data.items[0].position").value("JOB_HOME_TOP_BANNER"))
            .andExpect(jsonPath("$.data.items[0].name").value("채용공고 홈 상단 배너"))
            .andExpect(jsonPath("$.data.items[0].price").value(500_000))
            .andExpect(jsonPath("$.data.items[1].category").value("INSTAGRAM_1"))
            .andExpect(jsonPath("$.data.items[1].position").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data.items[1].price").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(10))

        Mockito.verify(advertisementProductService).getProducts(0, 10)
    }

    @Test
    fun `상품이 없으면 빈 배열을 반환한다`() {
        Mockito.`when`(advertisementProductService.getProducts(0, 10)).thenReturn(
            AdvertisementProductPageResult(emptyList(), page = 0, size = 10, totalElements = 0, totalPages = 0),
        )

        mockMvc.perform(get(PATH))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isArray)
            .andExpect(jsonPath("$.data.items").isEmpty)
    }

    private companion object {
        const val PATH = "/api/v1/advertisement-products"
        const val PRODUCT_ID = 11L
    }
}
