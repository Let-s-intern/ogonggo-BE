package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.persistence.RecruitmentPostBookmarkJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RecruitmentPostBookmarkReader internal constructor(
    private val bookmarkRepository: RecruitmentPostBookmarkJpaRepository,
) {

    fun readBookmarkedPublishedCursorPage(
        userId: Long,
        cursor: RecruitmentPostBookmarkCursor?,
        size: Int,
    ): RecruitmentPostBookmarkCursorPage {
        validateBookmarkCursorPageSize(size)
        val bookmarkedPosts = bookmarkRepository.findBookmarkedPublishedCursorPage(
            userId = userId,
            publicationStatus = PublicationStatus.PUBLISHED,
            cursorUpdatedAt = cursor?.updatedAt,
            cursorId = cursor?.id,
            pageable = PageRequest.of(0, size + 1),
        )
        return RecruitmentPostBookmarkCursorPage(
            items = bookmarkedPosts.take(size).map { bookmarkedPost ->
                RecruitmentPostBookmarkItem(
                    post = bookmarkedPost.post,
                    cursor = RecruitmentPostBookmarkCursor(
                        updatedAt = bookmarkedPost.updatedAt,
                        id = bookmarkedPost.bookmarkId,
                    ),
                )
            },
            hasNext = bookmarkedPosts.size > size,
        )
    }

    fun readBookmarkedPostIds(userId: Long, postIds: Collection<Long>): Set<Long> =
        if (postIds.isEmpty()) emptySet() else bookmarkRepository.findActivePostIds(userId, postIds)
}

data class RecruitmentPostBookmarkCursor(
    val updatedAt: LocalDateTime,
    val id: Long,
)

data class RecruitmentPostBookmarkItem(
    val post: com.ogonggo.core.community.domain.RecruitmentPost,
    val cursor: RecruitmentPostBookmarkCursor,
)

data class RecruitmentPostBookmarkCursorPage(
    val items: List<RecruitmentPostBookmarkItem>,
    val hasNext: Boolean,
)

private fun validateBookmarkCursorPageSize(size: Int) {
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
