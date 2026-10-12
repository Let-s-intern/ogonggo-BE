package com.ogonggo.core.image.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.image.domain.ImageAsset
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.image.persistence.ImageAssetJpaRepository
import org.springframework.stereotype.Component

@Component
class ImageAssetReader internal constructor(
    private val imageAssetRepository: ImageAssetJpaRepository,
) {

    /** 내가 올린, 아직 지워지지 않은 이미지를 읽는다. 남의 이미지는 없는 것과 같다. */
    fun readOwned(ownerUserId: Long, imageId: String): ImageAsset =
        imageAssetRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(imageId, ownerUserId)
            ?: throw EntityNotFoundException(ImageUploadErrorCode.IMAGE_ASSET_NOT_FOUND)
}
