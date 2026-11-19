package com.ogonggo.userapi.advertisementexposure.presentation

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.userapi.advertisementexposure.business.AdvertisementExposureResult
import com.ogonggo.userapi.advertisementexposure.business.AdvertisementPostingResult
import com.ogonggo.userapi.bootcamp.business.UserBootcampSummary
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
import java.time.LocalDate
import java.time.LocalDateTime

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
    fun `광고 공고 자리는 배너 조회에서 BAD_REQUEST를 반환한다`() {
        mockMvc.perform(get(PATH).queryParam("position", AdvertisementProductPosition.JOB_LIST.name))
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

    @Test
    fun `비회원이 광고 공고 자리의 카드를 노출 ID·순서·대상 요약과 함께 조회한다`() {
        // given
        Mockito.`when`(advertisementExposureService.getShownPostings(null, AdvertisementProductPosition.BOOTCAMP_LIST)).thenReturn(
            listOf(
                AdvertisementPostingResult(
                    exposureId = EXPOSURE_ID,
                    position = AdvertisementProductPosition.BOOTCAMP_LIST,
                    jobField = null,
                    recommendationTitle = null,
                    recommendationDescription = null,
                    startAt = LocalDateTime.parse("2026-11-20T00:00:00"),
                    endAt = LocalDateTime.parse("2026-11-30T23:59:59"),
                    displayOrder = 1,
                    adLabel = true,
                    targetType = AdvertisementExposureTargetType.BOOTCAMP,
                    job = null,
                    bootcamp = BOOTCAMP_SUMMARY,
                ),
            ),
        )

        // when, then
        mockMvc.perform(get(POSTINGS_PATH).queryParam("position", "BOOTCAMP_LIST"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].exposureId").value(EXPOSURE_ID))
            .andExpect(jsonPath("$.data[0].position").value("BOOTCAMP_LIST"))
            .andExpect(jsonPath("$.data[0].jobField").value(null as Any?))
            .andExpect(jsonPath("$.data[0].recommendationTitle").value(null as Any?))
            .andExpect(jsonPath("$.data[0].recommendationDescription").value(null as Any?))
            .andExpect(jsonPath("$.data[0].startAt").value("2026-11-20T00:00:00"))
            .andExpect(jsonPath("$.data[0].endAt").value("2026-11-30T23:59:59"))
            .andExpect(jsonPath("$.data[0].displayOrder").value(1))
            .andExpect(jsonPath("$.data[0].adLabel").value(true))
            .andExpect(jsonPath("$.data[0].targetType").value("BOOTCAMP"))
            .andExpect(jsonPath("$.data[0].job").value(null as Any?))
            .andExpect(jsonPath("$.data[0].bootcamp.id").value(BOOTCAMP_SUMMARY.id))
            .andExpect(jsonPath("$.data[0].bootcamp.title").value(BOOTCAMP_SUMMARY.title))
            .andExpect(jsonPath("$.data[0].bootcamp.bookmarked").value(false))
            .andExpect(jsonPath("$.data[0].contractId").doesNotExist())
            .andExpect(jsonPath("$.data[0].productId").doesNotExist())
    }

    @Test
    fun `채용공고 목록 자리는 직군 탭의 광고 공고를 직군과 함께 조회한다`() {
        // given
        Mockito.`when`(
            advertisementExposureService.getShownPostings(null, AdvertisementProductPosition.JOB_LIST, JobField.IT_DEVELOPMENT),
        ).thenReturn(
            listOf(
                AdvertisementPostingResult(
                    exposureId = EXPOSURE_ID,
                    position = AdvertisementProductPosition.JOB_LIST,
                    jobField = JobField.IT_DEVELOPMENT,
                    recommendationTitle = null,
                    recommendationDescription = null,
                    startAt = null,
                    endAt = null,
                    displayOrder = 1,
                    adLabel = true,
                    targetType = AdvertisementExposureTargetType.BOOTCAMP,
                    job = null,
                    bootcamp = BOOTCAMP_SUMMARY,
                ),
            ),
        )

        // when, then
        mockMvc.perform(get(POSTINGS_PATH).queryParam("position", "JOB_LIST").queryParam("jobField", "IT_DEVELOPMENT"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].position").value("JOB_LIST"))
            .andExpect(jsonPath("$.data[0].jobField").value("IT_DEVELOPMENT"))
    }

    @Test
    fun `오공고 PICK 자리는 광고 공고를 추천 문구와 함께 조회한다`() {
        // given
        Mockito.`when`(advertisementExposureService.getShownPostings(null, AdvertisementProductPosition.JOB_PICK)).thenReturn(
            listOf(
                AdvertisementPostingResult(
                    exposureId = EXPOSURE_ID,
                    position = AdvertisementProductPosition.JOB_PICK,
                    jobField = null,
                    recommendationTitle = "이번 주 추천",
                    recommendationDescription = "신입도 지원할 수 있는 공고예요.",
                    startAt = null,
                    endAt = null,
                    displayOrder = 1,
                    adLabel = true,
                    targetType = AdvertisementExposureTargetType.BOOTCAMP,
                    job = null,
                    bootcamp = BOOTCAMP_SUMMARY,
                ),
            ),
        )

        // when, then
        mockMvc.perform(get(POSTINGS_PATH).queryParam("position", "JOB_PICK"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].position").value("JOB_PICK"))
            .andExpect(jsonPath("$.data[0].recommendationTitle").value("이번 주 추천"))
            .andExpect(jsonPath("$.data[0].startAt").value(null as Any?))
            .andExpect(jsonPath("$.data[0].endAt").value(null as Any?))
            .andExpect(jsonPath("$.data[0].recommendationDescription").value("신입도 지원할 수 있는 공고예요."))
    }

    @Test
    fun `채용공고 목록 자리에 직군이 없거나 다른 자리에 직군을 보내면 BAD_REQUEST를 반환한다`() {
        mockMvc.perform(get(POSTINGS_PATH).queryParam("position", "JOB_LIST"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(get(POSTINGS_PATH).queryParam("position", "CALENDAR_DAY").queryParam("jobField", "IT_DEVELOPMENT"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(get(POSTINGS_PATH).queryParam("position", "JOB_LIST").queryParam("jobField", "NOT_A_FIELD"))
            .andExpect(status().isBadRequest)

        Mockito.verifyNoInteractions(advertisementExposureService)
    }

    @Test
    fun `배너 자리는 광고 공고 조회에서 BAD_REQUEST를 반환한다`() {
        mockMvc.perform(get(POSTINGS_PATH).queryParam("position", POSITION.name))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(advertisementExposureService)
    }

    private companion object {
        const val POSTINGS_PATH = "/api/v1/advertisement-exposures/postings"
        val BOOTCAMP_SUMMARY = UserBootcampSummary(
            id = 77L,
            companyName = "오공고 교육사",
            title = "백엔드 부트캠프",
            programType = "개발",
            operationType = BootcampOperationType.ONLINE,
            recruitmentType = BootcampRecruitmentType.ALWAYS_OPEN,
            recruitmentStartAt = null,
            recruitmentEndAt = null,
            programStartDate = LocalDate.of(2026, 9, 1),
            programEndDate = LocalDate.of(2026, 12, 1),
            capacity = null,
            tuitionType = BootcampTuitionType.FREE,
            tuitionAmount = null,
            representativeImageUrl = null,
            logoUrl = null,
            shortDescription = "백엔드 개발자로 성장하는 12주",
            status = BootcampRecruitmentStatus.RECRUITING,
            closedAt = null,
            bookmarked = false,
            viewCount = 0,
            bookmarkCount = 0,
            commentCount = 0,
        )
        const val PATH = "/api/v1/advertisement-exposures"
        const val EXPOSURE_ID = 501L
        val POSITION = AdvertisementProductPosition.JOB_HOME_TOP_BANNER
    }
}
