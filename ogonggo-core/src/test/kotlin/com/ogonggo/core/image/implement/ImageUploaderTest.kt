package com.ogonggo.core.image.implement

import com.ogonggo.core.image.implement.dto.ImageUploadCommand
import com.ogonggo.core.storage.s3.S3ImageStorage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.stubbing.Answer

class ImageUploaderTest {

    private val imageFileValidator = ImageFileValidator()
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
                "https://cdn.example.com/images/image.png"
            } else {
                Mockito.RETURNS_DEFAULTS.answer(invocation)
            }
        },
    )
    private val uploader = ImageUploader(imageFileValidator, s3ImageStorage)

    @Test
    fun `UUID key로 이미지를 S3에 저장하고 업로드 결과를 반환한다`() {
        val content = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        )
        val result = uploader.upload(ImageUploadCommand(content))

        assertEquals("https://cdn.example.com/images/image.png", result.url)
        assertEquals("image/png", result.mimeType)
        assertEquals(content.size.toLong(), result.size)
        assertTrue(uploadedKey.matches(Regex("images/[0-9a-f-]{36}\\.png")))
        assertTrue(uploadedContent.contentEquals(content))
        assertEquals("image/png", uploadedContentType)
    }
}
