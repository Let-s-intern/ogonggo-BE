package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.ConcernImage
import com.ogonggo.core.concern.implement.dto.ConcernImageDto
import com.ogonggo.core.concern.persistence.ConcernImageJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ConcernImageManager internal constructor(
    private val concernImageRepository: ConcernImageJpaRepository,
) {

    /**
     * 고민글의 첨부 사진을 [images] 순서대로 교체한다. 남겨 둔 사진은 행을 그대로 두고, 빠진 사진은 소프트 삭제한다.
     * 순서가 바뀌면 그 사진도 새 행으로 바꾼다. 이미지 첨부·참조 해제는 호출자가 `ImageAssetManager`로 맞춘다.
     */
    fun replace(concernId: Long, images: List<ConcernImageDto>, now: LocalDateTime) {
        val current = concernImageRepository.findAllByConcernIdAndDeletedAtIsNullOrderByDisplayOrderAsc(concernId)
        val kept = current.filter { image -> images.getOrNull(image.displayOrder)?.imageId == image.imageId }
        (current - kept.toSet()).forEach { it.delete(now) }
        val keptOrders = kept.map(ConcernImage::displayOrder).toSet()
        val added = images.withIndex()
            .filter { (order, _) -> order !in keptOrders }
            .map { (order, image) -> ConcernImage(concernId, image.imageId, image.url, order) }
        concernImageRepository.saveAll(current + added)
    }
}
