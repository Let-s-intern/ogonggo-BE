package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.community.persistence.RecruitmentPostJpaRepository
import com.ogonggo.core.community.persistence.RecruitmentPostQueryRepository
import org.springframework.stereotype.Component

@Component
class RecruitmentPostReader internal constructor(
    private val postRepository: RecruitmentPostJpaRepository,
    private val postQueryRepository: RecruitmentPostQueryRepository,
) {
    fun readPublished(postId: Long): RecruitmentPost =
        postRepository.findByIdAndPublicationStatusAndDeletedAtIsNull(postId, PublicationStatus.PUBLISHED)
            ?: throw EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND)

    fun readOwned(ownerUserId: Long, postId: Long): RecruitmentPost =
        postRepository.findOwnedByIdForUpdate(ownerUserId, postId)
            ?: throw EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND)

    fun readOwnedForDelete(ownerUserId: Long, postId: Long): RecruitmentPost =
        postRepository.findOwnedByIdForDelete(ownerUserId, postId)
            ?: throw EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND)

    fun readPublishedPage(
        page: Int,
        size: Int,
        filter: RecruitmentPostListFilter,
        sortType: RecruitmentPostSortType,
    ): RecruitmentPostPage {
        validatePageRequest(page, size)

        val result = postQueryRepository.findPublishedPage(page, size, filter, sortType)
        return RecruitmentPostPage(
            posts = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

data class RecruitmentPostListFilter(
    val recruitmentTypes: Set<RecruitmentType> = emptySet(),
    val progressMethods: Set<ProgressMethod> = emptySet(),
    val recruitmentStatuses: Set<RecruitmentStatus> = emptySet(),
    val positions: Set<RecruitmentPosition> = emptySet(),
)

data class RecruitmentPostPage(
    val posts: List<RecruitmentPost>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

private fun validatePageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
