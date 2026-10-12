package com.ogonggo.core.image.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ImageAssetTest {

    @Test
    fun `임시 이미지를 첨부하면 첨부 상태가 된다`() {
        val asset = asset().also { it.markUploaded() }

        asset.attach()

        assertEquals(ImageAssetStatus.ATTACHED, asset.status)
    }

    @Test
    fun `이미 첨부된 이미지는 다른 곳에서 쓰는 중일 수 있어 다시 첨부할 수 없다`() {
        val asset = asset().also {
            it.markUploaded()
            it.attach()
        }

        assertThrows(IllegalStateException::class.java) { asset.attach() }
    }

    @Test
    fun `쓰이지 않게 된 이미지는 참조 해제 상태와 시각을 기록하고 다시 첨부할 수 있다`() {
        // given
        val asset = asset().also {
            it.markUploaded()
            it.attach()
        }
        val now = LocalDateTime.of(2026, 9, 12, 10, 0)

        // when
        asset.unreference(now)

        // then
        assertEquals(ImageAssetStatus.UNREFERENCED, asset.status)
        assertEquals(now, asset.unreferencedAt)
        asset.attach()
        assertEquals(ImageAssetStatus.ATTACHED, asset.status)
        assertEquals(null, asset.unreferencedAt)
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
