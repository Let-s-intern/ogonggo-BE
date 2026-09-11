package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.Post
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.community.persistence.PostJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component

interface PostReader {
    fun readPublished(postId: Long): Post

    fun readOwned(ownerUserId: Long, postId: Long): Post

    fun readOwnedForDelete(ownerUserId: Long, postId: Long): Post

    fun readPublishedPage(
        page: Int,
        size: Int,
        filter: RecruitmentPostListFilter,
        sortType: RecruitmentPostSortType,
    ): RecruitmentPostPage
}

@Component
internal class PostReaderImpl(
    private val postRepository: PostJpaRepository,
) : PostReader {

    override fun readPublished(postId: Long): Post =
        postRepository.findByIdAndPublicationStatusAndDeletedAtIsNull(postId, PublicationStatus.PUBLISHED)
            ?: throw EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND)

    override fun readOwned(ownerUserId: Long, postId: Long): Post =
        postRepository.findOwnedByIdForUpdate(ownerUserId, postId)
            ?: throw EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND)

    override fun readOwnedForDelete(ownerUserId: Long, postId: Long): Post =
        postRepository.findOwnedByIdForDelete(ownerUserId, postId)
            ?: throw EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND)

    override fun readPublishedPage(
        page: Int,
        size: Int,
        filter: RecruitmentPostListFilter,
        sortType: RecruitmentPostSortType,
    ): RecruitmentPostPage {
        validatePageRequest(page, size)

        val result = postRepository.findAll(
            publishedRecruitmentPosts(filter),
            PageRequest.of(page, size, sortType.toSort()),
        )
        return RecruitmentPostPage(
            posts = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    private fun RecruitmentPostSortType.toSort(): Sort = when (this) {
        RecruitmentPostSortType.LATEST -> Sort.by(Sort.Order.desc("id"))
        RecruitmentPostSortType.DEADLINE -> Sort.by(
            Sort.Order.asc("recruitmentEndDate"),
            Sort.Order.desc("id"),
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
    val posts: List<Post>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

private fun publishedRecruitmentPosts(filter: RecruitmentPostListFilter): Specification<Post> =
    Specification { root, query, criteriaBuilder ->
        val predicates = mutableListOf(
            criteriaBuilder.equal(root.get<PublicationStatus>("publicationStatus"), PublicationStatus.PUBLISHED),
            criteriaBuilder.isNull(root.get<Any>("deletedAt")),
        )

        if (filter.recruitmentTypes.isNotEmpty()) {
            predicates += root.get<RecruitmentType>("recruitmentType").`in`(filter.recruitmentTypes)
        }
        if (filter.progressMethods.isNotEmpty()) {
            predicates += root.get<ProgressMethod>("progressMethod").`in`(filter.progressMethods)
        }
        if (filter.recruitmentStatuses.isNotEmpty()) {
            predicates += root.get<RecruitmentStatus>("recruitmentStatus").`in`(filter.recruitmentStatuses)
        }
        if (filter.positions.isNotEmpty()) {
            query.distinct(true)
            val positionJoin = root.join<Post, RecruitmentPosition>("positions")
            predicates += positionJoin.`in`(filter.positions)
        }

        criteriaBuilder.and(*predicates.toTypedArray())
    }

private fun validatePageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
