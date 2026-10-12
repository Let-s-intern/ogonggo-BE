package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.ConcernImage
import com.ogonggo.core.concern.implement.dto.ConcernImageDto
import com.ogonggo.core.concern.persistence.ConcernImageJpaRepository
import org.springframework.stereotype.Component

@Component
class ConcernImageReader internal constructor(
    private val concernImageRepository: ConcernImageJpaRepository,
) {

    /** 고민글의 첨부 사진을 노출 순서대로 읽는다. */
    fun read(concernId: Long): List<ConcernImageDto> =
        concernImageRepository.findAllByConcernIdAndDeletedAtIsNullOrderByDisplayOrderAsc(concernId)
            .map(ConcernImage::toDto)

    /** 목록의 고민글들의 첨부 사진을 한 번에 읽는다. 사진이 없는 고민글은 빈 목록이다. */
    fun readAll(concernIds: Collection<Long>): Map<Long, List<ConcernImageDto>> {
        if (concernIds.isEmpty()) return emptyMap()
        val images = concernImageRepository.findAllByConcernIdInAndDeletedAtIsNull(concernIds)
            .groupBy(ConcernImage::concernId)
        return concernIds.associateWith { concernId ->
            images[concernId].orEmpty().sortedBy(ConcernImage::displayOrder).map(ConcernImage::toDto)
        }
    }
}

private fun ConcernImage.toDto(): ConcernImageDto = ConcernImageDto(imageId = imageId, url = imageUrl)
