package com.ogonggo.adminapi.advertisernote.presentation

import com.ogonggo.adminapi.advertisernote.business.AdminAdvertiserNoteCreateCommand
import com.ogonggo.adminapi.advertisernote.business.AdminAdvertiserNoteResult
import com.ogonggo.adminapi.advertisernote.business.AdminAdvertiserNoteService
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.advertiser.error.AdvertiserErrorCode
import com.ogonggo.core.error.EntityNotFoundException
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [AdminAdvertiserNoteController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminAdvertiserNoteControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminAdvertiserNoteService: AdminAdvertiserNoteService

    @Test
    fun `메모를 쓰면 요청한 관리자를 쓴 사람으로 넘기고 201과 쓴 관리자 이름을 준다`() {
        // given
        val command = AdminAdvertiserNoteCreateCommand(title = "통화 기록", content = "12월 홈 배너 관심")
        Mockito.`when`(adminAdvertiserNoteService.createNote(ADVERTISER_ID, ADMIN_ID, command)).thenReturn(NOTE_ID)
        Mockito.`when`(adminAdvertiserNoteService.getNote(NOTE_ID)).thenReturn(
            AdminAdvertiserNoteResult(
                id = NOTE_ID,
                advertiserId = ADVERTISER_ID,
                title = "통화 기록",
                content = "12월 홈 배너 관심",
                authorName = "김운영",
                createdAt = NOW,
                updatedAt = NOW,
            ),
        )

        // when & then
        mockMvc.perform(
            admin(post("/api/v1/admin/advertisers/$ADVERTISER_ID/notes"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"통화 기록","content":"12월 홈 배너 관심","authorName":"보낸 이름은 무시"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.authorName").value("김운영"))
            .andExpect(jsonPath("$.data.createdAt").value("2026-11-06T10:00:00"))
    }

    @Test
    fun `없는 광고주의 메모는 404 ADVERTISER_NOT_FOUND다`() {
        Mockito.`when`(adminAdvertiserNoteService.getNotes(ADVERTISER_ID))
            .thenThrow(EntityNotFoundException(AdvertiserErrorCode.ADVERTISER_NOT_FOUND))

        mockMvc.perform(admin(get("/api/v1/admin/advertisers/$ADVERTISER_ID/notes")))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ADVERTISER_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("광고주를 찾을 수 없습니다."))
    }

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder = request.with(
        authentication(UsernamePasswordAuthenticationToken(ADMIN_ID, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY)))),
    )

    private companion object {
        const val ADVERTISER_ID = 3L
        const val ADMIN_ID = 9L
        const val NOTE_ID = 21L
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 10, 0)
    }
}
