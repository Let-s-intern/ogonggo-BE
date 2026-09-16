package com.ogonggo.userapi.image.presentation

import com.ogonggo.core.image.implement.dto.ImageUploadCommand
import com.ogonggo.core.image.implement.dto.ImageUploadResult
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.image.business.ImageUploadService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [ImageUploadController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class ImageUploadControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var imageUploadService: ImageUploadService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `인증된 사용자가 이미지를 업로드하면 201과 이미지 정보를 반환한다`() {
        val content = byteArrayOf(1, 2, 3)
        val file = MockMultipartFile("file", "image.png", "image/png", content)
        val command = ImageUploadCommand(content)
        Mockito.`when`(imageUploadService.upload(USER_ID, command))
            .thenReturn(
                ImageUploadResult(
                    id = "image-id",
                    url = "https://cdn.example.com/images/image-id.png",
                    mimeType = "image/png",
                    size = 3L,
                ),
            )

        mockMvc.perform(
            multipart("/api/v1/images")
                .file(file)
                .with(authentication(authenticatedUser())),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.id").value("image-id"))
            .andExpect(jsonPath("$.data.url").value("https://cdn.example.com/images/image-id.png"))

        Mockito.verify(imageUploadService).upload(USER_ID, command)
    }

    @Test
    fun `인증되지 않은 사용자는 이미지를 업로드할 수 없다`() {
        mockMvc.perform(
            multipart("/api/v1/images")
                .file(MockMultipartFile("file", "image.png", "image/png", byteArrayOf(1))),
        )
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(imageUploadService)
    }

    @Test
    fun `파일 파트가 없으면 이미지 필수 오류를 반환한다`() {
        mockMvc.perform(
            multipart("/api/v1/images")
                .with(authentication(authenticatedUser())),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("IMAGE_FILE_REQUIRED"))

        Mockito.verifyNoInteractions(imageUploadService)
    }

    private fun authenticatedUser(): UsernamePasswordAuthenticationToken =
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList())

    companion object {
        private const val USER_ID = 17L
    }
}
