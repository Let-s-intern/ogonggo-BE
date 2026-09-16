package com.ogonggo.userapi.community.presentation

import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.implement.PostAppendCommand
import com.ogonggo.core.community.implement.RecruitmentPostListFilter
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.community.business.RecruitmentPostAuthorResult
import com.ogonggo.userapi.community.business.RecruitmentPostContactResult
import com.ogonggo.userapi.community.business.RecruitmentPostDetailResult
import com.ogonggo.userapi.community.business.RecruitmentPostListQuery
import com.ogonggo.userapi.community.business.RecruitmentPostService
import com.ogonggo.userapi.community.business.RecruitmentPostPageResult
import com.ogonggo.userapi.community.business.RecruitmentPostSummary
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
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
    fun `인증되지 않은 사용자도 공개 모집글 상세를 조회한다`() {
        Mockito.`when`(recruitmentPostService.getRecruitmentPost(12L)).thenReturn(detailResult())

        mockMvc.perform(get("/api/v1/recruitment-posts/12"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.id").value(12))
            .andExpect(jsonPath("$.data.author.userId").value(17))
            .andExpect(jsonPath("$.data.contact.method").value("EMAIL"))
            .andExpect(jsonPath("$.data.content").value("<p>상세 내용</p>"))
            .andExpect(jsonPath("$.data.eligibilityAndSelectionProcess").doesNotExist())
    }

    @Test
    fun `모집글 식별자가 1보다 작으면 400을 반환한다`() {
        mockMvc.perform(get("/api/v1/recruitment-posts/0"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(recruitmentPostService)
    }

    @Test
    fun `존재하지 않는 모집글은 404를 반환한다`() {
        Mockito.`when`(recruitmentPostService.getRecruitmentPost(12L))
            .thenThrow(EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND))

        mockMvc.perform(get("/api/v1/recruitment-posts/12"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RECRUITMENT_POST_NOT_FOUND"))
    }

    @Test
    fun `모집글 목록을 페이지와 enum 필터로 조회한다`() {
        val filter = RecruitmentPostListFilter(
            recruitmentTypes = setOf(RecruitmentType.STUDY, RecruitmentType.SIDE_PROJECT),
            progressMethods = setOf(ProgressMethod.ONLINE),
            recruitmentStatuses = setOf(RecruitmentStatus.RECRUITING),
            positions = setOf(RecruitmentPosition.BACKEND),
        )
        Mockito.`when`(
            recruitmentPostService.getRecruitmentPosts(
                RecruitmentPostListQuery(
                    page = 1,
                    size = 2,
                    sortType = RecruitmentPostSortType.DEADLINE,
                    filter = filter,
                ),
            ),
        ).thenReturn(
            RecruitmentPostPageResult(
                items = listOf(
                    RecruitmentPostSummary(
                        id = 12L,
                        title = "스터디 모집",
                        recruitmentType = RecruitmentType.STUDY,
                        progressMethod = ProgressMethod.ONLINE,
                        recruitmentStatus = RecruitmentStatus.RECRUITING,
                        capacity = 6,
                        activityDurationMonths = 3,
                        technologyStacks = listOf("Kotlin"),
                        recruitmentStartDate = LocalDate.of(2026, 9, 1),
                        recruitmentEndDate = LocalDate.of(2026, 9, 30),
                    ),
                ),
                page = 1,
                size = 2,
                totalElements = 3,
                totalPages = 2,
            ),
        )

        mockMvc.perform(
            get("/api/v1/recruitment-posts")
                .param("page", "2")
                .param("size", "2")
                .param("sort", "DEADLINE")
                .param("recruitmentTypes", "STUDY", "SIDE_PROJECT")
                .param("progressMethods", "ONLINE")
                .param("recruitmentStatuses", "RECRUITING")
                .param("positions", "BACKEND"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.items[0].id").value(12))
            .andExpect(jsonPath("$.data.items[0].recruitmentStatus").value("RECRUITING"))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(2))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(3))

        Mockito.verify(recruitmentPostService).getRecruitmentPosts(
            RecruitmentPostListQuery(1, 2, RecruitmentPostSortType.DEADLINE, filter),
        )
    }

    @Test
    fun `인증되지 않은 사용자도 공개 모집글 목록을 조회할 수 있다`() {
        Mockito.`when`(
            recruitmentPostService.getRecruitmentPosts(
                RecruitmentPostListQuery(
                    page = 0,
                    size = 10,
                    sortType = RecruitmentPostSortType.LATEST,
                    filter = RecruitmentPostListFilter(),
                ),
            ),
        ).thenReturn(RecruitmentPostPageResult(emptyList(), 0, 10, 0, 0))

        mockMvc.perform(get("/api/v1/recruitment-posts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isEmpty)
    }

    @Test
    fun `페이지 크기가 범위를 벗어나면 400을 반환한다`() {
        mockMvc.perform(get("/api/v1/recruitment-posts").param("size", "101"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(recruitmentPostService)
    }

    @Test
    fun `페이지 번호가 1보다 작으면 400을 반환한다`() {
        mockMvc.perform(get("/api/v1/recruitment-posts").param("page", "0"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(recruitmentPostService)
    }

    @Test
    fun `지원하지 않는 enum 필터는 400을 반환한다`() {
        mockMvc.perform(
            get("/api/v1/recruitment-posts")
                .param("recruitmentTypes", "UNKNOWN"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(recruitmentPostService)
    }

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

    private fun detailResult() = RecruitmentPostDetailResult(
        id = 12L,
        author = RecruitmentPostAuthorResult(userId = 17L),
        title = "스터디 모집",
        recruitmentType = RecruitmentType.STUDY,
        recruitmentStatus = RecruitmentStatus.RECRUITING,
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        progressMethod = ProgressMethod.ONLINE,
        capacity = 6,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin"),
        positions = listOf(RecruitmentPosition.BACKEND),
        contact = RecruitmentPostContactResult(ContactMethod.EMAIL, "team@example.com"),
        summary = "함께 공부할 분을 모집합니다.",
        content = "<p>상세 내용</p>",
        eligibilityAndSelectionProcess = null,
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
