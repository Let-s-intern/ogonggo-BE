package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.implement.PostAppendCommand
import com.ogonggo.core.community.implement.PostAppender
import com.ogonggo.core.community.implement.PostManager
import com.ogonggo.core.community.implement.PostReader
import com.ogonggo.core.community.implement.PostUpdateCommand
import com.ogonggo.core.editor.lexical.LexicalEditorStateValidator
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class RecruitmentPostService(
    private val userReader: UserReader,
    private val postAppender: PostAppender,
    private val postManager: PostManager,
    private val postReader: PostReader,
    private val contentValidator: LexicalEditorStateValidator,
    private val imageAssetManager: ImageAssetManager,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getRecruitmentPost(postId: Long): RecruitmentPostDetailResult =
        RecruitmentPostDetailResult.from(postReader.readPublished(postId))

    @Transactional(readOnly = true)
    fun getRecruitmentPosts(query: RecruitmentPostListQuery): RecruitmentPostPageResult =
        postReader.readPublishedPage(query.page, query.size, query.filter, query.sortType).toResult()

    @Transactional
    fun create(userId: Long, command: PostAppendCommand): Long {
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
    fun update(userId: Long, postId: Long, command: PostUpdateCommand) {
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
