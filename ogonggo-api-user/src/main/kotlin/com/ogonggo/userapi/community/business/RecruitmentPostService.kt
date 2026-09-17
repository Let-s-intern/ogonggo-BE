package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.implement.dto.RecruitmentPostAppendDto
import com.ogonggo.core.community.implement.dto.RecruitmentPostDraftAppendDto
import com.ogonggo.core.community.implement.RecruitmentPostAppender
import com.ogonggo.core.community.implement.RecruitmentPostBookmarkReader
import com.ogonggo.core.community.implement.RecruitmentPostApplicationReader
import com.ogonggo.core.community.implement.RecruitmentPostManager
import com.ogonggo.core.community.implement.PostMetricReader
import com.ogonggo.core.community.implement.PostMetricManager
import com.ogonggo.core.community.implement.RecruitmentPostReader
import com.ogonggo.core.community.implement.dto.RecruitmentPostUpdateDto
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.editor.lexical.LexicalEditorStateValidator
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class RecruitmentPostService(
    private val userReader: UserReader,
    private val postAppender: RecruitmentPostAppender,
    private val postManager: RecruitmentPostManager,
    private val postReader: RecruitmentPostReader,
    private val postBookmarkReader: RecruitmentPostBookmarkReader,
    private val postMetricReader: PostMetricReader,
    private val postMetricManager: PostMetricManager,
    private val contentValidator: LexicalEditorStateValidator,
    private val imageAssetManager: ImageAssetManager,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
    private val userProfileReader: UserProfileReader,
    private val applicationReader: RecruitmentPostApplicationReader,
) {

    @Transactional(readOnly = true)
    fun getRecruitmentPost(postId: Long): RecruitmentPostDetailResult = getRecruitmentPost(null, postId)

    @Transactional(readOnly = true)
    fun getRecruitmentPost(userId: Long?, postId: Long): RecruitmentPostDetailResult {
        val post = postReader.readPublished(postId)
        val metric = postMetricReader.read(postId)
        val result = RecruitmentPostDetailResult.from(
            post = post,
            metric = metric,
            author = RecruitmentPostAuthorResult.from(
                userId = post.authorUserId,
                profile = userProfileReader.read(post.authorUserId),
            ),
            bookmarked = userId != null && postBookmarkReader.readBookmarkedPostIds(userId, listOf(postId)).contains(postId),
        )
        return result.copy(
            bookmarkCount = if (userId != null) metric.bookmarkCount else 0L,
        )
            .also { eventPublisher.publishEvent(RecruitmentPostViewedEvent(it.id)) }
    }

    @Transactional(readOnly = true)
    fun getRecruitmentPosts(query: RecruitmentPostListQuery): RecruitmentPostPageResult =
        getRecruitmentPosts(null, query)

    @Transactional(readOnly = true)
    fun getRecruitmentPosts(userId: Long?, query: RecruitmentPostListQuery): RecruitmentPostPageResult {
        val page = postReader.readPublishedPage(query.page, query.size, query.filter, query.sortType)
        val postIds = page.posts.map { checkNotNull(it.id) { "조회된 모집글 식별자가 없습니다." } }
        val authorIds = page.posts.map { it.authorUserId }.distinct()
        val authorsByUserId = userProfileReader.readAll(authorIds).mapValues { (authorId, profile) ->
            RecruitmentPostAuthorResult.from(authorId, profile)
        }
        return page.toResult(
            metrics = postMetricReader.readAll(postIds),
            authorsByUserId = authorsByUserId,
            bookmarkedPostIds = if (userId == null) emptySet() else postBookmarkReader.readBookmarkedPostIds(userId, postIds),
            applicationCounts = applicationReader.countByPostIds(postIds),
            includeBookmarkCount = userId != null,
        )
    }

    @Transactional
    fun create(userId: Long, command: RecruitmentPostAppendDto): Long {
        verifyActiveUser(userId)
        val sanitizedCommand = command.copy(
            authorUserId = userId,
            content = contentValidator.validateAndSerialize(command.content),
        )
        val post = postAppender.append(sanitizedCommand)
        postMetricManager.initialize(checkNotNull(post.id))
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
    fun save(userId: Long, command: RecruitmentPostSaveCommand): Long = when (command) {
        is RecruitmentPostSaveCommand.Draft -> createDraft(userId, command.command)
        is RecruitmentPostSaveCommand.Published -> create(userId, command.command)
    }

    @Transactional
    fun createDraft(userId: Long, command: RecruitmentPostDraftAppendDto): Long {
        verifyActiveUser(userId)
        val sanitizedCommand = command.copy(
            authorUserId = userId,
            content = command.content?.let(contentValidator::validateAndSerialize),
        )
        val post = postAppender.appendDraft(sanitizedCommand)
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
    fun update(
        userId: Long,
        postId: Long,
        command: RecruitmentPostUpdateDto,
        saveMode: RecruitmentPostSaveMode? = null,
    ) {
        verifyActiveUser(userId)
        val post = postReader.readOwnedForUpdate(userId, postId)
        val previousContent = post.content.orEmpty()
        val sanitizedCommand = command.copy(
            content = command.content?.let(contentValidator::validateAndSerialize),
        )
        val now = LocalDateTime.now(clock)
        imageAssetManager.syncPostImages(
            ownerUserId = userId,
            postId = postId,
            previousContent = previousContent,
            currentContent = sanitizedCommand.content,
            now = now,
        )
        if (post.publicationStatus == PublicationStatus.DRAFT) {
            postManager.updateDraft(post, sanitizedCommand)
            if (saveMode == RecruitmentPostSaveMode.PUBLISH) {
                try {
                    postManager.publish(post)
                    postMetricManager.initialize(postId)
                } catch (_: IllegalArgumentException) {
                    throw InvalidValueException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_READY)
                } catch (_: IllegalStateException) {
                    throw InvalidValueException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_READY)
                }
            }
        } else {
            postManager.update(post, sanitizedCommand, now.toLocalDate())
        }
    }

    @Transactional
    fun delete(userId: Long, postId: Long) {
        verifyActiveUser(userId)
        val now = LocalDateTime.now(clock)
        postManager.delete(postReader.readOwnedForDelete(userId, postId), now)
        imageAssetManager.unreferencePostImages(postId, now)
    }

    @Transactional
    fun close(userId: Long, postId: Long) {
        verifyActiveUser(userId)
        postManager.close(
            postReader.readOwnedForUpdate(userId, postId),
            LocalDateTime.now(clock),
        )
    }

    @Transactional
    fun reopen(userId: Long, postId: Long) {
        verifyActiveUser(userId)
        val post = postReader.readOwnedForUpdate(userId, postId)
        if (post.recruitmentStatus == RecruitmentStatus.CLOSED &&
            post.recruitmentEndDate?.isAfter(LocalDate.now(clock)) != true
        ) {
            throw ConflictException(RecruitmentPostErrorCode.RECRUITMENT_POST_REOPEN_END_DATE_REQUIRED)
        }
        postManager.reopen(post)
    }

    private fun verifyActiveUser(userId: Long) {
        when (userReader.read(userId).status) {
            UserStatus.ACTIVE -> Unit
            UserStatus.SUSPENDED -> throw ForbiddenException(UserErrorCode.USER_SUSPENDED)
            UserStatus.WITHDRAWN -> throw ForbiddenException(UserErrorCode.USER_WITHDRAWN)
        }
    }
}
