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
import java.time.LocalDateTime
import java.util.UUID

@Component
class ImageAssetManager internal constructor(
    private val imageAssetRepository: ImageAssetJpaRepository,
    private val s3ObjectClient: S3ObjectClient,
    private val objectMapper: ObjectMapper,
) {

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

    fun markUploaded(id: String) {
        val asset = imageAssetRepository.findById(id).orElseThrow {
            EntityNotFoundException(ImageUploadErrorCode.IMAGE_ASSET_NOT_FOUND)
        }
        asset.markUploaded()
        imageAssetRepository.save(asset)
    }

    /**
     * 본문(Lexical JSON)의 이미지 노드(`imageId`·`src`)를 기준으로 이미지 상태를 맞춘다.
     * 이번에 새로 들어온 이미지는 첨부하고, 본문에서 빠진 이미지는 참조를 풀어 보존 기간이 지나면 정리되게 한다.
     * 이전 본문에 있던 이미지만 이미 첨부된 상태여도 받으므로, 다른 글이나 프로필에서 쓰는 이미지는 넣을 수 없다.
     */
    fun syncContentImages(
        ownerUserId: Long,
        previousContent: String?,
        currentContent: String?,
        now: LocalDateTime,
    ) {
        val previousIds = extractImageReferences(previousContent).keys
        val currentReferences = extractImageReferences(currentContent)
        val addedIds = currentReferences.keys - previousIds
        val currentAssets = readOwnedAssets(ownerUserId, currentReferences.keys)
        if (
            currentAssets.size != currentReferences.size ||
            currentAssets.any { currentReferences[it.id] != it.url } ||
            currentAssets.any { it.id in addedIds && !it.isAttachable() }
        ) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE)
        }

        val attached = currentAssets.filter { it.isAttachable() }.onEach { it.attach() }
        imageAssetRepository.saveAll(attached)
        unreference(ownerUserId, previousIds - currentReferences.keys, now)
    }

    /** 지운 글의 본문 이미지 참조를 풀어 보존 기간이 지나면 정리되게 한다. */
    fun unreferenceContentImages(ownerUserId: Long, content: String?, now: LocalDateTime) {
        unreference(ownerUserId, extractImageReferences(content).keys, now)
    }

    /** 본문 이미지를 새 저장 키와 새 이미지로 복제하고, 본문의 이미지 참조를 복제본으로 바꿔 돌려준다. */
    fun copyContentImages(ownerUserId: Long, content: String?): String? {
        val references = extractImageReferences(content)
        if (references.isEmpty()) return content

        val sourceAssets = readOwnedAssets(ownerUserId, references.keys).associateBy(ImageAsset::id)
        if (
            sourceAssets.size != references.size ||
            sourceAssets.values.any { it.status != ImageAssetStatus.ATTACHED || references[it.id] != it.url }
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
                // 원본과 같은 폴더(용도)에 둔다.
                val targetKey = "${source.storageKey.substringBeforeLast('/')}/$targetId.$extension"
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
                    it.attach()
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

    /**
     * 이미지 한 장을 쓰는 자리(회원 프로필 이미지, 기업 로고)의 이미지를 바꾸고 새 이미지 URL을 돌려준다.
     * 새 이미지는 첨부하고 이전 이미지는 참조를 푼다. 같은 이미지를 다시 보내면 그대로 둔다.
     * 남의 이미지, 다른 곳에서 쓰는 이미지, 지워진 이미지는 쓸 수 없다.
     */
    fun replaceImage(ownerUserId: Long, previousImageId: String?, imageId: String, now: LocalDateTime): String {
        val asset = imageAssetRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(imageId, ownerUserId)
            ?: throw InvalidValueException(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE)
        if (imageId == previousImageId) return asset.url
        if (!asset.isAttachable()) {
            throw InvalidValueException(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE)
        }

        asset.attach()
        imageAssetRepository.save(asset)
        previousImageId?.let { unreference(ownerUserId, setOf(it), now) }
        return asset.url
    }

    /** 더 쓰지 않는 이미지의 참조를 풀어 보존 기간이 지나면 정리되게 한다. */
    fun unreferenceImage(ownerUserId: Long, imageId: String, now: LocalDateTime) {
        unreference(ownerUserId, setOf(imageId), now)
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

    private fun readOwnedAssets(ownerUserId: Long, ids: Set<String>): List<ImageAsset> =
        if (ids.isEmpty()) {
            emptyList()
        } else {
            imageAssetRepository.findAllByIdInAndOwnerUserIdAndDeletedAtIsNull(ids, ownerUserId)
        }

    private fun unreference(ownerUserId: Long, ids: Set<String>, now: LocalDateTime) {
        val assets = readOwnedAssets(ownerUserId, ids).onEach { it.unreference(now) }
        imageAssetRepository.saveAll(assets)
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
