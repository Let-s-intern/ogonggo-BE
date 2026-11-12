package com.ogonggo.adminapi.announcement.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.adminapi.announcement.business.AdminAnnouncementCreateCommand
import com.ogonggo.adminapi.announcement.business.AdminAnnouncementPageResult
import com.ogonggo.adminapi.announcement.business.AdminAnnouncementResult
import com.ogonggo.adminapi.announcement.business.AdminAnnouncementService
import com.ogonggo.adminapi.announcement.business.AdminAnnouncementSummary
import com.ogonggo.core.announcement.domain.AnnouncementCategory
import com.ogonggo.core.announcement.domain.AnnouncementManagementSearchCondition
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [AdminAnnouncementController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminAnnouncementControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminAnnouncementService: AdminAnnouncementService

    @Test
    fun `관리자 토큰이 없으면 공지 목록을 볼 수 없다`() {
        mockMvc.perform(get("/api/v1/admin/announcements"))
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminAnnouncementService)
    }

    @Test
    fun `노출 필터를 게시 여부 조건으로 옮기고 기본 크기는 20이다`() {
        Mockito.`when`(adminAnnouncementService.getAnnouncements(AnnouncementManagementSearchCondition(published = false), 0, 20))
            .thenReturn(AdminAnnouncementPageResult(emptyList(), page = 0, size = 20, totalElements = 0, totalPages = 0))

        mockMvc.perform(admin(get("/api/v1/admin/announcements").param("visibility", "HIDDEN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20))
    }

    @Test
    fun `공지를 등록하면 201과 등록한 공지 전체를 준다`() {
        // given
        val command = AdminAnnouncementCreateCommand(
            title = "서비스 점검 안내",
            content = CONTENT,
            pinned = true,
            visibility = AdminContentVisibility.VISIBLE,
            category = AnnouncementCategory.SERVICE_NOTICE,
            thumbnailUrl = THUMBNAIL_URL,
            summary = "이번 주 일요일 새벽 점검",
        )
        Mockito.`when`(adminAnnouncementService.createAnnouncement(command, ADMIN_ID)).thenReturn(ANNOUNCEMENT_ID)
        Mockito.`when`(adminAnnouncementService.getAnnouncement(ANNOUNCEMENT_ID)).thenReturn(announcementResult())

        // when & then
        mockMvc.perform(
            adminWithId(
                post("/api/v1/admin/announcements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "title" to "서비스 점검 안내",
                                "content" to CONTENT,
                                "pinned" to true,
                                "visibility" to "VISIBLE",
                                "category" to "SERVICE_NOTICE",
                                "thumbnailUrl" to THUMBNAIL_URL,
                                "summary" to "이번 주 일요일 새벽 점검",
                            ),
                        ),
                    ),
            ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.id").value(ANNOUNCEMENT_ID))
            .andExpect(jsonPath("$.data.content").value(CONTENT))
            .andExpect(jsonPath("$.data.category").value("SERVICE_NOTICE"))
            .andExpect(jsonPath("$.data.thumbnailUrl").value(THUMBNAIL_URL))
    }

    @Test
    fun `요약이 300자를 넘거나 썸네일이 주소가 아니면 등록하지 않는다`() {
        val cases = listOf(
            mapOf("summary" to "가".repeat(301)),
            mapOf("thumbnailUrl" to "thumbnail.png"),
        )

        cases.forEach { extra ->
            mockMvc.perform(
                adminWithId(
                    post("/api/v1/admin/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "title" to "공지",
                                    "content" to CONTENT,
                                    "pinned" to false,
                                    "visibility" to "VISIBLE",
                                    "category" to "OGONGGO_UPDATE",
                                ) + extra,
                            ),
                        ),
                ),
            )
                .andExpect(status().isBadRequest)
        }

        Mockito.verifyNoInteractions(adminAnnouncementService)
    }

    @Test
    fun `등록할 때 상단 고정이나 노출 여부를 빠뜨리면 400이다`() {
        mockMvc.perform(
            admin(
                post("/api/v1/admin/announcements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("title" to "공지", "content" to CONTENT))),
            ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(adminAnnouncementService)
    }

    @Test
    fun `본문이 에디터 JSON이 아니면 content 필드 오류로 알린다`() {
        val command = AdminAnnouncementCreateCommand(
            title = "공지",
            content = "평문",
            pinned = false,
            visibility = AdminContentVisibility.HIDDEN,
            category = AnnouncementCategory.OGONGGO_UPDATE,
            thumbnailUrl = null,
            summary = null,
        )
        Mockito.`when`(adminAnnouncementService.createAnnouncement(command, ADMIN_ID))
            .thenThrow(InvalidRequestFieldException("content", "에디터 내용 JSON 형식이 올바르지 않습니다."))

        mockMvc.perform(
            adminWithId(
                post("/api/v1/admin/announcements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "title" to "공지",
                                "content" to "평문",
                                "pinned" to false,
                                "visibility" to "HIDDEN",
                                "category" to "OGONGGO_UPDATE",
                            ),
                        ),
                    ),
            ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[content] 에디터 내용 JSON 형식이 올바르지 않습니다."))
    }

    @Test
    fun `수정할 때 제목을 비울 수 없다`() {
        mockMvc.perform(
            adminWithId(
                patch("/api/v1/admin/announcements/$ANNOUNCEMENT_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("title" to " "))),
            ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[title] 제목을 입력해 주세요."))

        Mockito.verifyNoInteractions(adminAnnouncementService)
    }

    private fun announcementResult(): AdminAnnouncementResult = AdminAnnouncementResult(
        summary = AdminAnnouncementSummary(
            id = ANNOUNCEMENT_ID,
            title = "서비스 점검 안내",
            pinned = true,
            visibility = AdminContentVisibility.VISIBLE,
            category = AnnouncementCategory.SERVICE_NOTICE,
            thumbnailUrl = THUMBNAIL_URL,
            summary = "이번 주 일요일 새벽 점검",
            registeredAt = NOW,
            updatedAt = NOW,
        ),
        content = CONTENT,
    )

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    /** 등록·수정은 썸네일을 올린 관리자로 첨부하므로 관리자 식별자가 주체여야 한다. */
    private fun adminWithId(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(
            authentication(
                UsernamePasswordAuthenticationToken(ADMIN_ID, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY))),
            ),
        )

    private companion object {
        const val ANNOUNCEMENT_ID = 7L
        const val ADMIN_ID = 9L
        const val THUMBNAIL_URL = "https://bucket.example.com/ogonggo/notification/thumb.png"
        const val CONTENT = """{"root":{"children":[],"type":"root","version":1}}"""
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 22, 10, 0)
    }
}
