package com.ogonggo.core.image.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.image.domain.ImageAsset
import com.ogonggo.core.image.domain.ImageAssetStatus
import com.ogonggo.core.image.persistence.ImageAssetJpaRepository
import com.ogonggo.core.storage.s3.S3ImageStorage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Duration
import java.time.LocalDateTime

class ImageAssetManagerTest {

    private val repository = Mockito.mock(ImageAssetJpaRepository::class.java)
    private val storage = Mockito.mock(S3ImageStorage::class.java)
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
