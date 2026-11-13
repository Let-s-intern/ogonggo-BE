package com.ogonggo.userapi.announcement.presentation

import com.ogonggo.core.announcement.domain.AnnouncementCategory
import com.ogonggo.core.announcement.domain.AnnouncementPublicSearchCondition
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.announcement.error.AnnouncementErrorCode
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.announcement.business.UserAnnouncementCategoryResult
import com.ogonggo.userapi.announcement.business.UserAnnouncementPageResult
import com.ogonggo.userapi.announcement.business.UserAnnouncementService
import com.ogonggo.userapi.announcement.business.UserAnnouncementSummary
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

@WebMvcTest(controllers = [UserAnnouncementController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserAnnouncementControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userAnnouncementService: UserAnnouncementService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `로그인 없이 공지 목록을 1 기반 페이지로 조회한다`() {
        val summary = summary()
        Mockito.`when`(userAnnouncementService.getAnnouncements(AnnouncementPublicSearchCondition(), 0, 10))
            .thenReturn(UserAnnouncementPageResult(listOf(summary), page = 0, size = 10, totalElements = 1, totalPages = 1))

        mockMvc.perform(get("/api/v1/announcements"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(7))
            .andExpect(jsonPath("$.data.items[0].pinned").value(true))
            .andExpect(jsonPath("$.data.items[0].category").value("SERVICE_NOTICE"))
            .andExpect(jsonPath("$.data.items[0].summary").value("새벽 점검"))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
    }

    @Test
    fun `카테고리·고정 여부·검색어로 공지 목록을 거른다`() {
        val condition = AnnouncementPublicSearchCondition(
            category = AnnouncementCategory.NEW_JOB_NOTE,
            pinned = true,
            keyword = "삼성",
        )
        Mockito.`when`(userAnnouncementService.getAnnouncements(condition, 0, 3))
            .thenReturn(UserAnnouncementPageResult(emptyList(), page = 0, size = 3, totalElements = 0, totalPages = 0))

        mockMvc.perform(
            get("/api/v1/announcements")
                .param("size", "3")
                .param("category", "NEW_JOB_NOTE")
                .param("pinned", "true")
                .param("keyword", "삼성"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(3))
    }

    @Test
    fun `검색어가 100자를 넘으면 400이다`() {
        mockMvc.perform(get("/api/v1/announcements").param("keyword", "가".repeat(101)))
            .andExpect(status().isBadRequest)

        Mockito.verifyNoInteractions(userAnnouncementService)
    }

    @Test
    fun `로그인 없이 카테고리마다 공지 수와 최근 공지를 조회한다`() {
        Mockito.`when`(userAnnouncementService.getCategories()).thenReturn(
            listOf(
                UserAnnouncementCategoryResult(AnnouncementCategory.OGONGGO_UPDATE, count = 0, recentAnnouncements = emptyList()),
                UserAnnouncementCategoryResult(AnnouncementCategory.SERVICE_NOTICE, count = 1, recentAnnouncements = listOf(summary())),
            ),
        )

        mockMvc.perform(get("/api/v1/announcements/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].category").value("OGONGGO_UPDATE"))
            .andExpect(jsonPath("$.data[0].count").value(0))
            .andExpect(jsonPath("$.data[1].recentAnnouncements[0].id").value(7))
    }

    @Test
    fun `노출 중이 아닌 공지 상세는 404 ANNOUNCEMENT_NOT_FOUND다`() {
        Mockito.`when`(userAnnouncementService.getAnnouncement(7L))
            .thenThrow(EntityNotFoundException(AnnouncementErrorCode.ANNOUNCEMENT_NOT_FOUND))

        mockMvc.perform(get("/api/v1/announcements/7"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ANNOUNCEMENT_NOT_FOUND"))
    }

    @Test
    fun `사용자 API로는 공지를 작성할 수 없다`() {
        mockMvc.perform(post("/api/v1/announcements"))
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(userAnnouncementService)
    }

    private fun summary() = UserAnnouncementSummary(
        id = 7L,
        title = "서비스 점검 안내",
        pinned = true,
        category = AnnouncementCategory.SERVICE_NOTICE,
        thumbnailUrl = null,
        summary = "새벽 점검",
        createdAt = NOW,
    )

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 22, 10, 0)
    }
}
