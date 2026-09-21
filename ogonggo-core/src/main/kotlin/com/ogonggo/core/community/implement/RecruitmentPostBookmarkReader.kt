package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPostBookmarkSearchCondition
import com.ogonggo.core.community.persistence.RecruitmentPostBookmarkJpaRepository
import com.ogonggo.core.community.persistence.RecruitmentPostQueryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

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
    ): RecruitmentPostBookmarkPage {
        validateBookmarkPageRequest(page, size)
        val bookmarkedPosts = postQueryRepository.findBookmarkedPublishedPage(
            userId = userId,
            condition = condition,
            pageable = PageRequest.of(page, size),
        )
        return RecruitmentPostBookmarkPage(
            items = bookmarkedPosts.content.map(::RecruitmentPostBookmarkItem),
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

data class RecruitmentPostBookmarkItem(
    val post: com.ogonggo.core.community.domain.RecruitmentPost,
)

data class RecruitmentPostBookmarkPage(
    val items: List<RecruitmentPostBookmarkItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

private fun validateBookmarkPageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
