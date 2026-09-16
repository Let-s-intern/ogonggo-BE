package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.persistence.RecruitmentPostBookmarkJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class RecruitmentPostBookmarkReader internal constructor(
    private val bookmarkRepository: RecruitmentPostBookmarkJpaRepository,
) {

    fun readBookmarkedPublishedPage(
        userId: Long,
        page: Int,
        size: Int,
    ): RecruitmentPostBookmarkPage {
        validateBookmarkPageRequest(page, size)
        val bookmarkedPosts = bookmarkRepository.findBookmarkedPublishedPage(
            userId = userId,
            publicationStatus = PublicationStatus.PUBLISHED,
            pageable = PageRequest.of(page, size),
        )
        return RecruitmentPostBookmarkPage(
            items = bookmarkedPosts.content.map { bookmarkedPost -> RecruitmentPostBookmarkItem(bookmarkedPost.post) },
            page = bookmarkedPosts.number,
            size = bookmarkedPosts.size,
            totalElements = bookmarkedPosts.totalElements,
            totalPages = bookmarkedPosts.totalPages,
        )
    }

    fun readBookmarkedPostIds(userId: Long, postIds: Collection<Long>): Set<Long> =
        if (postIds.isEmpty()) emptySet() else bookmarkRepository.findActivePostIds(userId, postIds)
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
