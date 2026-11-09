package com.ogonggo.userapi.advertisementexposure.presentation

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.userapi.advertisementexposure.business.AdvertisementExposureResult
import com.ogonggo.userapi.advertisementexposure.business.AdvertisementExposurePageResult
import com.ogonggo.userapi.advertisementexposure.business.AdvertisementExposureService
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

@WebMvcTest(controllers = [AdvertisementExposureController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class AdvertisementExposureControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var advertisementExposureService: AdvertisementExposureService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `비회원이 배너 자리 노출을 공개 필드만 포함해 조회한다`() {
        // given
        Mockito.`when`(advertisementExposureService.getShown(POSITION, 0, 10)).thenReturn(
            AdvertisementExposurePageResult(
                items = listOf(
                AdvertisementExposureResult(
                    id = EXPOSURE_ID,
                    name = "샘플기업 인턴 채용",
                    imageUrl = "https://example.com/banner.png",
                    mobileImageUrl = "https://example.com/banner-mobile.png",
                    linkUrl = "/jobs/5501",
                    adLabel = true,
                ),
                ),
                page = 0,
                size = 10,
                totalElements = 1,
                totalPages = 1,
            ),
        )

        // when, then
        mockMvc.perform(get(PATH).queryParam("position", POSITION.name))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.items[0].id").value(EXPOSURE_ID))
            .andExpect(jsonPath("$.data.items[0].name").value("샘플기업 인턴 채용"))
            .andExpect(jsonPath("$.data.items[0].imageUrl").value("https://example.com/banner.png"))
            .andExpect(jsonPath("$.data.items[0].mobileImageUrl").value("https://example.com/banner-mobile.png"))
            .andExpect(jsonPath("$.data.items[0].linkUrl").value("/jobs/5501"))
            .andExpect(jsonPath("$.data.items[0].adLabel").value(true))
            .andExpect(jsonPath("$.data.items[0].contractId").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].productId").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].targetType").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].startAt").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].advertiserCompanyName").doesNotExist())
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))

        Mockito.verify(advertisementExposureService).getShown(POSITION, 0, 10)
    }

    @Test
    fun `유효한 자리에 노출이 없으면 빈 배열을 반환한다`() {
        // given
        Mockito.`when`(advertisementExposureService.getShown(POSITION, 0, 10)).thenReturn(
            AdvertisementExposurePageResult(emptyList(), page = 0, size = 10, totalElements = 0, totalPages = 0),
        )

        // when, then
        mockMvc.perform(get(PATH).queryParam("position", POSITION.name))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isArray)
            .andExpect(jsonPath("$.data.items").isEmpty)
    }

    @Test
    fun `position이 없으면 BAD_REQUEST를 반환한다`() {
        mockMvc.perform(get(PATH))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(advertisementExposureService)
    }

    @Test
    fun `현재 배너 자리가 아닌 position은 BAD_REQUEST를 반환한다`() {
        mockMvc.perform(get(PATH).queryParam("position", "JOB_POSTING"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(advertisementExposureService)
    }

    @Test
    fun `페이지 범위가 잘못되면 BAD_REQUEST를 반환한다`() {
        // given / when / then
        mockMvc.perform(get(PATH).queryParam("position", POSITION.name).queryParam("page", "0"))
            .andExpect(status().isBadRequest)
        mockMvc.perform(get(PATH).queryParam("position", POSITION.name).queryParam("size", "101"))
            .andExpect(status().isBadRequest)
        Mockito.verifyNoInteractions(advertisementExposureService)
    }

    private companion object {
        const val PATH = "/api/v1/advertisement-exposures"
        const val EXPOSURE_ID = 501L
        val POSITION = AdvertisementProductPosition.JOB_HOME_TOP_BANNER
    }
}
