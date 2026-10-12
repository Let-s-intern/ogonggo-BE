package com.ogonggo.userapi.image.presentation

import com.ogonggo.core.image.domain.ImageAssetUsage
import com.ogonggo.core.image.implement.dto.ImagePresignDto
import com.ogonggo.core.image.implement.dto.ImagePresignedUploadDto
import com.ogonggo.core.image.implement.dto.ImageUploadDto
import com.ogonggo.core.image.implement.dto.ImageUploadResultDto
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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
        val command = ImageUploadDto(content)
        Mockito.`when`(imageUploadService.upload(USER_ID, command))
            .thenReturn(
                ImageUploadResultDto(
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

    @Test
    fun `presigned URL을 요청하면 201과 업로드 주소·헤더·표시 주소를 준다`() {
        // given
        Mockito.`when`(imageUploadService.issueUploadUrl(USER_ID, ImagePresignDto(ImageAssetUsage.CONCERN, "image/png", 1_100_000L))).thenReturn(
            ImagePresignedUploadDto(
                id = "image-id",
                uploadUrl = "https://bucket.s3.amazonaws.com/images/image-id.png?X-Amz-Signature=sig",
                uploadHeaders = mapOf("Content-Type" to "image/png"),
                url = "https://cdn.example.com/images/image-id.png",
            ),
        )

        // when
        mockMvc.perform(
            post("/api/v1/images/presigned-urls")
                .with(authentication(authenticatedUser()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"usage":"CONCERN","contentType":"image/png","size":1100000}"""),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value("image-id"))
            .andExpect(jsonPath("$.data.uploadUrl").value("https://bucket.s3.amazonaws.com/images/image-id.png?X-Amz-Signature=sig"))
            .andExpect(jsonPath("$.data.uploadHeaders['Content-Type']").value("image/png"))
            .andExpect(jsonPath("$.data.url").value("https://cdn.example.com/images/image-id.png"))
    }

    @Test
    fun `multipart 업로드에 용도를 보내면 그 용도로 저장한다`() {
        // given
        val content = byteArrayOf(1, 2, 3)
        val command = ImageUploadDto(content, ImageAssetUsage.PROFILE)
        Mockito.`when`(imageUploadService.upload(USER_ID, command)).thenReturn(
            ImageUploadResultDto(id = "image-id", url = "https://cdn.example.com/ogonggo/profile/image-id.png", mimeType = "image/png", size = 3L),
        )

        // when
        mockMvc.perform(
            multipart("/api/v1/images")
                .file(MockMultipartFile("file", "image.png", "image/png", content))
                .param("usage", "PROFILE")
                .with(authentication(authenticatedUser())),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.url").value("https://cdn.example.com/ogonggo/profile/image-id.png"))
    }

    @Test
    fun `presigned URL 요청에 용도가 없으면 400이다`() {
        // when
        mockMvc.perform(
            post("/api/v1/images/presigned-urls")
                .with(authentication(authenticatedUser()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"contentType":"image/png","size":10}"""),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(imageUploadService)
    }

    @Test
    fun `presigned URL 요청의 크기가 양수가 아니면 400이다`() {
        // when
        mockMvc.perform(
            post("/api/v1/images/presigned-urls")
                .with(authentication(authenticatedUser()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"usage":"CONCERN","contentType":"image/png","size":0}"""),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(imageUploadService)
    }

    @Test
    fun `업로드를 완료하면 200과 이미지 정보를 준다`() {
        // given
        Mockito.`when`(imageUploadService.completeUpload(USER_ID, "image-id")).thenReturn(
            ImageUploadResultDto(
                id = "image-id",
                url = "https://cdn.example.com/images/image-id.png",
                mimeType = "image/png",
                size = 1_100_000L,
            ),
        )

        // when
        mockMvc.perform(post("/api/v1/images/image-id/complete").with(authentication(authenticatedUser())))
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value("image-id"))
            .andExpect(jsonPath("$.data.size").value(1_100_000))
    }

    @Test
    fun `인증되지 않은 사용자는 업로드 URL을 받거나 완료할 수 없다`() {
        listOf(
            post("/api/v1/images/presigned-urls").contentType(MediaType.APPLICATION_JSON)
                .content("""{"usage":"CONCERN","contentType":"image/png","size":1}"""),
            post("/api/v1/images/image-id/complete"),
        ).forEach { request ->
            mockMvc.perform(request).andExpect(status().isUnauthorized)
        }

        Mockito.verifyNoInteractions(imageUploadService)
    }

    private fun authenticatedUser(): UsernamePasswordAuthenticationToken =
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList())

    companion object {
        private const val USER_ID = 17L
    }
}
