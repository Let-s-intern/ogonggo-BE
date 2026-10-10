package com.ogonggo.core.image.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.domain.ImageAsset
import com.ogonggo.core.image.domain.ImageAssetStatus
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.persistence.ImageAssetJpaRepository
import com.ogonggo.core.storage.s3.S3ObjectClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Duration
import java.time.LocalDateTime

class ImageAssetManagerTest {

    private val repository = Mockito.mock(ImageAssetJpaRepository::class.java)
    private val storage = Mockito.mock(S3ObjectClient::class.java)
    private val manager = ImageAssetManager(repository, storage, ObjectMapper())
    private val now = LocalDateTime.of(2026, 9, 12, 10, 0)

    @Test
    fun `게시글 본문의 이미지 ID와 URL이 일치하면 이미지를 연결한다`() {
        val asset = uploadedAsset()
        Mockito.`when`(
            repository.findAllByIdInAndOwnerUserIdAndDeletedAtIsNull(setOf(asset.id), 17L),
        ).thenReturn(listOf(asset))

        manager.syncPostImages(17L, 12L, null, content(asset), now)

        assertEquals(ImageAssetStatus.ATTACHED, asset.status)
        assertEquals(12L, asset.postId)
    }

    @Test
    fun `게시글에서 이미지가 제거되면 참조 해제한다`() {
        val asset = uploadedAsset().also { it.attach(12L) }
        Mockito.`when`(
            repository.findAllByIdInAndOwnerUserIdAndDeletedAtIsNull(setOf(asset.id), 17L),
        ).thenReturn(listOf(asset))

        manager.syncPostImages(17L, 12L, content(asset), plainContent(), now)

        assertEquals(ImageAssetStatus.UNREFERENCED, asset.status)
        assertEquals(now, asset.unreferencedAt)
    }

    @Test
    fun `게시글 이미지를 새 저장 키와 새 자산으로 복제하고 본문 참조를 치환한다`() {
        val asset = uploadedAsset().also { it.attach(12L) }
        Mockito.`when`(
            repository.findAllByIdInAndOwnerUserIdAndDeletedAtIsNull(setOf(asset.id), 17L),
        ).thenReturn(listOf(asset))
        Mockito.`when`(storage.urlOf(Mockito.anyString())).thenAnswer { invocation ->
            "https://cdn.example.com/${invocation.arguments[0]}"
        }

        val copiedContent = manager.copyPostImages(
            ownerUserId = 17L,
            sourcePostId = 12L,
            targetPostId = 101L,
            content = content(asset),
        )

        assertTrue(copiedContent.orEmpty().contains("https://cdn.example.com/images/"))
        assertTrue(copiedContent.orEmpty().contains("imageId"))
        assertTrue(!copiedContent.orEmpty().contains(asset.id))
        val copyInvocation = Mockito.mockingDetails(storage).invocations.single { it.method.name == "copy" }
        assertEquals(asset.storageKey, copyInvocation.arguments[0])
        assertTrue((copyInvocation.arguments[1] as String).startsWith("images/"))
        assertEquals(asset.mimeType, copyInvocation.arguments[2])
        val saveInvocation = Mockito.mockingDetails(repository).invocations.single { it.method.name == "save" }
        assertEquals(101L, (saveInvocation.arguments[0] as ImageAsset).postId)
        assertEquals(12L, asset.postId)
    }

    @Test
    fun `업로드한 이미지를 프로필 이미지로 연결하면 정리 대상에서 빠지고 게시글에는 쓸 수 없다`() {
        // given
        val asset = uploadedAsset()
        Mockito.`when`(repository.findByIdAndOwnerUserIdAndDeletedAtIsNull(asset.id, 17L)).thenReturn(asset)
        Mockito.`when`(
            repository.findAllByIdInAndOwnerUserIdAndDeletedAtIsNull(setOf(asset.id), 17L),
        ).thenReturn(listOf(asset))

        // when
        val url = manager.attachProfileImage(17L, asset.id)

        // then
        assertEquals(asset.url, url)
        assertEquals(ImageAssetStatus.ATTACHED, asset.status)
        val exception = assertThrows(InvalidValueException::class.java) {
            manager.syncPostImages(17L, 12L, null, content(asset), now)
        }
        assertEquals(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE, exception.errorCode)
    }

    @Test
    fun `게시글에 쓰는 이미지나 남의 이미지는 프로필 이미지로 연결할 수 없다`() {
        // given
        val postImage = uploadedAsset().also { it.attach(12L) }
        Mockito.`when`(repository.findByIdAndOwnerUserIdAndDeletedAtIsNull(postImage.id, 17L)).thenReturn(postImage)

        // when & then
        listOf(17L to postImage.id, 18L to postImage.id).forEach { (ownerUserId, imageId) ->
            val exception = assertThrows(InvalidValueException::class.java) {
                manager.attachProfileImage(ownerUserId, imageId)
            }
            assertEquals(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE, exception.errorCode)
        }
        assertEquals(12L, postImage.postId)
    }

    @Test
    fun `프로필 이미지 참조를 풀어도 게시글에 쓰는 이미지는 그대로 둔다`() {
        // given
        val profileImage = uploadedAsset().also { it.attachToProfile() }
        val postImage = ImageAsset.uploading(
            id = "post-image-id",
            ownerUserId = 17L,
            storageKey = "images/post-image-id.png",
            url = "https://cdn.example.com/images/post-image-id.png",
            mimeType = "image/png",
            size = 3L,
        ).also {
            it.markUploaded()
            it.attach(12L)
        }
        Mockito.`when`(repository.findByIdAndOwnerUserIdAndDeletedAtIsNull(profileImage.id, 17L)).thenReturn(profileImage)
        Mockito.`when`(repository.findByIdAndOwnerUserIdAndDeletedAtIsNull(postImage.id, 17L)).thenReturn(postImage)

        // when
        manager.unreferenceProfileImage(17L, profileImage.id, now)
        manager.unreferenceProfileImage(17L, postImage.id, now)

        // then
        assertEquals(ImageAssetStatus.UNREFERENCED, profileImage.status)
        assertEquals(now, profileImage.unreferencedAt)
        assertEquals(ImageAssetStatus.ATTACHED, postImage.status)
    }

    @Test
    fun `보존 기간이 지난 임시 이미지는 S3 삭제 후 삭제 상태로 변경한다`() {
        val asset = uploadedAsset()
        Mockito.`when`(
            repository.findAllByStatusInAndDeletedAtIsNullAndCreatedAtBefore(
                setOf(ImageAssetStatus.UPLOADING, ImageAssetStatus.TEMPORARY),
                now.minusHours(24),
                org.springframework.data.domain.PageRequest.of(0, 100),
            ),
        ).thenReturn(listOf(asset))

        val deletedCount = manager.cleanup(now, Duration.ofHours(24))

        assertEquals(1, deletedCount)
        assertEquals(ImageAssetStatus.DELETED, asset.status)
        Mockito.verify(storage).delete(asset.storageKey)
    }

    private fun uploadedAsset(): ImageAsset = ImageAsset.uploading(
        id = "image-id",
        ownerUserId = 17L,
        storageKey = "images/image-id.png",
        url = "https://cdn.example.com/images/image-id.png",
        mimeType = "image/png",
        size = 3L,
    ).also { it.markUploaded() }

    private fun content(asset: ImageAsset): String =
        """{"root":{"children":[{"type":"image","imageId":"${asset.id}","src":"${asset.url}"}]}}"""

    private fun plainContent(): String = """{"root":{"children":[]}}"""
}
