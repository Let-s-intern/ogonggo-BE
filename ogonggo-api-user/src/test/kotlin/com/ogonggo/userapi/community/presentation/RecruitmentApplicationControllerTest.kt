package com.ogonggo.userapi.community.presentation

import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.community.business.RecruitmentPostApplicationCreateResult
import com.ogonggo.userapi.community.business.RecruitmentPostApplicationItemResult
import com.ogonggo.userapi.community.business.RecruitmentPostApplicationPageResult
import com.ogonggo.userapi.community.business.RecruitmentPostApplicationService
import com.ogonggo.userapi.community.business.RecruitmentPostAuthorResult
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [RecruitmentApplicationController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class RecruitmentApplicationControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var applicationService: RecruitmentPostApplicationService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `지원 이력을 기록하고 연락처 원본을 응답한다`() {
        Mockito.`when`(applicationService.createApplication(USER_ID, POST_ID)).thenReturn(
            RecruitmentPostApplicationCreateResult(
                postId = POST_ID,
                contactMethod = ContactMethod.OPEN_KAKAO,
                contactValue = "https://open.kakao.com/o/example",
                clickedAt = CLICKED_AT,
            ),
        )

        mockMvc.perform(post("/api/v1/recruitment-posts/{postId}/applications", POST_ID).with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.postId").value(POST_ID))
            .andExpect(jsonPath("$.data.contactMethod").value("OPEN_KAKAO"))
            .andExpect(jsonPath("$.data.contactValue").value("https://open.kakao.com/o/example"))
            .andExpect(jsonPath("$.data.clickedAt").value("2026-09-16T09:00:00"))
    }

    @Test
    fun `내 모집글 지원 이력 목록을 페이지로 조회한다`() {
        Mockito.`when`(applicationService.getApplications(USER_ID, null, null, null, 0, 10)).thenReturn(pageResult())

        mockMvc.perform(get("/api/v1/users/me/recruitment/applications").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.items[0].postId").value(POST_ID))
            .andExpect(jsonPath("$.data.items[0].author.userId").value(AUTHOR_ID))
            .andExpect(jsonPath("$.data.items[0].author.nickname").value("홍길동"))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
    }

    @Test
    fun `지원 이력 목록의 검색어와 필터를 서비스에 전달한다`() {
        Mockito.`when`(
            applicationService.getApplications(
                USER_ID,
                RecruitmentStatus.RECRUITING,
                RecruitmentType.SIDE_PROJECT,
                "Kotlin",
                1,
                20,
            ),
        ).thenReturn(pageResult())

        mockMvc.perform(
            get("/api/v1/users/me/recruitment/applications")
                .param("page", "2")
                .param("size", "20")
                .param("recruitmentStatus", "RECRUITING")
                .param("recruitmentType", "SIDE_PROJECT")
                .param("keyword", " Kotlin ")
                .with(authenticatedUser()),
        ).andExpect(status().isOk)

        Mockito.verify(applicationService).getApplications(
            USER_ID,
            RecruitmentStatus.RECRUITING,
            RecruitmentType.SIDE_PROJECT,
            "Kotlin",
            1,
            20,
        )
    }

    @Test
    fun `지원 이력 목록 검색어가 허용 길이를 벗어나면 400으로 응답한다`() {
        listOf("", "가", " 가 ", "가".repeat(101)).forEach { keyword ->
            mockMvc.perform(
                get("/api/v1/users/me/recruitment/applications")
                    .param("keyword", keyword)
                    .with(authenticatedUser()),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }
    }

    @Test
    fun `인증이 없거나 postId가 잘못되면 표준 오류로 응답한다`() {
        mockMvc.perform(post("/api/v1/recruitment-posts/{postId}/applications", POST_ID))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        mockMvc.perform(post("/api/v1/recruitment-posts/0/applications").with(authenticatedUser()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        mockMvc.perform(get("/api/v1/users/me/recruitment/applications"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `마감 모집글과 없는 모집글 오류를 전달한다`() {
        Mockito.doThrow(ConflictException(RecruitmentPostErrorCode.RECRUITMENT_POST_CLOSED))
            .`when`(applicationService).createApplication(USER_ID, POST_ID)
        mockMvc.perform(post("/api/v1/recruitment-posts/{postId}/applications", POST_ID).with(authenticatedUser()))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("RECRUITMENT_POST_CLOSED"))

        Mockito.doThrow(EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND))
            .`when`(applicationService).createApplication(USER_ID, POST_ID)
        mockMvc.perform(post("/api/v1/recruitment-posts/{postId}/applications", POST_ID).with(authenticatedUser()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RECRUITMENT_POST_NOT_FOUND"))
    }

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    private fun pageResult() = RecruitmentPostApplicationPageResult(
        items = listOf(
            RecruitmentPostApplicationItemResult(
                postId = POST_ID,
                title = "Kotlin 팀원 모집",
                recruitmentType = RecruitmentType.SIDE_PROJECT,
                recruitmentStatus = RecruitmentStatus.RECRUITING,
                recruitmentEndDate = LocalDate.of(2026, 9, 30),
                lastClickedAt = CLICKED_AT,
                author = RecruitmentPostAuthorResult(
                    userId = AUTHOR_ID,
                    nickname = "홍길동",
                    profileImageUrl = "https://cdn.example.com/33.png",
                ),
            ),
        ),
        page = 0,
        size = 10,
        totalElements = 1,
        totalPages = 1,
    )

    companion object {
        private const val USER_ID = 17L
        private const val POST_ID = 12L
        private const val AUTHOR_ID = 33L
        private val CLICKED_AT = LocalDateTime.of(2026, 9, 16, 9, 0)
    }
}
