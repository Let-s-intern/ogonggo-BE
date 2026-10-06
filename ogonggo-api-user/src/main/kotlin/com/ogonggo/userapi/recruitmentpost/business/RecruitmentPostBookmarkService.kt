package com.ogonggo.userapi.recruitmentpost.business

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationProgressStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostBookmarkSearchCondition
import com.ogonggo.core.recruitmentpost.error.RecruitmentPostApplicationErrorCode
import com.ogonggo.core.recruitmentpost.error.RecruitmentPostErrorCode
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostApplicationManager
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostApplicationReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostBookmarkManager
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostBookmarkReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostMetricReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostReader
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostMetricDto
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.user.implement.requireActive
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecruitmentPostBookmarkService(
    private val userReader: UserReader,
    private val postReader: RecruitmentPostReader,
    private val postBookmarkReader: RecruitmentPostBookmarkReader,
    private val postBookmarkManager: RecruitmentPostBookmarkManager,
    private val eventPublisher: ApplicationEventPublisher,
    private val recruitmentPostMetricReader: RecruitmentPostMetricReader,
    private val clock: Clock,
    private val userProfileReader: UserProfileReader,
    private val applicationReader: RecruitmentPostApplicationReader,
    private val applicationManager: RecruitmentPostApplicationManager,
) {

    @Transactional(readOnly = true)
    fun getBookmarks(
        userId: Long,
        page: Int,
        size: Int,
        condition: RecruitmentPostBookmarkSearchCondition = RecruitmentPostBookmarkSearchCondition.NONE,
    ): RecruitmentPostBookmarkPageResult {
        verifyActiveUser(userId)
        val result = postBookmarkReader.readBookmarkedPublishedPage(userId, page, size, condition)
        val postIds = result.items.map { item -> checkNotNull(item.post.id) { "조회된 모집글 식별자가 없습니다." } }
        val authorIds = result.items.map { it.post.authorUserId }.distinct()
        val authorsByUserId = userProfileReader.readAll(authorIds).mapValues { (authorId, profile) ->
            RecruitmentPostAuthorResult.from(authorId, profile)
        }
        val metrics = recruitmentPostMetricReader.readAll(postIds)
        val applicationCounts = applicationReader.countByPostIds(postIds)
        return RecruitmentPostBookmarkPageResult(
            items = result.items.map { item ->
                val post = item.post
                val postId = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." }
                RecruitmentPostSummary.from(
                    post = post,
                    metric = metrics[postId] ?: RecruitmentPostMetricDto.EMPTY,
                    author = authorsByUserId[post.authorUserId]
                        ?: RecruitmentPostAuthorResult.from(post.authorUserId, null),
                    bookmarked = true,
                    applicationCount = applicationCounts[postId] ?: 0L,
                )
            },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional
    fun addBookmark(userId: Long, postId: Long) {
        verifyActiveUser(userId)
        postReader.readPublished(postId)
        val now = LocalDateTime.now(clock)
        if (postBookmarkManager.append(userId, postId, now)) {
            eventPublisher.publishEvent(RecruitmentPostBookmarkChangedEvent(postId))
        }
    }

    @Transactional
    fun deleteBookmark(userId: Long, postId: Long) {
        verifyActiveUser(userId)
        postReader.readIncludingDeleted(postId)
        val now = LocalDateTime.now(clock)
        if (postBookmarkManager.delete(userId, postId, now)) {
            eventPublisher.publishEvent(RecruitmentPostBookmarkChangedEvent(postId))
        }
    }

    /**
     * 스크랩 칸의 모집글을 지원 준비 중 칸으로 옮긴다. 두 칸은 북마크와 지원 이력으로 나뉘어 있어 북마크를 해제하고 지원 이력을 만든다.
     * 연락처를 열어 이미 지원 준비 중 이력이 있으면 북마크만 정리하므로 여러 번 옮겨도 결과가 같다.
     */
    @Transactional
    fun prepare(userId: Long, postId: Long) {
        verifyActiveUser(userId)
        postReader.readPublished(postId)
        val now = LocalDateTime.now(clock)
        val applicationStatus = applicationReader.readActiveStatus(postId, userId)
        if (applicationStatus != null && applicationStatus != RecruitmentPostApplicationProgressStatus.PREPARING) {
            throw ConflictException(RecruitmentPostApplicationErrorCode.INVALID_RECRUITMENT_APPLICATION_STATUS_TRANSITION)
        }

        val unbookmarked = postBookmarkManager.delete(userId, postId, now)
        if (unbookmarked) {
            eventPublisher.publishEvent(RecruitmentPostBookmarkChangedEvent(postId))
        }
        if (applicationStatus == null) {
            if (!unbookmarked) {
                throw EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_BOOKMARK_NOT_FOUND)
            }
            applicationManager.startPreparation(postId, userId, now)
        }
    }

    /**
     * 지원 준비 중 칸의 모집글을 스크랩 칸으로 되돌린다. 지원 이력을 지우고 북마크가 없으면 다시 북마크한다.
     * 이미 스크랩 칸에만 있으면 그대로 끝나므로 여러 번 되돌려도 결과가 같다.
     */
    @Transactional
    fun cancelPreparation(userId: Long, postId: Long) {
        verifyActiveUser(userId)
        postReader.readPublished(postId)
        val now = LocalDateTime.now(clock)
        val bookmarked = postBookmarkReader.isBookmarked(userId, postId)
        when (applicationReader.readActiveStatus(postId, userId)) {
            null -> if (bookmarked) {
                return
            } else {
                throw EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_BOOKMARK_NOT_FOUND)
            }
            RecruitmentPostApplicationProgressStatus.PREPARING -> Unit
            else -> throw ConflictException(
                RecruitmentPostApplicationErrorCode.INVALID_RECRUITMENT_APPLICATION_STATUS_TRANSITION,
            )
        }

        applicationManager.delete(postId = postId, userId = userId, deletedAt = now)
        if (!bookmarked && postBookmarkManager.append(userId, postId, now)) {
            eventPublisher.publishEvent(RecruitmentPostBookmarkChangedEvent(postId))
        }
    }

    private fun verifyActiveUser(userId: Long) {
        userReader.read(userId).status.requireActive()
    }
}

data class RecruitmentPostBookmarkPageResult(
    val items: List<RecruitmentPostSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
