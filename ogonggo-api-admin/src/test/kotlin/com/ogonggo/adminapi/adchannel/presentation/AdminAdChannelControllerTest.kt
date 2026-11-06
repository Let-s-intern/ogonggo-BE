package com.ogonggo.adminapi.adchannel.presentation

import com.ogonggo.adminapi.adchannel.business.AdminAdChannelCreateCommand
import com.ogonggo.adminapi.adchannel.business.AdminAdChannelResult
import com.ogonggo.adminapi.adchannel.business.AdminAdChannelService
import com.ogonggo.adminapi.adchannel.business.AdminAdChannelUpdateCommand
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.Optional

@WebMvcTest(controllers = [AdminAdChannelController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminAdChannelControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminAdChannelService: AdminAdChannelService

    @Test
    fun `채널을 등록하면 201과 광고 수를 실은 채널을 준다`() {
        // given
        val command = AdminAdChannelCreateCommand(
            platform = AdChannelPlatform.INSTAGRAM,
            name = "렛츠커리어 오공고",
            handle = "@letscareer.job",
            url = "https://www.instagram.com/letscareer.job",
            profileImageUrl = "/community/instagram-job-profile-ogonggo.jpg",
            memberCount = null,
            enabled = null,
            memo = null,
        )
        Mockito.`when`(adminAdChannelService.createChannel(command)).thenReturn(CHANNEL_ID)
        Mockito.`when`(adminAdChannelService.getChannel(CHANNEL_ID)).thenReturn(result())

        // when & then
        mockMvc.perform(
            admin(post("/api/v1/admin/ad-channels"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"platform":"INSTAGRAM","name":"렛츠커리어 오공고","handle":"@letscareer.job",
                     "url":"https://www.instagram.com/letscareer.job","profileImageUrl":"/community/instagram-job-profile-ogonggo.jpg"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value(CHANNEL_ID))
            .andExpect(jsonPath("$.data.platform").value("INSTAGRAM"))
            .andExpect(jsonPath("$.data.handle").value("letscareer.job"))
            .andExpect(jsonPath("$.data.memberCount").isEmpty)
            .andExpect(jsonPath("$.data.adCount").value(2))
            .andExpect(jsonPath("$.data.createdAt").value("2026-11-06T09:00:00"))
    }

    @Test
    fun `목록은 배열로 준다`() {
        Mockito.`when`(adminAdChannelService.getChannels()).thenReturn(listOf(result()))

        mockMvc.perform(admin(get("/api/v1/admin/ad-channels")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(CHANNEL_ID))
    }

    @Test
    fun `수정은 null을 보낸 칸은 지우고 보내지 않은 칸은 그대로 둔다`() {
        // given
        Mockito.`when`(adminAdChannelService.getChannel(CHANNEL_ID)).thenReturn(result())

        // when
        mockMvc.perform(
            admin(patch("/api/v1/admin/ad-channels/$CHANNEL_ID"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"memberCount":null,"enabled":false}"""),
        )
            .andExpect(status().isOk)

        // then
        Mockito.verify(adminAdChannelService)
            .updateChannel(CHANNEL_ID, AdminAdChannelUpdateCommand(memberCount = Optional.empty(), enabled = false))
    }

    @Test
    fun `광고가 잡힌 채널을 지우면 화면 문구로 400을 준다`() {
        // given
        Mockito.doThrow(AdminConsoleBadRequestException("이 채널에 잡힌 채널 광고가 있어 지울 수 없습니다. 사용 안 함으로 바꿔 주세요."))
            .`when`(adminAdChannelService).deleteChannel(CHANNEL_ID)

        // when & then
        mockMvc.perform(admin(delete("/api/v1/admin/ad-channels/$CHANNEL_ID")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("이 채널에 잡힌 채널 광고가 있어 지울 수 없습니다. 사용 안 함으로 바꿔 주세요."))
    }

    private fun result() = AdminAdChannelResult(
        id = CHANNEL_ID,
        platform = AdChannelPlatform.INSTAGRAM,
        name = "렛츠커리어 오공고",
        handle = "letscareer.job",
        url = "https://www.instagram.com/letscareer.job",
        profileImageUrl = "/community/instagram-job-profile-ogonggo.jpg",
        memberCount = null,
        enabled = true,
        memo = null,
        adCount = 2,
        createdAt = SEED_TIME,
        updatedAt = SEED_TIME,
    )

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private companion object {
        const val CHANNEL_ID = 1L
        val SEED_TIME: LocalDateTime = LocalDateTime.of(2026, 11, 6, 9, 0)
    }
}
