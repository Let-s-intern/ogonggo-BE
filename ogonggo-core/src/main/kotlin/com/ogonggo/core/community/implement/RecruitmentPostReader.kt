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

    fun readPublishedCursorPage(
        cursor: RecruitmentPostCursor?,
        size: Int,
        filter: RecruitmentPostListFilter,
        sortType: RecruitmentPostSortType,
    ): RecruitmentPostCursorPage {
        validatePageRequest(size)
        require(cursor == null || cursor.sortType == sortType) {
            "모집글 커서의 정렬 기준이 현재 요청과 다릅니다."
        }
        require(cursor == null || cursor.queryKey == filter.cursorKey(sortType)) {
            "모집글 커서의 조회 조건이 현재 요청과 다릅니다."
        }

        val posts = postQueryRepository.findPublishedCursorPage(cursor, size, filter, sortType)
        return RecruitmentPostCursorPage(
            posts = posts.take(size),
            hasNext = posts.size > size,
            sortType = sortType,
        )
    }
}

data class RecruitmentPostListFilter(
    val recruitmentTypes: Set<RecruitmentType> = emptySet(),
    val progressMethods: Set<ProgressMethod> = emptySet(),
    val recruitmentStatuses: Set<RecruitmentStatus> = emptySet(),
    val positions: Set<RecruitmentPosition> = emptySet(),
)

data class RecruitmentPostCursorPage(
    val posts: List<RecruitmentPost>,
    val hasNext: Boolean,
    val sortType: RecruitmentPostSortType,
)

private fun validatePageRequest(size: Int) {
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}

fun RecruitmentPostListFilter.cursorKey(sortType: RecruitmentPostSortType): String = buildString {
    append(sortType.name)
    append("|types=").append(recruitmentTypes.map { it.name }.sorted().joinToString(","))
    append("|progress=").append(progressMethods.map { it.name }.sorted().joinToString(","))
    append("|statuses=").append(recruitmentStatuses.map { it.name }.sorted().joinToString(","))
    append("|positions=").append(positions.map { it.name }.sorted().joinToString(","))
}
