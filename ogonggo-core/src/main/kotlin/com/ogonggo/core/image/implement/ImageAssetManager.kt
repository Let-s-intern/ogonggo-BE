package com.ogonggo.core.image.implement

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.domain.ImageAsset
import com.ogonggo.core.image.domain.ImageAssetStatus
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.persistence.ImageAssetJpaRepository
import com.ogonggo.core.storage.s3.S3ObjectClient
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Component
class ImageAssetManager internal constructor(
    private val imageAssetRepository: ImageAssetJpaRepository,
    private val s3ObjectClient: S3ObjectClient,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun startUploading(
        id: String,
        ownerUserId: Long,
        storageKey: String,
        url: String,
        mimeType: String,
        size: Long,
    ) {
        imageAssetRepository.save(
            ImageAsset.uploading(id, ownerUserId, storageKey, url, mimeType, size),
        )
    }

    @Transactional
    fun markUploaded(id: String) {
        val asset = imageAssetRepository.findById(id).orElseThrow {
            EntityNotFoundException(ImageUploadErrorCode.IMAGE_ASSET_NOT_FOUND)
        }
        asset.markUploaded()
        imageAssetRepository.save(asset)
    }

    @Transactional
    fun syncPostImages(
        ownerUserId: Long,
        postId: Long,
        previousContent: String?,
        currentContent: String?,
        now: LocalDateTime,
    ) {
        val previousIds = extractImageReferences(previousContent).keys
        val currentReferences = extractImageReferences(currentContent)
        val currentIds = currentReferences.keys
        val currentAssets = loadAndValidate(ownerUserId, postId, currentIds)
        if (currentAssets.any { currentReferences[it.id] != it.url }) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE)
        }

        currentAssets.forEach { it.attach(postId) }
        val removedIds = previousIds - currentIds
        if (removedIds.isNotEmpty()) {
            imageAssetRepository
                .findAllByIdInAndOwnerUserIdAndDeletedAtIsNull(removedIds, ownerUserId)
                .filter { it.postId == postId }
                .forEach { it.unreference(now) }
        }
        imageAssetRepository.saveAll(currentAssets)
    }

    @Transactional
    fun copyPostImages(
        ownerUserId: Long,
        sourcePostId: Long,
        targetPostId: Long,
        content: String?,
    ): String? {
        val references = extractImageReferences(content)
        if (references.isEmpty()) return content

        val sourceAssets = imageAssetRepository
            .findAllByIdInAndOwnerUserIdAndDeletedAtIsNull(references.keys, ownerUserId)
            .associateBy(ImageAsset::id)
        if (
            sourceAssets.size != references.size ||
            sourceAssets.values.any {
                it.status != ImageAssetStatus.ATTACHED ||
                    it.postId != sourcePostId ||
                    references[it.id] != it.url
            }
        ) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE)
        }

        val copiedKeys = mutableListOf<String>()
        val replacements = mutableMapOf<String, ImageReference>()
        try {
            references.keys.forEach { sourceId ->
                val source = checkNotNull(sourceAssets[sourceId])
                val targetId = UUID.randomUUID().toString()
                val extension = source.storageKey.substringAfterLast('.', "bin")
                val targetKey = "images/$targetId.$extension"
                val targetUrl = s3ObjectClient.urlOf(targetKey)

                s3ObjectClient.copy(source.storageKey, targetKey, source.mimeType)
                copiedKeys += targetKey

                val targetAsset = ImageAsset.uploading(
                    id = targetId,
                    ownerUserId = ownerUserId,
                    storageKey = targetKey,
                    url = targetUrl,
                    mimeType = source.mimeType,
                    size = source.size,
                ).also {
                    it.markUploaded()
                    it.attach(targetPostId)
                }
                imageAssetRepository.save(targetAsset)
                replacements[sourceId] = ImageReference(targetId, targetUrl)
            }
            return replaceImageReferences(content, replacements)
        } catch (exception: Exception) {
            copiedKeys.asReversed().forEach { key -> runCatching { s3ObjectClient.delete(key) } }
            throw exception
        }
    }

    @Transactional
    fun unreferencePostImages(postId: Long, now: LocalDateTime) {
        val assets = imageAssetRepository.findAllByPostIdAndStatusAndDeletedAtIsNull(
            postId = postId,
            status = ImageAssetStatus.ATTACHED,
        )
        assets.forEach { it.unreference(now) }
        imageAssetRepository.saveAll(assets)
    }

    /**
     * 사용자가 올린 이미지를 프로필 이미지(일반 회원 프로필 이미지, 기업 로고)로 연결하고 URL을 돌려준다.
     * 남의 이미지, 게시글에 쓰는 이미지, 아직 올리는 중이거나 정리된 이미지는 쓸 수 없다.
     */
    @Transactional
    fun attachProfileImage(ownerUserId: Long, imageId: String): String {
        val asset = imageAssetRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(imageId, ownerUserId)
        if (asset == null || !asset.isAttachableToProfile()) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE)
        }
        asset.attachToProfile()
        imageAssetRepository.save(asset)
        return asset.url
    }

    /** 더 쓰지 않는 프로필 이미지의 참조를 풀어 보존 기간이 지나면 정리되게 한다. */
    @Transactional
    fun unreferenceProfileImage(ownerUserId: Long, imageId: String, now: LocalDateTime) {
        val asset = imageAssetRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(imageId, ownerUserId) ?: return
        if (asset.isAttachedToProfile()) {
            asset.unreference(now)
            imageAssetRepository.save(asset)
        }
    }

    fun cleanup(now: LocalDateTime, retention: java.time.Duration, batchSize: Int = 100): Int {
        val cutoff = now.minus(retention)
        val candidates = linkedSetOf<ImageAsset>().apply {
            addAll(
                imageAssetRepository.findAllByStatusInAndDeletedAtIsNullAndCreatedAtBefore(
                    statuses = setOf(ImageAssetStatus.UPLOADING, ImageAssetStatus.TEMPORARY),
                    createdAt = cutoff,
                    pageable = PageRequest.of(0, batchSize),
                ),
            )
            addAll(
                imageAssetRepository.findAllByStatusAndDeletedAtIsNullAndUnreferencedAtBefore(
                    status = ImageAssetStatus.UNREFERENCED,
                    unreferencedAt = cutoff,
                    pageable = PageRequest.of(0, batchSize),
                ),
            )
            addAll(
                imageAssetRepository.findAllByStatusAndDeletedAtIsNull(
                    status = ImageAssetStatus.DELETE_PENDING,
                    pageable = PageRequest.of(0, batchSize),
                ),
            )
        }

        var deletedCount = 0
        candidates.forEach { asset ->
            asset.markDeletePending()
            imageAssetRepository.save(asset)
            try {
                s3ObjectClient.delete(asset.storageKey)
                asset.markDeleted(now)
                imageAssetRepository.save(asset)
                deletedCount++
            } catch (_: Exception) {
                // DELETE_PENDING 상태를 남겨 다음 실행에서 재시도한다.
            }
        }
        return deletedCount
    }

    private fun loadAndValidate(ownerUserId: Long, postId: Long, ids: Set<String>): List<ImageAsset> {
        if (ids.isEmpty()) return emptyList()

        val assets = imageAssetRepository
            .findAllByIdInAndOwnerUserIdAndDeletedAtIsNull(ids, ownerUserId)
            .associateBy(ImageAsset::id)
        if (
            assets.size != ids.size ||
            assets.values.any {
                it.status !in setOf(ImageAssetStatus.TEMPORARY, ImageAssetStatus.UNREFERENCED) &&
                    !(it.status == ImageAssetStatus.ATTACHED && it.postId == postId)
            }
        ) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE)
        }
        return ids.map { id -> checkNotNull(assets[id]) }
    }

    private fun extractImageReferences(content: String?): Map<String, String> {
        if (content.isNullOrBlank()) return emptyMap()
        val root = try {
            objectMapper.readTree(content)
        } catch (_: Exception) {
            return emptyMap()
        }
        return buildMap {
            collectImageReferences(root, this)
        }
    }

    private fun collectImageReferences(node: JsonNode, references: MutableMap<String, String>) {
        if (node.isObject && node.path("type").asText() == "image") {
            val imageId = node.path("imageId").asText(null)?.takeIf(String::isNotBlank)
            val src = node.path("src").asText(null)?.takeIf(String::isNotBlank)
            if (imageId != null) {
                references[imageId] = src.orEmpty()
            }
        }
        node.elements().forEachRemaining { child -> collectImageReferences(child, references) }
    }

    private fun replaceImageReferences(
        content: String?,
        replacements: Map<String, ImageReference>,
    ): String? {
        if (content.isNullOrBlank() || replacements.isEmpty()) return content
        val root = objectMapper.readTree(content)
        replaceImageReferences(root, replacements)
        return objectMapper.writeValueAsString(root)
    }

    private fun replaceImageReferences(
        node: JsonNode,
        replacements: Map<String, ImageReference>,
    ) {
        if (node.isObject && node.path("type").asText() == "image") {
            val imageId = node.path("imageId").asText(null)
            replacements[imageId]?.let { replacement ->
                val objectNode = node as ObjectNode
                objectNode.put("imageId", replacement.id)
                objectNode.put("src", replacement.url)
            }
        }
        node.elements().forEachRemaining { child -> replaceImageReferences(child, replacements) }
    }

    private data class ImageReference(
        val id: String,
        val url: String,
    )
}
