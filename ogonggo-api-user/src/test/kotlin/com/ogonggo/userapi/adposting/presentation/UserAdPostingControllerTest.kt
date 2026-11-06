package com.ogonggo.userapi.adposting.presentation

import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.core.adposting.domain.AdPostingTargetType
import com.ogonggo.core.job.domain.JobEducationLevel
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.userapi.adposting.business.UserAdPostingResult
import com.ogonggo.userapi.adposting.business.UserAdPostingService
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.job.business.UserJobSummary
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
import java.time.LocalDateTime

@WebMvcTest(controllers = [UserAdPostingController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserAdPostingControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userAdPostingService: UserAdPostingService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `로그인 없이 자리의 광고 공고를 목록 항목 모양의 대상 요약과 함께 준다`() {
        // given
        Mockito.`when`(userAdPostingService.getAdPostings(AdPostingPlacement.JOB_LIST)).thenReturn(
            listOf(
                UserAdPostingResult(
                    id = 1,
                    placement = AdPostingPlacement.JOB_LIST,
                    targetType = AdPostingTargetType.JOB,
                    job = jobSummary(),
                ),
            ),
        )

        // when & then
        mockMvc.perform(get(PATH).param("placement", "JOB_LIST"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].placement").value("JOB_LIST"))
            .andExpect(jsonPath("$.data[0].targetType").value("JOB"))
            .andExpect(jsonPath("$.data[0].job.id").value(13))
            .andExpect(jsonPath("$.data[0].job.bookmarked").value(false))
            .andExpect(jsonPath("$.data[0].bootcamp").doesNotExist())
            .andExpect(jsonPath("$.data[0].advertiserId").doesNotExist())
            .andExpect(jsonPath("$.data[0].displayOrder").doesNotExist())
    }

    @Test
    fun `자리가 없거나 모르는 값이면 400이다`() {
        listOf(get(PATH), get(PATH).param("placement", "HOME")).forEach { request ->
            mockMvc.perform(request)
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }
        Mockito.verifyNoInteractions(userAdPostingService)
    }

    private fun jobSummary() = UserJobSummary(
        id = 13,
        companyName = "삼성전자 DS부문",
        title = "R&D분야 외국인 경력사원 채용(DS부문)",
        coverImageUrl = null,
        logoUrl = null,
        employmentType = JobEmploymentType.FULL_TIME,
        experienceType = JobExperienceType.EXPERIENCED,
        jobField = null,
        jobRole = null,
        experienceMinYears = 3,
        educationLevel = JobEducationLevel.ANY,
        region = null,
        subRegion = null,
        recruitmentType = JobRecruitmentType.ALWAYS_OPEN,
        recruitmentStartAt = null,
        recruitmentEndAt = null,
        closedAt = null,
        registeredAt = LocalDateTime.of(2026, 11, 1, 9, 0),
        bookmarked = false,
        viewCount = 10,
        bookmarkCount = 2,
        commentCount = 0,
    )

    private companion object {
        const val PATH = "/api/v1/ad-postings"
    }
}
