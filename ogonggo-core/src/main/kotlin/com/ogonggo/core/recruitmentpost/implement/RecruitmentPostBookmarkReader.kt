package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.paging.validatePageRequest
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostBookmarkSearchCondition
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostBookmarkJpaRepository
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostQueryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostBookmarkItemDto
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostBookmarkPageDto

@Component
class RecruitmentPostBookmarkReader internal constructor(
    private val bookmarkRepository: RecruitmentPostBookmarkJpaRepository,
    private val postQueryRepository: RecruitmentPostQueryRepository,
) {

    /** 정렬은 조회 쿼리가 북마크 정렬 기준으로 정하므로 Pageable에는 페이지 범위만 넘긴다. */
    fun readBookmarkedPublishedPage(
        userId: Long,
        page: Int,
        size: Int,
        condition: RecruitmentPostBookmarkSearchCondition = RecruitmentPostBookmarkSearchCondition.NONE,
    ): RecruitmentPostBookmarkPageDto {
        validatePageRequest(page, size)
        val bookmarkedPosts = postQueryRepository.findBookmarkedPublishedPage(
            userId = userId,
            condition = condition,
            pageable = PageRequest.of(page, size),
        )
        return RecruitmentPostBookmarkPageDto(
            items = bookmarkedPosts.content.map(::RecruitmentPostBookmarkItemDto),
            page = bookmarkedPosts.number,
            size = bookmarkedPosts.size,
            totalElements = bookmarkedPosts.totalElements,
            totalPages = bookmarkedPosts.totalPages,
        )
    }

    fun readBookmarkedPostIds(userId: Long, postIds: Collection<Long>): Set<Long> =
        if (postIds.isEmpty()) emptySet() else bookmarkRepository.findActivePostIds(userId, postIds)

    fun isBookmarked(userId: Long, postId: Long): Boolean = readBookmarkedPostIds(userId, listOf(postId)).isNotEmpty()
}
