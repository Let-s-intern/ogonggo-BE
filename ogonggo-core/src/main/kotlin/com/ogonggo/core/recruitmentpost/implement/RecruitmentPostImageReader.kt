package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostImage
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostImageDto
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostImageJpaRepository
import org.springframework.stereotype.Component

@Component
class RecruitmentPostImageReader internal constructor(
    private val recruitmentPostImageRepository: RecruitmentPostImageJpaRepository,
) {

    /** 모집글의 첨부 사진을 노출 순서대로 읽는다. */
    fun read(postId: Long): List<RecruitmentPostImageDto> =
        recruitmentPostImageRepository.findAllByPostIdAndDeletedAtIsNullOrderByDisplayOrderAsc(postId)
            .map { RecruitmentPostImageDto(imageId = it.imageId, url = it.imageUrl) }

    /** 목록의 모집글들의 썸네일, 즉 첫 번째 첨부 사진 URL을 한 번에 읽는다. 사진이 없는 모집글은 빠진다. */
    fun readThumbnailUrls(postIds: Collection<Long>): Map<Long, String> {
        if (postIds.isEmpty()) return emptyMap()
        return recruitmentPostImageRepository.findAllByPostIdInAndDeletedAtIsNull(postIds)
            .groupBy(RecruitmentPostImage::postId)
            .mapValues { (_, images) -> images.minBy(RecruitmentPostImage::displayOrder).imageUrl }
    }
}
