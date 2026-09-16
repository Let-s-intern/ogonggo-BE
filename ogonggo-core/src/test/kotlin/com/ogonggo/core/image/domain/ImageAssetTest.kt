package com.ogonggo.core.image.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ImageAssetTest {

    @Test
    fun `이미지는 임시 상태에서 게시글에 연결된다`() {
        val asset = asset().also { it.markUploaded() }

        asset.attach(12L)

        assertEquals(ImageAssetStatus.ATTACHED, asset.status)
        assertEquals(12L, asset.postId)
    }

    @Test
    fun `게시글에서 이미지가 빠지면 참조 해제 상태와 시각을 기록한다`() {
        val asset = asset().also {
            it.markUploaded()
            it.attach(12L)
        }
        val now = LocalDateTime.of(2026, 9, 12, 10, 0)

        asset.unreference(now)

        assertEquals(ImageAssetStatus.UNREFERENCED, asset.status)
        assertEquals(null, asset.postId)
        assertEquals(now, asset.unreferencedAt)
    }

    private fun asset() = ImageAsset.uploading(
        id = "image-id",
        ownerUserId = 17L,
        storageKey = "images/image-id.png",
        url = "https://cdn.example.com/images/image-id.png",
        mimeType = "image/png",
        size = 3L,
    )
}
