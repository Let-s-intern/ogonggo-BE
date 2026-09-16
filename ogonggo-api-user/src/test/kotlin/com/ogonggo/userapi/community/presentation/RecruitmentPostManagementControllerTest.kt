package com.ogonggo.userapi.community.presentation

import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPostApplicationStatus
import com.ogonggo.core.community.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.community.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.community.business.RecruitmentPostManagementItemResult
import com.ogonggo.userapi.community.business.RecruitmentPostManagementPageResult
import com.ogonggo.userapi.community.business.RecruitmentPostManagementService
import com.ogonggo.userapi.community.business.RecruitmentPostFormResult
import com.ogonggo.userapi.community.business.RecruitmentPostService
import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostDraftRequest
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

@WebMvcTest(controllers = [RecruitmentPostManagementController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class RecruitmentPostManagementControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var managementService: RecruitmentPostManagementService

    @MockBean
    private lateinit var recruitmentPostService: RecruitmentPostService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `제목만 입력한 임시저장 모집글을 생성한다`() {
        val request = CreateRecruitmentPostDraftRequest(title = "작성 중인 모집글")
        Mockito.`when`(recruitmentPostService.createDraft(USER_ID, request.toCommand(USER_ID))).thenReturn(15L)

        mockMvc.perform(
            post("/api/v1/me/recruitment-posts/drafts")
                .with(authenticatedUser())
                .contentType("application/json")
                .content("{\"title\":\"작성 중인 모집글\"}"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.id").value(15))

        Mockito.verify(recruitmentPostService).createDraft(USER_ID, request.toCommand(USER_ID))
    }

    @Test
    fun `정책에 동의하지 않으면 임시저장 모집글 게시를 요청할 수 없다`() {
        mockMvc.perform(
            post("/api/v1/me/recruitment-posts/$POST_ID/publish")
                .with(authenticatedUser())
                .contentType("application/json")
                .content("{\"agreedToPolicy\":false}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(managementService)
    }

    @Test
    fun `정책에 동의하면 임시저장 모집글 게시를 요청한다`() {
        mockMvc.perform(
            post("/api/v1/me/recruitment-posts/$POST_ID/publish")
                .with(authenticatedUser())
                .contentType("application/json")
                .content("{\"agreedToPolicy\":true}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))

        Mockito.verify(managementService).publish(USER_ID, POST_ID)
    }

    @Test
    fun `내 모집글 관리 목록을 최근 저장순으로 조회한다`() {
        Mockito.`when`(
            managementService.getPosts(
                USER_ID,
                RecruitmentPostManagementStatus.ALL,
                null,
                null,
                null,
                null,
                0,
                10,
                RecruitmentPostManagementSortType.LATEST_SAVED,
            ),
        ).thenReturn(pageResult())

        mockMvc.perform(get("/api/v1/me/recruitment-posts").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.items[0].postId").value(POST_ID))
            .andExpect(jsonPath("$.data.items[0].status").value("PUBLISHED"))
            .andExpect(jsonPath("$.data.items[0].applicationCount").value(2))
            .andExpect(jsonPath("$.data.items[0].capacity").value(5))
            .andExpect(jsonPath("$.data.items[0].continueWriting").value(false))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
    }

    @Test
    fun `관리 목록 필터를 서비스에 전달한다`() {
        Mockito.`when`(
            managementService.getPosts(
                USER_ID,
                RecruitmentPostManagementStatus.PUBLISHED,
                RecruitmentStatus.RECRUITING,
                RecruitmentPostApplicationStatus.HAS_APPLICATIONS,
                RecruitmentType.SIDE_PROJECT,
                "Kotlin",
                1,
                20,
                RecruitmentPostManagementSortType.LATEST_SAVED,
            ),
        ).thenReturn(pageResult())

        mockMvc.perform(
            get("/api/v1/me/recruitment-posts")
                .param("page", "2")
                .param("size", "20")
                .param("status", "PUBLISHED")
                .param("recruitmentStatus", "RECRUITING")
                .param("applicationStatus", "HAS_APPLICATIONS")
                .param("recruitmentType", "SIDE_PROJECT")
                .param("keyword", " Kotlin ")
                .with(authenticatedUser()),
        ).andExpect(status().isOk)

        Mockito.verify(managementService).getPosts(
            USER_ID,
            RecruitmentPostManagementStatus.PUBLISHED,
            RecruitmentStatus.RECRUITING,
            RecruitmentPostApplicationStatus.HAS_APPLICATIONS,
            RecruitmentType.SIDE_PROJECT,
            "Kotlin",
            1,
            20,
            RecruitmentPostManagementSortType.LATEST_SAVED,
        )
    }

    @Test
    fun `관리 목록 검색어는 trim 후 길이를 검증한다`() {
        mockMvc.perform(
            get("/api/v1/me/recruitment-posts")
                .param("keyword", " 가 ")
                .with(authenticatedUser()),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `내 모집글을 새 임시저장 글로 복사하고 작성 폼 전체를 반환한다`() {
        Mockito.`when`(managementService.copy(USER_ID, POST_ID)).thenReturn(copyResult())

        mockMvc.perform(post("/api/v1/me/recruitment-posts/$POST_ID/copy").with(authenticatedUser()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.postId").value(101))
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.recruitmentStatus").value("RECRUITING"))
            .andExpect(jsonPath("$.data.title").value("복사된 모집글"))
            .andExpect(jsonPath("$.data.content.root.children").isArray)
            .andExpect(jsonPath("$.data.agreedToPolicy").value(false))

        Mockito.verify(managementService).copy(USER_ID, POST_ID)
    }

    @Test
    fun `내 모집글 작성 폼 상세를 전체 필드로 조회한다`() {
        Mockito.`when`(managementService.getPostForm(USER_ID, POST_ID)).thenReturn(formResult())

        mockMvc.perform(get("/api/v1/me/recruitment-posts/$POST_ID").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.postId").value(101))
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.recruitmentStatus").value("RECRUITING"))
            .andExpect(jsonPath("$.data.title").value("작성 중인 모집글"))
            .andExpect(jsonPath("$.data.technologyStacks[0]").value("Kotlin"))
            .andExpect(jsonPath("$.data.content.root.children").isArray)
            .andExpect(jsonPath("$.data.agreedToPolicy").value(false))

        Mockito.verify(managementService).getPostForm(USER_ID, POST_ID)
    }

    @Test
    fun `인증 없이 모집글 복사를 요청하면 인증 오류를 반환한다`() {
        mockMvc.perform(post("/api/v1/me/recruitment-posts/$POST_ID/copy"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        mockMvc.perform(get("/api/v1/me/recruitment-posts/$POST_ID"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `인증이 없거나 페이지가 잘못되면 표준 오류로 응답한다`() {
        mockMvc.perform(get("/api/v1/me/recruitment-posts"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        mockMvc.perform(get("/api/v1/me/recruitment-posts").param("page", "0").with(authenticatedUser()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    private fun pageResult() = RecruitmentPostManagementPageResult(
        items = listOf(
            RecruitmentPostManagementItemResult(
                postId = POST_ID,
                status = RecruitmentPostManagementStatus.PUBLISHED,
                title = "Kotlin 사이드 프로젝트",
                recruitmentType = RecruitmentType.SIDE_PROJECT,
                progressMethod = ProgressMethod.ONLINE,
                activityDurationMonths = 3,
                recruitmentStatus = RecruitmentStatus.RECRUITING,
                recruitmentStartDate = LocalDate.of(2026, 9, 1),
                recruitmentEndDate = LocalDate.of(2026, 9, 30),
                applicationCount = 2,
                capacity = 5,
                viewCount = 148,
                commentCount = 3,
                lastSavedAt = LocalDateTime.of(2026, 9, 16, 10, 0),
                continueWriting = false,
            ),
        ),
        page = 0,
        size = 10,
        totalElements = 1,
        totalPages = 1,
    )

    private fun copyResult() = formResult(
        postId = 101L,
        title = "복사된 모집글",
        status = RecruitmentPostManagementStatus.DRAFT,
    )

    private fun formResult(
        postId: Long = 101L,
        title: String = "작성 중인 모집글",
        status: RecruitmentPostManagementStatus = RecruitmentPostManagementStatus.DRAFT,
    ) = RecruitmentPostFormResult(
        postId = postId,
        status = status,
        recruitmentStatus = RecruitmentStatus.RECRUITING,
        title = title,
        recruitmentType = RecruitmentType.SIDE_PROJECT,
        capacity = 5,
        progressMethod = ProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin"),
        summary = "복사된 요약",
        content = "{\"root\":{\"children\":[]}}",
        eligibilityAndSelectionProcess = null,
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        positions = listOf(RecruitmentPosition.BACKEND),
        contactMethod = ContactMethod.EMAIL,
        contactValue = "team@example.com",
        agreedToPolicy = false,
    )

    companion object {
        private const val USER_ID = 17L
        private const val POST_ID = 12L
    }
}
