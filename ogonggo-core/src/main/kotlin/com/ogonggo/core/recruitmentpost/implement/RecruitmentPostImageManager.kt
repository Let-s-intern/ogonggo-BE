package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostImage
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostImageDto
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostImageJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RecruitmentPostImageManager internal constructor(
    private val recruitmentPostImageRepository: RecruitmentPostImageJpaRepository,
) {

    /**
     * 모집글의 첨부 사진을 [images] 순서대로 교체한다. 같은 자리에 남은 사진은 행을 그대로 두고, 나머지는 소프트 삭제한 뒤 새로 넣는다.
     * 이미지 첨부·참조 해제는 호출자가 `ImageAssetManager`로 맞춘다.
     */
    fun replace(postId: Long, images: List<RecruitmentPostImageDto>, now: LocalDateTime) {
        val current = recruitmentPostImageRepository.findAllByPostIdAndDeletedAtIsNullOrderByDisplayOrderAsc(postId)
        val kept = current.filter { image -> images.getOrNull(image.displayOrder)?.imageId == image.imageId }
        (current - kept.toSet()).forEach { it.delete(now) }
        val keptOrders = kept.map(RecruitmentPostImage::displayOrder).toSet()
        val added = images.withIndex()
            .filter { (order, _) -> order !in keptOrders }
            .map { (order, image) -> RecruitmentPostImage(postId, image.imageId, image.url, order) }
        recruitmentPostImageRepository.saveAll(current + added)
    }
}
