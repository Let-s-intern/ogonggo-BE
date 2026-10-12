package com.ogonggo.core.recruitmentpost.implement

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
}
