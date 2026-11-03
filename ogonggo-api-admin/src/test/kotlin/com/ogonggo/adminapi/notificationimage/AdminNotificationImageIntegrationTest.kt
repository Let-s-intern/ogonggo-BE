package com.ogonggo.adminapi.notificationimage

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.core.image.implement.ImageUploader
import com.ogonggo.core.image.implement.dto.ImageUploadDto
import com.ogonggo.core.image.implement.dto.ImageUploadResultDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:admin-notification-image;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
])
@AutoConfigureMockMvc
class AdminNotificationImageIntegrationTest @Autowired constructor(
    private val mvc: MockMvc,
    private val mapper: ObjectMapper,
) {
    @MockBean private lateinit var uploader: ImageUploader

    @Test
    @DisplayName("인증되지 않은 사용자는 관리자 푸시 이미지를 업로드할 수 없다")
    fun `관리자 이미지 업로드는 인증이 필요하다`() {
        // given
        val image = MockMultipartFile("file", "push.png", "image/png", byteArrayOf(1, 2, 3))
        // when, then
        mvc.perform(multipart("/api/v1/admin/notification-images").file(image)).andExpect(status().isUnauthorized)
        Mockito.verifyNoInteractions(uploader)
    }

    @Test
    @DisplayName("관리자는 푸시 알림 이미지를 업로드하고 이미지 ID와 URL을 받는다")
    fun `관리자 알림 이미지를 업로드한다`() {
        // given
        val image = MockMultipartFile("file", "push.png", "image/png", byteArrayOf(1, 2, 3))
        Mockito.`when`(uploader.upload(eq(42L), any(ImageUploadDto::class.java) ?: ImageUploadDto(byteArrayOf(1))))
            .thenReturn(ImageUploadResultDto("image-1", "https://cdn.example.com/push.png", "image/png", 3))
        val admin = UsernamePasswordAuthenticationToken(42L, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

        // when
        val response = mvc.perform(multipart("/api/v1/admin/notification-images").file(image).with(authentication(admin)))
            .andExpect(status().isCreated).andReturn().response

        // then
        val data = mapper.readTree(response.contentAsString)["data"]
        assertEquals("image-1", data["id"].asText())
        assertEquals("https://cdn.example.com/push.png", data["url"].asText())
    }
}
