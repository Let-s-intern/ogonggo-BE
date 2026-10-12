package com.ogonggo.core.image.implement

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.domain.ImageAsset
import com.ogonggo.core.image.domain.ImageAssetStatus
import com.ogonggo.core.image.domain.ImageAssetUsage
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.implement.dto.ImagePresignDto
import com.ogonggo.core.image.implement.dto.ImageUploadDto
import com.ogonggo.core.storage.s3.PresignedUpload
import com.ogonggo.core.storage.s3.S3ObjectClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.stubbing.Answer

class ImageUploaderTest {

    private val imageFileValidator = ImageFileValidator()
    private val imageAssetReader = Mockito.mock(ImageAssetReader::class.java)
    private val imageAssetManager = Mockito.mock(ImageAssetManager::class.java)
    private lateinit var uploadedKey: String
    private lateinit var uploadedContent: ByteArray
    private lateinit var uploadedContentType: String
    private val s3ObjectClient = Mockito.mock(
        S3ObjectClient::class.java,
        Answer { invocation ->
            if (invocation.method.name == "put") {
                uploadedKey = invocation.arguments[0] as String
                uploadedContent = invocation.arguments[1] as ByteArray
                uploadedContentType = invocation.arguments[2] as String
                "https://cdn.example.com/$uploadedKey"
            } else if (invocation.method.name == "urlOf") {
                "https://cdn.example.com/${invocation.arguments[0]}"
            } else if (invocation.method.name == "presignPut") {
                PresignedUpload(
                    url = "https://bucket.s3.amazonaws.com/${invocation.arguments[0]}?X-Amz-Signature=sig",
                    headers = mapOf("Content-Type" to invocation.arguments[1] as String),
                )
            } else {
                Mockito.RETURNS_DEFAULTS.answer(invocation)
            }
        },
    )
    private val uploader = ImageUploader(imageFileValidator, imageAssetReader, imageAssetManager, s3ObjectClient)

    @Test
    fun `용도를 보내지 않은 multipart 업로드는 ogonggo etc 폴더에 UUID key로 저장한다`() {
        val content = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        )
        val result = uploader.upload(17L, ImageUploadDto(content))

