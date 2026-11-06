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

    /**
     * 오공고에 올렸고 아직 지우지 않은 이미지의 URL인가. 누가 올렸는지와 첨부 여부는 보지 않는다.
     * 이미지를 URL로만 저장하는 쪽(관리자 배너)이 외부 주소나 정리 작업이 지운 이미지를 막을 때 쓴다.
     */
    fun isAvailableUrl(url: String): Boolean = imageAssetRepository.findAllByUrlIn(setOf(url)).any(ImageAsset::isAvailable)
}
