package com.ogonggo.userapi.advertisementexposure.presentation

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.userapi.advertisementexposure.business.AdvertisementExposureResult
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
        Mockito.`when`(advertisementExposureService.getShown(POSITION)).thenReturn(
            listOf(
                AdvertisementExposureResult(
                    id = EXPOSURE_ID,
                    name = "샘플기업 인턴 채용",
                    imageUrl = "https://example.com/banner.png",
                    linkUrl = "/jobs/5501",
                    adLabel = true,
                ),
            ),
        )

        // when, then
        mockMvc.perform(get(PATH).queryParam("position", POSITION.name))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data[0].id").value(EXPOSURE_ID))
            .andExpect(jsonPath("$.data[0].name").value("샘플기업 인턴 채용"))
            .andExpect(jsonPath("$.data[0].imageUrl").value("https://example.com/banner.png"))
            .andExpect(jsonPath("$.data[0].linkUrl").value("/jobs/5501"))
            .andExpect(jsonPath("$.data[0].adLabel").value(true))
            .andExpect(jsonPath("$.data[0].contractId").doesNotExist())
            .andExpect(jsonPath("$.data[0].productId").doesNotExist())
            .andExpect(jsonPath("$.data[0].targetType").doesNotExist())
            .andExpect(jsonPath("$.data[0].startAt").doesNotExist())
            .andExpect(jsonPath("$.data[0].advertiserCompanyName").doesNotExist())

        Mockito.verify(advertisementExposureService).getShown(POSITION)
    }

    @Test
    fun `유효한 자리에 노출이 없으면 빈 배열을 반환한다`() {
        // given
        Mockito.`when`(advertisementExposureService.getShown(POSITION)).thenReturn(emptyList())

        // when, then
        mockMvc.perform(get(PATH).queryParam("position", POSITION.name))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isEmpty)
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

    private companion object {
        const val PATH = "/api/v1/advertisement-exposures"
        const val EXPOSURE_ID = 501L
        val POSITION = AdvertisementProductPosition.JOB_HOME_TOP_BANNER
    }
}
