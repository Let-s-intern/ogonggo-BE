package com.ogonggo.core.image.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.image.domain.ImageAsset
import com.ogonggo.core.image.domain.ImageAssetStatus
import com.ogonggo.core.image.persistence.ImageAssetJpaRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.storage.s3.S3ObjectClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import java.time.Duration
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
internal class ImageAssetPersistenceTest @Autowired constructor(
    private val repository: ImageAssetJpaRepository,
) {
    private val storage = Mockito.mock(S3ObjectClient::class.java)
    private val manager = ImageAssetManager(repository, storage, ObjectMapper())

    @Test
    fun `공고에 저장해 첨부한 이미지는 보존 기간이 지나도 남고 저장하지 않은 업로드만 지운다`() {
        // given
        val saved = upload("saved-logo", OWNER_ID)
        val notSaved = upload("not-saved-logo", OWNER_ID)
        manager.attachImageUrls(OWNER_ID, listOf(saved.url, "https://example.com/external.png"))
        repository.flush()

        // when
        val deletedCount = manager.cleanup(LocalDateTime.now().plusDays(2), Duration.ofHours(1))

        // then
        assertEquals(1, deletedCount)
        assertEquals(ImageAssetStatus.ATTACHED, repository.findById(saved.id).orElseThrow().status)
        assertEquals(ImageAssetStatus.DELETED, repository.findById(notSaved.id).orElseThrow().status)
        Mockito.verify(storage).delete(notSaved.storageKey)
        Mockito.verify(storage, Mockito.never()).delete(saved.storageKey)
    }

    @Test
    fun `남의 이미지 URL을 공고에 저장해도 그 이미지는 첨부하지 않는다`() {
        // given
        val others = upload("others-logo", OWNER_ID + 1)

        // when
        manager.attachImageUrls(OWNER_ID, listOf(others.url))
        repository.flush()

        // then
        assertEquals(ImageAssetStatus.TEMPORARY, repository.findById(others.id).orElseThrow().status)
    }

    private fun upload(id: String, ownerUserId: Long): ImageAsset = repository.save(
        ImageAsset.uploading(
            id = id,
            ownerUserId = ownerUserId,
            storageKey = "ogonggo/etc/$id.png",
            url = "https://bucket.example.com/ogonggo/etc/$id.png",
            mimeType = "image/png",
            size = 3L,
        ).also { it.markUploaded() },
    )

    companion object {
        private const val OWNER_ID = 81000L
    }
}
