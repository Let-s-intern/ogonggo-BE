package com.ogonggo.userapi.notice.presentation

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.notice.error.NoticeErrorCode
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.notice.business.UserNoticePageResult
import com.ogonggo.userapi.notice.business.UserNoticeService
import com.ogonggo.userapi.notice.business.UserNoticeSummary
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [UserNoticeController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserNoticeControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userNoticeService: UserNoticeService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `로그인 없이 공지 목록을 1 기반 페이지로 조회한다`() {
        val summary = UserNoticeSummary(id = 7L, title = "서비스 점검 안내", pinned = true, createdAt = NOW)
        Mockito.`when`(userNoticeService.getNotices(0, 10))
            .thenReturn(UserNoticePageResult(listOf(summary), page = 0, size = 10, totalElements = 1, totalPages = 1))

        mockMvc.perform(get("/api/v1/notices"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(7))
            .andExpect(jsonPath("$.data.items[0].pinned").value(true))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
    }

    @Test
    fun `노출 중이 아닌 공지 상세는 404 NOTICE_NOT_FOUND다`() {
        Mockito.`when`(userNoticeService.getNotice(7L))
            .thenThrow(EntityNotFoundException(NoticeErrorCode.NOTICE_NOT_FOUND))

        mockMvc.perform(get("/api/v1/notices/7"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOTICE_NOT_FOUND"))
    }

    @Test
    fun `사용자 API로는 공지를 작성할 수 없다`() {
        mockMvc.perform(post("/api/v1/notices"))
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(userNoticeService)
    }

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 22, 10, 0)
    }
}