        assertTrue(result.url.matches(Regex("https://cdn.example.com/ogonggo/etc/[0-9a-f-]{36}\\.png")))
        assertEquals("image/png", result.mimeType)
        assertEquals(content.size.toLong(), result.size)
        assertTrue(uploadedKey.matches(Regex("ogonggo/etc/[0-9a-f-]{36}\\.png")))
        assertTrue(uploadedContent.contentEquals(content))
        assertEquals("image/png", uploadedContentType)
        val invocation = Mockito.mockingDetails(imageAssetManager).invocations
            .single { it.method.name == "startUploading" }
        assertEquals(17L, invocation.arguments[1])
        assertEquals("image/png", invocation.arguments[4])
        assertEquals(content.size.toLong(), invocation.arguments[5])
    }

    @Test
    fun `presigned URL을 발급하면 용도별 폴더에 알린 형식과 크기로 업로드 중 이미지를 기록한다`() {
        // when
        val result = uploader.issueUploadUrl(
            17L,
            ImagePresignDto(usage = ImageAssetUsage.CONCERN, mimeType = "image/png", size = 1_100_000L),
        )

        // then
        assertEquals("https://cdn.example.com/ogonggo/concern/${result.id}.png", result.url)
        assertTrue(result.uploadUrl.contains("ogonggo/concern/${result.id}.png"))
        assertEquals("image/png", result.uploadHeaders["Content-Type"])
        val invocation = Mockito.mockingDetails(imageAssetManager).invocations
            .single { it.method.name == "startUploading" }
        assertEquals(result.id, invocation.arguments[0])
        assertEquals("image/png", invocation.arguments[4])
        assertEquals(1_100_000L, invocation.arguments[5])
    }

    @Test
    fun `용도마다 ogonggo 아래 정해진 폴더에 둔다`() {
        mapOf(
            ImageAssetUsage.PROFILE to "ogonggo/profile/",
            ImageAssetUsage.RECRUITMENT_POST to "ogonggo/side/",
            ImageAssetUsage.CONCERN to "ogonggo/concern/",
        ).forEach { (usage, directory) ->
            val result = uploader.issueUploadUrl(17L, ImagePresignDto(usage, "image/jpeg", 100L))
            assertEquals("https://cdn.example.com/$directory${result.id}.jpg", result.url)
        }
    }

    @Test
    fun `지원하지 않는 형식이나 10 MiB를 넘는 파일은 URL을 발급하지 않는다`() {
        listOf(
            ImagePresignDto(ImageAssetUsage.PROFILE, "image/gif", 100L) to ImageUploadErrorCode.IMAGE_FILE_TYPE_NOT_SUPPORTED,
            ImagePresignDto(ImageAssetUsage.PROFILE, "image/png", 10L * 1024 * 1024 + 1) to ImageUploadErrorCode.IMAGE_FILE_TOO_LARGE,
        ).forEach { (command, errorCode) ->
            val exception = assertThrows(InvalidValueException::class.java) { uploader.issueUploadUrl(17L, command) }
            assertEquals(errorCode, exception.errorCode)
        }
        Mockito.verifyNoInteractions(imageAssetManager)
    }

    @Test
    fun `S3에 올라온 파일이 알린 크기와 형식이면 업로드를 완료한다`() {
        // given
        val asset = uploadingAsset()
        Mockito.`when`(imageAssetReader.readOwned(17L, asset.id)).thenReturn(asset)
        Mockito.doReturn(asset.size).`when`(s3ObjectClient).sizeOf(asset.storageKey)
        Mockito.doReturn(PNG_SIGNATURE).`when`(s3ObjectClient).readHead(asset.storageKey, 12)

        // when
        val result = uploader.completeUpload(17L, asset.id)

        // then
        assertEquals(asset.url, result.url)
        Mockito.verify(imageAssetManager).markUploaded(asset.id)
    }

    @Test
    fun `아직 S3에 올리지 않았으면 IMAGE_FILE_NOT_UPLOADED이고 완료하지 않는다`() {
        // given
        val asset = uploadingAsset()
        Mockito.`when`(imageAssetReader.readOwned(17L, asset.id)).thenReturn(asset)
        Mockito.doReturn(null).`when`(s3ObjectClient).sizeOf(asset.storageKey)

        // when
        val exception = assertThrows(ConflictException::class.java) { uploader.completeUpload(17L, asset.id) }

        // then
        assertEquals(ImageUploadErrorCode.IMAGE_FILE_NOT_UPLOADED, exception.errorCode)
        Mockito.verifyNoInteractions(imageAssetManager)
    }

    @Test
    fun `올린 파일이 알린 형식의 이미지가 아니면 완료하지 않는다`() {
        // given
        val asset = uploadingAsset()
        Mockito.`when`(imageAssetReader.readOwned(17L, asset.id)).thenReturn(asset)
        Mockito.doReturn(asset.size).`when`(s3ObjectClient).sizeOf(asset.storageKey)
        Mockito.doReturn("<html>hello".toByteArray()).`when`(s3ObjectClient).readHead(asset.storageKey, 12)

        // when
        val exception = assertThrows(InvalidValueException::class.java) { uploader.completeUpload(17L, asset.id) }

        // then
        assertEquals(ImageUploadErrorCode.IMAGE_FILE_TYPE_NOT_SUPPORTED, exception.errorCode)
        Mockito.verifyNoInteractions(imageAssetManager)
    }

    @Test
    fun `이미 완료한 이미지를 다시 완료하면 S3를 다시 보지 않고 같은 결과를 준다`() {
        // given
        val asset = uploadingAsset().also { it.markUploaded() }
        Mockito.`when`(imageAssetReader.readOwned(17L, asset.id)).thenReturn(asset)

        // when
        val result = uploader.completeUpload(17L, asset.id)

        // then
        assertEquals(ImageAssetStatus.TEMPORARY, asset.status)
        assertEquals(asset.id, result.id)
        Mockito.verifyNoInteractions(imageAssetManager)
        assertTrue(Mockito.mockingDetails(s3ObjectClient).invocations.none { it.method.name in setOf("sizeOf", "readHead") })
    }

    private fun uploadingAsset(): ImageAsset = ImageAsset.uploading(
        id = "image-id",
        ownerUserId = 17L,
        storageKey = "images/image-id.png",
        url = "https://cdn.example.com/images/image-id.png",
        mimeType = "image/png",
        size = 1_100_000L,
    )

    private companion object {
        val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D)
    }
}
