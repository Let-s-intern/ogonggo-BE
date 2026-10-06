package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.paging.validatePageRequest
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostConsoleSearchCondition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostSortType
import com.ogonggo.core.recruitmentpost.error.RecruitmentPostErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostJpaRepository
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostQueryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostListFilterDto
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostPageDto

@Component
class RecruitmentPostReader internal constructor(
    private val postRepository: RecruitmentPostJpaRepository,
    private val postQueryRepository: RecruitmentPostQueryRepository,
) {
    fun readPublished(postId: Long): RecruitmentPost =
        postRepository.findByIdAndPublicationStatusAndDeletedAtIsNull(postId, RecruitmentPostPublicationStatus.PUBLISHED)
            ?: throw EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND)

    fun readPublishedForUpdate(postId: Long): RecruitmentPost =
        postRepository.findPublishedByIdForUpdate(
            postId = postId,
            publicationStatus = RecruitmentPostPublicationStatus.PUBLISHED,
        ) ?: throw EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND)

    fun readIncludingDeleted(postId: Long): RecruitmentPost =
        postRepository.findById(postId).orElseThrow {
            EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND)
        }

    fun readOwned(ownerUserId: Long, postId: Long): RecruitmentPost =
        postRepository.findOwnedById(ownerUserId, postId)
            ?: throw EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND)

    fun readOwnedForUpdate(ownerUserId: Long, postId: Long): RecruitmentPost =
        postRepository.findOwnedByIdForUpdate(ownerUserId, postId)
            ?: throw EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND)

    fun readOwnedForDelete(ownerUserId: Long, postId: Long): RecruitmentPost =
        postRepository.findOwnedByIdForDelete(ownerUserId, postId)
            ?: throw EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND)

    fun readPublishedPage(
        page: Int,
        size: Int,
        filter: RecruitmentPostListFilterDto,
        sortType: RecruitmentPostSortType,
    ): RecruitmentPostPageDto {
        validatePageRequest(page, size)
        val result = postQueryRepository.findPublishedPage(
            page = page,
            size = size,
            filter = filter,
            sortType = sortType,
        )
        return RecruitmentPostPageDto(
            posts = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    fun readConsolePage(
        condition: RecruitmentPostConsoleSearchCondition,
        sortType: RecruitmentPostSortType,
        page: Int,
        size: Int,
    ): RecruitmentPostPageDto {
        validatePageRequest(page, size)
        val result = postQueryRepository.findConsolePage(condition, sortType, PageRequest.of(page, size))
        return RecruitmentPostPageDto(
            posts = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    /** 게시된 적 있는 미삭제 모집글을 잠가 읽는다. 하나라도 없으면 없는 식별자를 모두 담아 실패한다. */
    fun readAllPostedForUpdate(postIds: Collection<Long>): List<RecruitmentPost> {
        val distinctIds = postIds.toSet()
        require(distinctIds.isNotEmpty()) { "잠글 모집글 식별자가 없습니다." }
        val posts = postRepository.findAllPostedByIdInForUpdate(distinctIds)
        val missingIds = distinctIds - posts.mapNotNullTo(HashSet()) { it.id }
        if (missingIds.isNotEmpty()) {
            throw EntityNotFoundException(
                RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND,
                "${RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND.message} (id: ${missingIds.sorted().joinToString()})",
            )
        }
        return posts
    }
}
