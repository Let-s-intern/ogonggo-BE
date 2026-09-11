package com.ogonggo.userapi.image.business

import com.ogonggo.core.storage.s3.S3ImageStorage
import com.ogonggo.userapi.image.implement.ImageFileValidator
import com.ogonggo.userapi.image.implement.ValidatedImage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.stubbing.Answer

class ImageUploadServiceTest {

    private val imageFileValidator = Mockito.mock(ImageFileValidator::class.java)
    private lateinit var uploadedKey: String
    private lateinit var uploadedContent: ByteArray
    private lateinit var uploadedContentType: String
    private val s3ImageStorage = Mockito.mock(
        S3ImageStorage::class.java,
        Answer { invocation ->
            if (invocation.method.name == "put") {
                uploadedKey = invocation.arguments[0] as String
                uploadedContent = invocation.arguments[1] as ByteArray
                uploadedContentType = invocation.arguments[2] as String
                "https://cdn.example.com/images/17/image.png"
            } else {
                Mockito.RETURNS_DEFAULTS.answer(invocation)
            }
        },
    )
    private val service = ImageUploadService(imageFileValidator, s3ImageStorage)

    @Test
    fun `사용자별 UUID key로 이미지를 S3에 저장하고 업로드 결과를 반환한다`() {
        val content = byteArrayOf(1, 2, 3)
        val command = UploadImageCommand(content)
        Mockito.`when`(imageFileValidator.validate(command)).thenReturn(
            ValidatedImage(
                content = content,
                mimeType = "image/png",
                extension = "png",
            ),
        )
        val result = service.upload(17L, command)

        assertEquals("https://cdn.example.com/images/17/image.png", result.url)
        assertEquals("image/png", result.mimeType)
        assertEquals(3L, result.size)

        assertEquals(true, uploadedKey.matches(Regex("images/[0-9a-f-]{36}\\.png")))
        assertEquals(true, uploadedContent.contentEquals(content))
        assertEquals("image/png", uploadedContentType)
    }
}
