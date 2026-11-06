package com.ogonggo.adminapi.adchannelformat.presentation

import com.ogonggo.adminapi.adchannelformat.business.AdminAdChannelFormatOrderCommand
import com.ogonggo.adminapi.adchannelformat.business.AdminAdChannelFormatResult
import com.ogonggo.adminapi.adchannelformat.business.AdminAdChannelFormatService
import com.ogonggo.adminapi.adchannelformat.business.AdminAdChannelFormatUpdateCommand
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.Optional

@WebMvcTest(controllers = [AdminAdChannelFormatController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminAdChannelFormatControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminAdChannelFormatService: AdminAdChannelFormatService

    @Test
    fun `순서 바꾸기는 PUT order로 받아 식별자 경로에 먹히지 않고 모든 유형 목록을 준다`() {
        // given
        Mockito.`when`(adminAdChannelFormatService.getFormats()).thenReturn(listOf(result()))

        // when
        mockMvc.perform(
            admin(put("/api/v1/admin/ad-channel-formats/order"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"platform":"KAKAO_OPENCHAT","formatIds":[8,6,7,9]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(FORMAT_ID))
            .andExpect(jsonPath("$.data[0].channelIds[0]").value(6))
            .andExpect(jsonPath("$.data[0].displayOrder").value(1))

        // then
        Mockito.verify(adminAdChannelFormatService)
            .reorderFormats(AdminAdChannelFormatOrderCommand(AdChannelPlatform.KAKAO_OPENCHAT, listOf(8, 6, 7, 9)))
    }

    @Test
    fun `수정은 null을 보낸 단가를 지우고 삭제는 지운 유형의 식별자를 준다`() {
        // given
        Mockito.`when`(adminAdChannelFormatService.getFormat(FORMAT_ID)).thenReturn(result())

        // when
        mockMvc.perform(
            admin(patch("/api/v1/admin/ad-channel-formats/$FORMAT_ID"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"defaultPrice":null,"channelIds":[]}"""),
        )
            .andExpect(status().isOk)
        mockMvc.perform(admin(delete("/api/v1/admin/ad-channel-formats/$FORMAT_ID")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(FORMAT_ID))

        // then
        Mockito.verify(adminAdChannelFormatService)
            .updateFormat(FORMAT_ID, AdminAdChannelFormatUpdateCommand(defaultPrice = Optional.empty(), channelIds = emptyList()))
        Mockito.verify(adminAdChannelFormatService).deleteFormat(FORMAT_ID)
    }

    private fun result() = AdminAdChannelFormatResult(
        id = FORMAT_ID,
        platform = AdChannelPlatform.KAKAO_OPENCHAT,
        name = "공지 등록",
        description = null,
        enabled = true,
        defaultPrice = 100_000,
        channelIds = listOf(6),
        displayOrder = 1,
        adCount = 0,
        createdAt = SEED_TIME,
        updatedAt = SEED_TIME,
    )

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private companion object {
        const val FORMAT_ID = 8L
        val SEED_TIME: LocalDateTime = LocalDateTime.of(2026, 11, 6, 9, 0)
    }
}
