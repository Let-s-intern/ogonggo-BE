package com.ogonggo.userapi.recruitmentpost.presentation

import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostApplicationService
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostBookmarkService
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostManagementService
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 프런트가 새 경로로 옮기기 전까지 예전 경로도 같은 동작으로 받는지 확인한다. 서버 2차 배포에서 컨트롤러와 함께 지운다. */
@WebMvcTest(
    controllers = [
        RecruitmentPostLegacyPathController::class,
        RecruitmentPostManagementController::class,
        RecruitmentPostApplicationController::class,
        RecruitmentPostBookmarkController::class,
    ],
)
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class RecruitmentPostLegacyPathControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var managementService: RecruitmentPostManagementService

    @MockBean
    private lateinit var recruitmentPostService: RecruitmentPostService

    @MockBean
    private lateinit var applicationService: RecruitmentPostApplicationService

    @MockBean
    private lateinit var bookmarkService: RecruitmentPostBookmarkService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `예전 북마크 경로로 등록하고 해제해도 새 경로와 같게 처리한다`() {
        mockMvc.perform(put("/api/v1/recruitment-posts/{postId}/bookmarks/me", POST_ID).with(authenticatedUser()))
            .andExpect(status().isCreated)
        mockMvc.perform(delete("/api/v1/recruitment-posts/{postId}/bookmarks/me", POST_ID).with(authenticatedUser()))
            .andExpect(status().isOk)

        Mockito.verify(bookmarkService).addBookmark(USER_ID, POST_ID)
        Mockito.verify(bookmarkService).deleteBookmark(USER_ID, POST_ID)
    }

    @Test
    fun `예전 지원 이력 경로로 삭제해도 새 경로와 같게 처리한다`() {
        mockMvc.perform(delete("/api/v1/me/recruitment-applications/{postId}", POST_ID).with(authenticatedUser()))
            .andExpect(status().isOk)

        Mockito.verify(applicationService).deleteApplication(USER_ID, POST_ID)
    }

    @Test
    fun `예전 경로도 로그인을 요구한다`() {
        mockMvc.perform(delete("/api/v1/me/recruitment-applications/{postId}", POST_ID))
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(applicationService)
    }

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    private companion object {
        const val USER_ID = 17L
        const val POST_ID = 12L
    }
}
