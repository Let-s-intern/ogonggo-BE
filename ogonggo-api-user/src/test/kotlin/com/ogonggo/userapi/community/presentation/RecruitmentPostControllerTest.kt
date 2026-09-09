package com.ogonggo.userapi.community.presentation

import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.implement.PostAppendCommand
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.community.business.RecruitmentPostService
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(controllers = [RecruitmentPostController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class RecruitmentPostControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var recruitmentPostService: RecruitmentPostService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `인증된 사용자가 모집글을 생성하면 201과 식별자를 반환한다`() {
        Mockito.`when`(recruitmentPostService.create(USER_ID, createCommand())).thenReturn(12L)

        mockMvc.perform(
            post("/api/v1/recruitment-posts")
                .with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.id").value(12))
    }

    @Test
    fun `인증되지 않은 사용자는 모집글을 생성할 수 없다`() {
        mockMvc.perform(
            post("/api/v1/recruitment-posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `필수 제목이 없으면 400을 반환하고 서비스를 호출하지 않는다`() {
        mockMvc.perform(
            post("/api/v1/recruitment-posts")
                .with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY.replace("사이드 프로젝트 팀원 모집", "")),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(recruitmentPostService)
    }

    @Test
    fun `운영 정책에 동의하지 않으면 400을 반환한다`() {
        mockMvc.perform(
            post("/api/v1/recruitment-posts")
                .with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY.replace("\"agreedToPolicy\": true", "\"agreedToPolicy\": false")),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(recruitmentPostService)
    }

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    private fun createCommand() = PostAppendCommand(
        authorUserId = USER_ID,
        title = "사이드 프로젝트 팀원 모집",
        recruitmentType = RecruitmentType.SIDE_PROJECT,
        capacity = 4,
        progressMethod = ProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin", "Spring"),
        summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
        content = "<p>모집 상세 내용입니다.</p>",
        eligibilityAndSelectionProcess = "주 1회 회의에 참여할 수 있는 분",
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        positions = listOf(RecruitmentPosition.BACKEND),
        contactMethod = ContactMethod.EMAIL,
        contactValue = "team@example.com",
    )

    companion object {
        private const val USER_ID = 17L
        private const val VALID_BODY = """
            {
              "title": "사이드 프로젝트 팀원 모집",
              "recruitmentType": "SIDE_PROJECT",
              "capacity": 4,
              "progressMethod": "ONLINE",
              "activityDurationMonths": 3,
              "technologyStacks": ["Kotlin", "Spring"],
              "summary": "함께 서비스를 만들어 볼 팀원을 모집합니다.",
              "content": "<p>모집 상세 내용입니다.</p>",
              "eligibilityAndSelectionProcess": "주 1회 회의에 참여할 수 있는 분",
              "recruitmentStartDate": "2026-09-01",
              "recruitmentEndDate": "2026-09-30",
              "positions": ["BACKEND"],
              "contactMethod": "EMAIL",
              "contactValue": "team@example.com",
              "agreedToPolicy": true
            }
        """
    }
}
