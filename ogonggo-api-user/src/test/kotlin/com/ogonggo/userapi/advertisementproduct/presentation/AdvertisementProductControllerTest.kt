package com.ogonggo.userapi.advertisementproduct.presentation

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
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
        Mockito.`when`(advertisementProductService.getProducts()).thenReturn(
            listOf(
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
        )

        mockMvc.perform(get(PATH))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data[0].id").value(PRODUCT_ID))
            .andExpect(jsonPath("$.data[0].category").value("BANNER"))
            .andExpect(jsonPath("$.data[0].position").value("JOB_HOME_TOP_BANNER"))
            .andExpect(jsonPath("$.data[0].name").value("채용공고 홈 상단 배너"))
            .andExpect(jsonPath("$.data[0].price").value(500_000))
            .andExpect(jsonPath("$.data[1].category").value("INSTAGRAM_1"))
            .andExpect(jsonPath("$.data[1].position").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data[1].price").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data[0].registeredAt").doesNotExist())

        Mockito.verify(advertisementProductService).getProducts()
    }

    @Test
    fun `상품이 없으면 빈 배열을 반환한다`() {
        Mockito.`when`(advertisementProductService.getProducts()).thenReturn(emptyList())

        mockMvc.perform(get(PATH))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isEmpty)
    }

    private companion object {
        const val PATH = "/api/v1/advertisement-products"
        const val PRODUCT_ID = 11L
    }
}
