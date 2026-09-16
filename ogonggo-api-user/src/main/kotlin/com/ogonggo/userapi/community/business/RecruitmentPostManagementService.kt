package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.domain.RecruitmentPostApplicationStatus
import com.ogonggo.core.community.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.community.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.community.implement.PostMetricDto
import com.ogonggo.core.community.implement.PostMetricManager
import com.ogonggo.core.community.implement.PostMetricReader
import com.ogonggo.core.community.implement.RecruitmentPostApplicationReader
import com.ogonggo.core.community.implement.RecruitmentPostManagementPage
import com.ogonggo.core.community.implement.RecruitmentPostManagementReader
import com.ogonggo.core.community.implement.RecruitmentPostManager
import com.ogonggo.core.community.implement.RecruitmentPostReader
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class RecruitmentPostManagementService(
    private val userReader: UserReader,
    private val managementReader: RecruitmentPostManagementReader,
    private val postMetricReader: PostMetricReader,
    private val applicationReader: RecruitmentPostApplicationReader,
    private val postReader: RecruitmentPostReader,
    private val postManager: RecruitmentPostManager,
    private val postMetricManager: PostMetricManager,
    private val imageAssetManager: ImageAssetManager,
) {

    @Transactional(readOnly = true)
    fun getPosts(
        userId: Long,
        status: RecruitmentPostManagementStatus,
        recruitmentStatus: RecruitmentStatus?,
        applicationStatus: RecruitmentPostApplicationStatus?,
        recruitmentType: RecruitmentType?,
        keyword: String?,
        page: Int,
        size: Int,
        sort: RecruitmentPostManagementSortType,
    ): RecruitmentPostManagementPageResult {
        verifyActiveUser(userId)
        val result = managementReader.readPage(
            ownerUserId = userId,
            status = status,
            recruitmentStatus = recruitmentStatus,
            applicationStatus = applicationStatus,
            recruitmentType = recruitmentType,
            keyword = keyword,
            page = page,
            size = size,
            sort = sort,
        )
        val postIds = result.posts.map { checkNotNull(it.id) { "조회된 모집글 식별자가 없습니다." } }
        val metrics = postMetricReader.readAll(postIds)
        val applications = applicationReader.countByPostIds(postIds)
        return RecruitmentPostManagementPageResult.from(result, metrics, applications)
    }

    @Transactional(readOnly = true)
    fun getPostForm(userId: Long, postId: Long): RecruitmentPostFormResult {
        verifyActiveUser(userId)
        return RecruitmentPostFormResult.from(postReader.readOwned(userId, postId))
    }

    @Transactional
    fun copy(userId: Long, postId: Long): RecruitmentPostFormResult {
        verifyActiveUser(userId)
        val source = postReader.readOwnedForUpdate(userId, postId)
        val copied = postManager.copyAsDraft(source)
        val copiedId = checkNotNull(copied.id) { "복사된 모집글 식별자가 없습니다." }
        copied.replaceDraftContent(
            imageAssetManager.copyPostImages(
                ownerUserId = userId,
                sourcePostId = postId,
                targetPostId = copiedId,
                content = copied.content,
            ),
        )
        return RecruitmentPostFormResult.from(copied)
    }

    @Transactional
    fun publish(userId: Long, postId: Long) {
        verifyActiveUser(userId)
        val post = postReader.readOwnedForUpdate(userId, postId)
        try {
            postManager.publish(post)
            postMetricManager.initialize(checkNotNull(post.id))
        } catch (_: IllegalArgumentException) {
            throw InvalidValueException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_READY)
        } catch (_: IllegalStateException) {
            throw InvalidValueException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_READY)
        }
    }

    private fun verifyActiveUser(userId: Long) {
        when (userReader.read(userId).status) {
            UserStatus.ACTIVE -> Unit
            UserStatus.SUSPENDED -> throw com.ogonggo.core.error.ForbiddenException(UserErrorCode.USER_SUSPENDED)
            UserStatus.WITHDRAWN -> throw com.ogonggo.core.error.ForbiddenException(UserErrorCode.USER_WITHDRAWN)
        }
    }
}

