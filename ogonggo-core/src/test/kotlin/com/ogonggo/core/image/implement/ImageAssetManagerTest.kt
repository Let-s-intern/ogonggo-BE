package com.ogonggo.core.image.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.domain.ImageAsset
import com.ogonggo.core.image.domain.ImageAssetStatus
import com.ogonggo.core.image.domain.ImageAssetUsage
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.persistence.ImageAssetJpaRepository
import com.ogonggo.core.image.persistence.ImageUrlReferenceQueryRepository
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
    private val references = Mockito.mock(ImageUrlReferenceQueryRepository::class.java)
    private val manager = ImageAssetManager(repository, storage, ObjectMapper(), references)
    private val now = LocalDateTime.of(2026, 9, 12, 10, 0)

    @Test
    fun `본문에 새로 넣은 이미지를 첨부한다`() {
        // given
        val asset = uploadedAsset()
        givenAssets(asset)

        // when
        manager.syncContentImages(17L, null, content(asset), now)

        // then
        assertEquals(ImageAssetStatus.ATTACHED, asset.status)
    }

    @Test
    fun `본문에서 빠진 이미지는 참조 해제하고 남은 이미지는 그대로 둔다`() {
        // given
        val kept = uploadedAsset().also { it.attach() }
        val removed = uploadedAsset("removed-image-id").also { it.attach() }
        givenAssets(kept, removed)

        // when
        manager.syncContentImages(17L, content(kept, removed), content(kept), now)

        // then
        assertEquals(ImageAssetStatus.ATTACHED, kept.status)
        assertEquals(ImageAssetStatus.UNREFERENCED, removed.status)
        assertEquals(now, removed.unreferencedAt)
    }

    @Test
    fun `다른 글이나 프로필에서 쓰는 이미지는 본문에 새로 넣을 수 없다`() {
        // given
        val usedElsewhere = uploadedAsset().also { it.attach() }
        givenAssets(usedElsewhere)

        // when
        val exception = assertThrows(InvalidValueException::class.java) {
            manager.syncContentImages(17L, plainContent(), content(usedElsewhere), now)
        }

        // then
        assertEquals(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE, exception.errorCode)
    }

    @Test
    fun `남의 이미지나 URL이 다른 이미지는 본문에 넣을 수 없다`() {
        // given
        val asset = uploadedAsset()
        givenAssets(asset)
        val wrongUrl = """{"root":{"children":[{"type":"image","imageId":"${asset.id}","src":"https://evil.example.com/a.png"}]}}"""

        // when & then
        listOf(18L to content(asset), 17L to wrongUrl).forEach { (ownerUserId, content) ->
            val exception = assertThrows(InvalidValueException::class.java) {
                manager.syncContentImages(ownerUserId, null, content, now)
            }
            assertEquals(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE, exception.errorCode)
        }
        assertEquals(ImageAssetStatus.TEMPORARY, asset.status)
    }

    @Test
    fun `첨부 목록을 바꾸면 새 이미지는 첨부하고 빠진 이미지는 참조를 풀고 순서대로 URL을 준다`() {
        // given
        val kept = uploadedAsset("kept").also { it.attach() }
        val removed = uploadedAsset("removed").also { it.attach() }
        val added = uploadedAsset("added")
        givenAssets(kept, removed, added)

        // when
        val result = manager.syncImages(17L, listOf("kept", "removed"), listOf("added", "kept"), ImageAssetUsage.CONCERN, now)

        // then
        assertEquals(listOf("added" to added.url, "kept" to kept.url), result.map { it.id to it.url })
        assertEquals(ImageAssetStatus.ATTACHED, added.status)
        assertEquals(ImageAssetStatus.ATTACHED, kept.status)
        assertEquals(ImageAssetStatus.UNREFERENCED, removed.status)
    }

    @Test
    fun `다른 곳에서 쓰는 이미지는 첨부 목록에 새로 넣을 수 없다`() {
        // given
        val usedElsewhere = uploadedAsset().also { it.attach() }
        givenAssets(usedElsewhere)

        // when
        val exception = assertThrows(InvalidValueException::class.java) {
            manager.syncImages(17L, emptyList(), listOf(usedElsewhere.id), ImageAssetUsage.CONCERN, now)
        }

        // then
        assertEquals(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE, exception.errorCode)
    }

    @Test
    fun `용도가 받지 않는 형식의 이미지는 첨부할 수 없다`() {
        // given
        val webp = ImageAsset.uploading("webp-id", 17L, "ogonggo/profile/webp-id.webp", "https://cdn.example.com/w.webp", "image/webp", 3L)
            .also { it.markUploaded() }
        givenAssets(webp)

        // when
        val exception = assertThrows(InvalidValueException::class.java) {
            manager.syncImages(17L, emptyList(), listOf(webp.id), ImageAssetUsage.CONCERN, now)
        }

        // then
        assertEquals(ImageUploadErrorCode.IMAGE_FILE_TYPE_NOT_SUPPORTED, exception.errorCode)
        assertEquals(ImageAssetStatus.TEMPORARY, webp.status)
    }

    @Test
    fun `첨부 이미지를 원본과 같은 폴더의 새 이미지로 순서대로 복제한다`() {
        // given
        val first = ImageAsset.uploading("first", 17L, "ogonggo/side/first.png", "https://cdn.example.com/ogonggo/side/first.png", "image/png", 3L)
            .also { it.markUploaded(); it.attach() }
        val second = ImageAsset.uploading("second", 17L, "ogonggo/side/second.jpg", "https://cdn.example.com/ogonggo/side/second.jpg", "image/jpeg", 3L)
            .also { it.markUploaded(); it.attach() }
        givenAssets(first, second)
        Mockito.`when`(storage.urlOf(Mockito.anyString())).thenAnswer { invocation -> "https://cdn.example.com/${invocation.arguments[0]}" }

        // when
        val copies = manager.copyImages(17L, listOf("second", "first"))

        // then
        assertEquals(2, copies.size)
        assertTrue(copies[0].url.matches(Regex("https://cdn.example.com/ogonggo/side/[0-9a-f-]{36}\\.jpg")))
        assertTrue(copies[1].url.matches(Regex("https://cdn.example.com/ogonggo/side/[0-9a-f-]{36}\\.png")))
        assertTrue(copies.none { it.id in setOf("first", "second") })
    }

    @Test
    fun `본문에 이미지 노드가 있는지 알려 준다`() {
        assertTrue(manager.containsImages(content(uploadedAsset())))
        assertTrue(!manager.containsImages(plainContent()))
        assertTrue(!manager.containsImages(null))
    }

    @Test
    fun `글을 지우면 본문 이미지의 참조를 푼다`() {
        // given
        val asset = uploadedAsset().also { it.attach() }
        givenAssets(asset)

        // when
        manager.unreferenceContentImages(17L, content(asset), now)

        // then
        assertEquals(ImageAssetStatus.UNREFERENCED, asset.status)
    }

    @Test
    fun `본문 이미지를 원본과 같은 폴더의 새 저장 키와 새 자산으로 복제하고 본문 참조를 치환한다`() {
        // given
        val asset = ImageAsset.uploading(
            id = "image-id",
            ownerUserId = 17L,
            storageKey = "ogonggo/side/image-id.png",
            url = "https://cdn.example.com/ogonggo/side/image-id.png",
            mimeType = "image/png",
            size = 3L,
        ).also {
            it.markUploaded()
            it.attach()
        }
        givenAssets(asset)
        Mockito.`when`(storage.urlOf(Mockito.anyString())).thenAnswer { invocation ->
            "https://cdn.example.com/${invocation.arguments[0]}"
        }

        // when
        val copiedContent = manager.copyContentImages(ownerUserId = 17L, content = content(asset))

        // then
        assertTrue(copiedContent.orEmpty().contains("https://cdn.example.com/ogonggo/side/"))
        assertTrue(!copiedContent.orEmpty().contains(asset.id))
        val copyInvocation = Mockito.mockingDetails(storage).invocations.single { it.method.name == "copy" }
        assertEquals(asset.storageKey, copyInvocation.arguments[0])
        assertTrue((copyInvocation.arguments[1] as String).matches(Regex("ogonggo/side/[0-9a-f-]{36}\\.png")))
        val saveInvocation = Mockito.mockingDetails(repository).invocations.single { it.method.name == "save" }
        assertEquals(ImageAssetStatus.ATTACHED, (saveInvocation.arguments[0] as ImageAsset).status)
        assertEquals(ImageAssetStatus.ATTACHED, asset.status)
    }

    @Test
    fun `프로필 이미지를 바꾸면 새 이미지를 첨부하고 전에 쓰던 이미지는 참조를 푼다`() {
        // given
        val previous = uploadedAsset("old-image").also { it.attach() }
        val next = uploadedAsset("new-image")
        givenAssets(previous, next)

        // when
        val url = manager.replaceImage(17L, previous.id, next.id, now)

        // then
        assertEquals(next.url, url)
        assertEquals(ImageAssetStatus.ATTACHED, next.status)
        assertEquals(ImageAssetStatus.UNREFERENCED, previous.status)
    }

    @Test
    fun `같은 프로필 이미지를 다시 보내면 그대로 둔다`() {
        // given
        val current = uploadedAsset().also { it.attach() }
        givenAssets(current)

        // when
        val url = manager.replaceImage(17L, current.id, current.id, now)

        // then
        assertEquals(current.url, url)
        assertEquals(ImageAssetStatus.ATTACHED, current.status)
    }

    @Test
    fun `다른 곳에서 쓰는 이미지나 남의 이미지는 프로필 이미지로 쓸 수 없다`() {
        // given
        val usedElsewhere = uploadedAsset().also { it.attach() }
        val previous = uploadedAsset("old-image").also { it.attach() }
        givenAssets(usedElsewhere, previous)

        // when & then
        listOf(17L, 18L).forEach { ownerUserId ->
            val exception = assertThrows(InvalidValueException::class.java) {
                manager.replaceImage(ownerUserId, previous.id, usedElsewhere.id, now)
            }
            assertEquals(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE, exception.errorCode)
        }
        assertEquals(ImageAssetStatus.ATTACHED, previous.status)
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

    @Test
    fun `공고에 저장한 URL 중 내가 올린 임시 이미지를 첨부한다`() {
        // given
        val logo = uploadedAsset("logo")
        val cover = uploadedAsset("cover")
        givenAssetsByUrl(logo, cover)

        // when
        manager.attachImageUrls(17L, listOf(logo.url, cover.url, null))

        // then
        assertEquals(ImageAssetStatus.ATTACHED, logo.status)
        assertEquals(ImageAssetStatus.ATTACHED, cover.status)
    }

    @Test
    fun `외부 URL이나 남의 이미지나 이미 첨부된 이미지가 있어도 막지 않고 그대로 둔다`() {
        // given
        val others = uploadedAsset("others")
        val alreadyAttached = uploadedAsset("attached").also { it.attach() }
        givenAssetsByUrl(others, alreadyAttached)

        // when
        manager.attachImageUrls(18L, listOf(others.url, "https://example.com/logo.png"))
        manager.attachImageUrls(17L, listOf(alreadyAttached.url))

        // then
        assertEquals(ImageAssetStatus.TEMPORARY, others.status)
        assertEquals(ImageAssetStatus.ATTACHED, alreadyAttached.status)
    }

    @Test
    fun `정리 작업이 이미 지운 이미지 URL을 공고에 저장하면 다시 올리도록 막는다`() {
        // given
        val deleted = deletedAsset("deleted")
        val pending = uploadedAsset("pending").also { it.markDeletePending() }
        val unconfirmed = ImageAsset.uploading("unconfirmed", 17L, "images/unconfirmed.png", "https://cdn.example.com/images/unconfirmed.png", "image/png", 3L)
        val fresh = uploadedAsset("fresh")
        givenAssetsByUrl(deleted, pending, unconfirmed, fresh)

        // when & then
        listOf(deleted, pending, unconfirmed).forEach { unavailable ->
            val exception = assertThrows(InvalidValueException::class.java) {
                manager.attachImageUrls(17L, listOf(fresh.url, unavailable.url))
            }
            assertEquals(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE, exception.errorCode)
        }
        assertEquals(ImageAssetStatus.TEMPORARY, fresh.status)
    }

    @Test
    fun `공고나 부트캠프가 URL로 쓰는 이전 로고는 기업 로고를 바꿔도 참조를 풀지 않는다`() {
        // given
        val previous = uploadedAsset("old-logo").also { it.attach() }
        val next = uploadedAsset("new-logo")
        givenAssets(previous, next)
        givenUsedByPostings(previous.url)

        // when
        manager.replaceImage(17L, previous.id, next.id, now)
        manager.unreferenceImage(17L, previous.id, now)

        // then
        assertEquals(ImageAssetStatus.ATTACHED, previous.status)
        assertEquals(null, previous.unreferencedAt)
    }

    @Test
    fun `정리 작업은 공고나 부트캠프가 URL로 쓰는 이미지를 지우지 않고 첨부로 되돌린다`() {
        // given
        val temporary = uploadedAsset("temporary-logo")
        val unreferenced = uploadedAsset("unreferenced-logo").also {
            it.attach()
            it.unreference(now.minusDays(3))
        }
        val pending = uploadedAsset("pending-logo").also { it.markDeletePending() }
        val orphan = uploadedAsset("orphan")
        givenCleanupCandidates(temporary = listOf(temporary, orphan), unreferenced = listOf(unreferenced), pending = listOf(pending))
        givenUsedByPostings(temporary.url, unreferenced.url, pending.url)

        // when
        val deletedCount = manager.cleanup(now, Duration.ofHours(24))

        // then
        assertEquals(1, deletedCount)
        listOf(temporary, unreferenced, pending).forEach {
            assertEquals(ImageAssetStatus.ATTACHED, it.status)
            Mockito.verify(storage, Mockito.never()).delete(it.storageKey)
        }
        assertEquals(ImageAssetStatus.DELETED, orphan.status)
    }

    /** URL로 거른 이미지를 소유자와 삭제 여부에 관계없이 돌려주는 저장소 대역이다. */
    private fun givenAssetsByUrl(vararg assets: ImageAsset) {
        Mockito.`when`(repository.findAllByUrlIn(Mockito.anyCollection()))
            .thenAnswer { invocation ->
                val urls = invocation.getArgument<Collection<String>>(0)
                assets.filter { it.url in urls }
            }
    }

    /** 채용공고·부트캠프의 로고·대표 이미지 칼럼이 [urls]를 쓴다. */
    private fun givenUsedByPostings(vararg urls: String) {
        Mockito.`when`(references.findUrlsUsedByPostings(Mockito.anyCollection()))
            .thenAnswer { invocation -> invocation.getArgument<Collection<String>>(0).filter { it in urls }.toSet() }
    }

    private fun givenCleanupCandidates(temporary: List<ImageAsset>, unreferenced: List<ImageAsset>, pending: List<ImageAsset>) {
        val page = org.springframework.data.domain.PageRequest.of(0, 100)
        Mockito.`when`(
            repository.findAllByStatusInAndDeletedAtIsNullAndCreatedAtBefore(
                setOf(ImageAssetStatus.UPLOADING, ImageAssetStatus.TEMPORARY),
                now.minusHours(24),
                page,
            ),
        ).thenReturn(temporary)
        Mockito.`when`(
            repository.findAllByStatusAndDeletedAtIsNullAndUnreferencedAtBefore(ImageAssetStatus.UNREFERENCED, now.minusHours(24), page),
        ).thenReturn(unreferenced)
        Mockito.`when`(repository.findAllByStatusAndDeletedAtIsNull(ImageAssetStatus.DELETE_PENDING, page)).thenReturn(pending)
    }

    private fun deletedAsset(id: String): ImageAsset = uploadedAsset(id).also {
        it.markDeletePending()
        it.markDeleted(now.minusDays(1))
    }

    /** 소유자와 식별자로 거른 이미지를 돌려주는 저장소 대역이다. */
    private fun givenAssets(vararg assets: ImageAsset) {
        Mockito.`when`(repository.findAllByIdInAndOwnerUserIdAndDeletedAtIsNull(Mockito.anyCollection(), Mockito.anyLong()))
            .thenAnswer { invocation ->
                val ids = invocation.getArgument<Collection<String>>(0)
                val ownerUserId = invocation.getArgument<Long>(1)
                assets.filter { it.id in ids && it.ownerUserId == ownerUserId }
            }
        Mockito.`when`(repository.findByIdAndOwnerUserIdAndDeletedAtIsNull(Mockito.anyString(), Mockito.anyLong()))
            .thenAnswer { invocation ->
                val id = invocation.getArgument<String>(0)
                val ownerUserId = invocation.getArgument<Long>(1)
                assets.firstOrNull { it.id == id && it.ownerUserId == ownerUserId }
            }
    }

    private fun uploadedAsset(id: String = "image-id"): ImageAsset = ImageAsset.uploading(
        id = id,
        ownerUserId = 17L,
        storageKey = "images/$id.png",
        url = "https://cdn.example.com/images/$id.png",
        mimeType = "image/png",
        size = 3L,
    ).also { it.markUploaded() }

    private fun content(vararg assets: ImageAsset): String =
        assets.joinToString(prefix = """{"root":{"children":[""", postfix = "]}}") {
            """{"type":"image","imageId":"${it.id}","src":"${it.url}"}"""
        }

    private fun plainContent(): String = """{"root":{"children":[]}}"""
}
