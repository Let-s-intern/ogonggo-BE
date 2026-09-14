package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.implement.RecruitmentPostAppendCommand
import com.ogonggo.core.community.implement.RecruitmentPostAppender
import com.ogonggo.core.community.implement.RecruitmentPostManager
import com.ogonggo.core.community.implement.PostMetricReader
import com.ogonggo.core.community.implement.RecruitmentPostReader
import com.ogonggo.core.community.implement.RecruitmentPostUpdateCommand
import com.ogonggo.core.editor.lexical.LexicalEditorStateValidator
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserReader
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class RecruitmentPostService(
    private val userReader: UserReader,
    private val postAppender: RecruitmentPostAppender,
    private val postManager: RecruitmentPostManager,
    private val postReader: RecruitmentPostReader,
    private val postMetricReader: PostMetricReader,
    private val contentValidator: LexicalEditorStateValidator,
    private val imageAssetManager: ImageAssetManager,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getRecruitmentPost(postId: Long): RecruitmentPostDetailResult {
        val result = RecruitmentPostDetailResult.from(
            post = postReader.readPublished(postId),
            metric = postMetricReader.read(postId),
        )
        eventPublisher.publishEvent(RecruitmentPostViewedEvent(result.id))
        return result
    }

    @Transactional(readOnly = true)
    fun getRecruitmentPosts(query: RecruitmentPostListQuery): RecruitmentPostPageResult =
        postReader.readPublishedPage(query.page, query.size, query.filter, query.sortType).let { page ->
            page.toResult(
                postMetricReader.readAll(page.posts.map { checkNotNull(it.id) { "조회된 모집글 식별자가 없습니다." } }),
            )
        }

    @Transactional
    fun create(userId: Long, command: RecruitmentPostAppendCommand): Long {
        verifyActiveUser(userId)
        val sanitizedCommand = command.copy(
            authorUserId = userId,
            content = contentValidator.validateAndSerialize(command.content),
        )
        val post = postAppender.append(sanitizedCommand)
        imageAssetManager.syncPostImages(
            ownerUserId = userId,
            postId = checkNotNull(post.id),
            previousContent = null,
            currentContent = sanitizedCommand.content,
            now = LocalDateTime.now(clock),
        )
        return checkNotNull(post.id) { "저장된 모집글 식별자가 없습니다." }
    }

    @Transactional
    fun update(userId: Long, postId: Long, command: RecruitmentPostUpdateCommand) {
        verifyActiveUser(userId)
        val post = postReader.readOwned(userId, postId)
        val previousContent = post.content
        val sanitizedCommand = command.copy(
            content = contentValidator.validateAndSerialize(command.content),
        )
        imageAssetManager.syncPostImages(
            ownerUserId = userId,
            postId = postId,
            previousContent = previousContent,
            currentContent = sanitizedCommand.content,
            now = LocalDateTime.now(clock),
        )
        postManager.update(post, sanitizedCommand)
    }

    @Transactional
    fun delete(userId: Long, postId: Long) {
        verifyActiveUser(userId)
        val now = LocalDateTime.now(clock)
        postManager.delete(postReader.readOwnedForDelete(userId, postId), now)
        imageAssetManager.unreferencePostImages(postId, now)
    }

    private fun verifyActiveUser(userId: Long) {
        when (userReader.read(userId).status) {
            UserStatus.ACTIVE -> Unit
            UserStatus.SUSPENDED -> throw ForbiddenException(UserErrorCode.USER_SUSPENDED)
            UserStatus.WITHDRAWN -> throw ForbiddenException(UserErrorCode.USER_WITHDRAWN)
        }
    }
}