data class RecruitmentPostFormResult(
    val postId: Long,
    val status: RecruitmentPostManagementStatus,
    val recruitmentStatus: RecruitmentStatus,
    val title: String,
    val recruitmentType: RecruitmentType?,
    val capacity: Int?,
    val progressMethod: ProgressMethod?,
    val activityDurationMonths: Int?,
    val technologyStacks: List<String>,
    val summary: String?,
    val content: String?,
    val eligibilityAndSelectionProcess: String?,
    val recruitmentStartDate: LocalDate?,
    val recruitmentEndDate: LocalDate?,
    val positions: List<RecruitmentPosition>,
    val contactMethod: ContactMethod?,
    val contactValue: String?,
    val agreedToPolicy: Boolean,
) {
    companion object {
        internal fun from(post: RecruitmentPost): RecruitmentPostFormResult = RecruitmentPostFormResult(
            postId = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." },
            status = when (post.publicationStatus) {
                PublicationStatus.DRAFT -> RecruitmentPostManagementStatus.DRAFT
                PublicationStatus.PUBLISHED -> RecruitmentPostManagementStatus.PUBLISHED
                PublicationStatus.HIDDEN -> RecruitmentPostManagementStatus.HIDDEN
            },
            recruitmentStatus = post.recruitmentStatus,
            title = post.title,
            recruitmentType = post.recruitmentType,
            capacity = post.capacity,
            progressMethod = post.progressMethod,
            activityDurationMonths = post.activityDurationMonths,
            technologyStacks = post.technologyStacks,
            summary = post.summary,
            content = post.content,
            eligibilityAndSelectionProcess = post.eligibilityAndSelectionProcess,
            recruitmentStartDate = post.recruitmentStartDate,
            recruitmentEndDate = post.recruitmentEndDate,
            positions = post.positions,
            contactMethod = post.contactMethod,
            contactValue = post.contactValue,
            agreedToPolicy = false,
        )
    }
}

data class RecruitmentPostManagementPageResult(
    val items: List<RecruitmentPostManagementItemResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(
            page: RecruitmentPostManagementPage,
            metrics: Map<Long, PostMetricDto>,
            applicationCounts: Map<Long, Long>,
        ): RecruitmentPostManagementPageResult = RecruitmentPostManagementPageResult(
            items = page.posts.map { post ->
                val postId = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." }
                RecruitmentPostManagementItemResult.from(
                    post = post,
                    metric = metrics[postId] ?: PostMetricDto.EMPTY,
                    applicationCount = applicationCounts[postId] ?: 0L,
                )
            },
            page = page.page,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}

data class RecruitmentPostManagementItemResult(
    val postId: Long,
    val status: RecruitmentPostManagementStatus,
    val title: String,
    val recruitmentType: RecruitmentType?,
    val progressMethod: ProgressMethod?,
    val activityDurationMonths: Int?,
    val recruitmentStatus: RecruitmentStatus?,
    val recruitmentStartDate: LocalDate?,
    val recruitmentEndDate: LocalDate?,
    val applicationCount: Long,
    val capacity: Int?,
    val viewCount: Long,
    val commentCount: Long,
    val lastSavedAt: LocalDateTime,
    val continueWriting: Boolean,
) {
    companion object {
        internal fun from(
            post: RecruitmentPost,
            metric: PostMetricDto,
            applicationCount: Long,
        ): RecruitmentPostManagementItemResult {
            val isDraft = post.publicationStatus == PublicationStatus.DRAFT
            return RecruitmentPostManagementItemResult(
                postId = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." },
                status = when (post.publicationStatus) {
                    PublicationStatus.DRAFT -> RecruitmentPostManagementStatus.DRAFT
                    PublicationStatus.PUBLISHED -> RecruitmentPostManagementStatus.PUBLISHED
                    PublicationStatus.HIDDEN -> RecruitmentPostManagementStatus.HIDDEN
                },
                title = post.title,
                recruitmentType = post.recruitmentType,
                progressMethod = post.progressMethod,
                activityDurationMonths = post.activityDurationMonths,
                recruitmentStatus = post.recruitmentStatus.takeUnless { isDraft },
                recruitmentStartDate = post.recruitmentStartDate,
                recruitmentEndDate = post.recruitmentEndDate,
                applicationCount = applicationCount,
                capacity = post.capacity,
                viewCount = metric.viewCount,
                commentCount = metric.commentCount,
                lastSavedAt = post.updatedAt,
                continueWriting = isDraft,
            )
        }
    }
}
