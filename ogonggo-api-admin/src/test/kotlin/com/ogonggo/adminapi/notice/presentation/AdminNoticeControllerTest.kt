package com.ogonggo.adminapi.notice.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.notice.business.AdminNoticeCreateCommand
import com.ogonggo.adminapi.notice.business.AdminNoticePageResult
import com.ogonggo.adminapi.notice.business.AdminNoticeResult
import com.ogonggo.adminapi.notice.business.AdminNoticeService
import com.ogonggo.adminapi.notice.business.AdminNoticeSummary
import com.ogonggo.core.editor.lexical.LexicalEditorStateException
import com.ogonggo.core.notice.domain.NoticeManagementSearchCondition
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [AdminNoticeController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminNoticeControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminNoticeService: AdminNoticeService

    @Test
    fun `관리자 토큰이 없으면 공지 목록을 볼 수 없다`() {
        mockMvc.perform(get("/api/v1/admin/notices"))
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminNoticeService)
    }

    @Test
    fun `노출 필터를 게시 여부 조건으로 옮기고 기본 크기는 20이다`() {
        Mockito.`when`(adminNoticeService.getNotices(NoticeManagementSearchCondition(published = false), 0, 20))
            .thenReturn(AdminNoticePageResult(emptyList(), page = 0, size = 20, totalElements = 0, totalPages = 0))

        mockMvc.perform(admin(get("/api/v1/admin/notices").param("visibility", "HIDDEN")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20))
    }

    @Test
    fun `공지를 등록하면 201과 등록한 공지 전체를 준다`() {
        // given
        val command = AdminNoticeCreateCommand(
            title = "서비스 점검 안내",
            content = CONTENT,
            pinned = true,
            visibility = AdminContentVisibility.VISIBLE,
        )
        Mockito.`when`(adminNoticeService.createNotice(command)).thenReturn(NOTICE_ID)
        Mockito.`when`(adminNoticeService.getNotice(NOTICE_ID)).thenReturn(noticeResult())

        // when & then
        mockMvc.perform(
            admin(
                post("/api/v1/admin/notices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "title" to "서비스 점검 안내",
                                "content" to CONTENT,
                                "pinned" to true,
                                "visibility" to "VISIBLE",
                            ),
                        ),
                    ),
            ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.id").value(NOTICE_ID))
            .andExpect(jsonPath("$.data.content").value(CONTENT))
    }

    @Test
    fun `등록할 때 상단 고정이나 노출 여부를 빠뜨리면 400이다`() {
        mockMvc.perform(
            admin(
                post("/api/v1/admin/notices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("title" to "공지", "content" to CONTENT))),
            ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(adminNoticeService)
    }

    @Test
    fun `본문이 에디터 JSON이 아니면 content 필드 오류로 알린다`() {
        val command = AdminNoticeCreateCommand(
            title = "공지",
            content = "평문",
            pinned = false,
            visibility = AdminContentVisibility.HIDDEN,
        )
        Mockito.`when`(adminNoticeService.createNotice(command))
            .thenThrow(LexicalEditorStateException("에디터 내용 JSON 형식이 올바르지 않습니다."))

        mockMvc.perform(
            admin(
                post("/api/v1/admin/notices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf("title" to "공지", "content" to "평문", "pinned" to false, "visibility" to "HIDDEN"),
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
            admin(
                patch("/api/v1/admin/notices/$NOTICE_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("title" to " "))),
            ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[title] 제목을 입력해 주세요."))

        Mockito.verifyNoInteractions(adminNoticeService)
    }

    private fun noticeResult(): AdminNoticeResult = AdminNoticeResult(
        summary = AdminNoticeSummary(
            id = NOTICE_ID,
            title = "서비스 점검 안내",
            pinned = true,
            visibility = AdminContentVisibility.VISIBLE,
            registeredAt = NOW,
            updatedAt = NOW,
        ),
        content = CONTENT,
    )

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private companion object {
        const val NOTICE_ID = 7L
        const val CONTENT = """{"root":{"children":[],"type":"root","version":1}}"""
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 22, 10, 0)
    }
}
