package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.implement.PostMetricDto
import com.ogonggo.core.community.implement.PostMetricReader
import com.ogonggo.core.community.implement.RecruitmentPostBookmarkManager
import com.ogonggo.core.community.implement.RecruitmentPostBookmarkReader
import com.ogonggo.core.community.implement.RecruitmentPostBookmarkCursor
import com.ogonggo.core.community.implement.RecruitmentPostReader
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class RecruitmentPostBookmarkService(
    private val userReader: UserReader,
    private val postReader: RecruitmentPostReader,
    private val postBookmarkReader: RecruitmentPostBookmarkReader,
    private val postBookmarkManager: RecruitmentPostBookmarkManager,
    private val postMetricReader: PostMetricReader,
    private val clock: Clock,
    private val userProfileReader: UserProfileReader,
) {

    @Transactional(readOnly = true)
    fun getBookmarks(
        userId: Long,
        cursor: RecruitmentPostBookmarkCursor?,
        size: Int,
    ): RecruitmentPostBookmarkCursorPageResult {
        verifyActiveUser(userId)
        val result = postBookmarkReader.readBookmarkedPublishedCursorPage(userId, cursor, size)
        val postIds = result.items.map { item -> checkNotNull(item.post.id) { "조회된 모집글 식별자가 없습니다." } }
        val authorIds = result.items.map { it.post.authorUserId }.distinct()
        val authorsByUserId = userProfileReader.readAll(authorIds).mapValues { (authorId, profile) ->
            RecruitmentPostAuthorResult.from(authorId, profile)
        }
        val metrics = postMetricReader.readAll(postIds)
        return RecruitmentPostBookmarkCursorPageResult(
            items = result.items.map { item ->
                val post = item.post
                val postId = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." }
                RecruitmentPostSummary.from(
                    post = post,
                    metric = metrics[postId] ?: PostMetricDto.EMPTY,
                    author = authorsByUserId[post.authorUserId]
                        ?: RecruitmentPostAuthorResult.from(post.authorUserId, null),
                    bookmarked = true,
                )
            },
            hasNext = result.hasNext,
            nextCursor = result.items.lastOrNull()?.cursor?.takeIf { result.hasNext },
        )
    }

    @Transactional
    fun addBookmark(userId: Long, postId: Long) {
        verifyActiveUser(userId)
        postReader.readPublished(postId)
        postBookmarkManager.append(userId, postId, LocalDateTime.now(clock))
    }

    @Transactional
    fun deleteBookmark(userId: Long, postId: Long) {
        verifyActiveUser(userId)
        postReader.readIncludingDeleted(postId)
        postBookmarkManager.delete(userId, postId, LocalDateTime.now(clock))
    }

    private fun verifyActiveUser(userId: Long) {
        when (userReader.read(userId).status) {
            UserStatus.ACTIVE -> Unit
            UserStatus.SUSPENDED -> throw ForbiddenException(UserErrorCode.USER_SUSPENDED)
            UserStatus.WITHDRAWN -> throw ForbiddenException(UserErrorCode.USER_WITHDRAWN)
        }
    }
}

data class RecruitmentPostBookmarkCursorPageResult(
    val items: List<RecruitmentPostSummary>,
    val hasNext: Boolean,
    val nextCursor: RecruitmentPostBookmarkCursor?,
)
