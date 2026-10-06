package com.ogonggo.userapi.recruitmentpost.business

import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostContactMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPosition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostApplicationReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostManagementReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostManager
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostMetricManager
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostMetricReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostReader
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostManagementPageDto
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostMetricDto
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.user.implement.requireActive
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecruitmentPostManagementService(
    private val userReader: UserReader,
    private val managementReader: RecruitmentPostManagementReader,
    private val recruitmentPostMetricReader: RecruitmentPostMetricReader,
    private val applicationReader: RecruitmentPostApplicationReader,
    private val postReader: RecruitmentPostReader,
    private val postManager: RecruitmentPostManager,
    private val recruitmentPostMetricManager: RecruitmentPostMetricManager,
    private val imageAssetManager: ImageAssetManager,
) {

    @Transactional(readOnly = true)
    fun getPosts(
        userId: Long,
        status: RecruitmentPostManagementStatus,
        recruitmentStatus: RecruitmentPostRecruitmentStatus?,
        applicationStatus: RecruitmentPostApplicationStatus?,
        recruitmentType: RecruitmentPostType?,
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
        val metrics = recruitmentPostMetricReader.readAll(postIds)
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
        postManager.publish(post)
        recruitmentPostMetricManager.initialize(checkNotNull(post.id))
    }

    private fun verifyActiveUser(userId: Long) {
        userReader.read(userId).status.requireActive()
    }
}

data class RecruitmentPostFormResult(
    val postId: Long,
    val status: RecruitmentPostManagementStatus,
    val recruitmentStatus: RecruitmentPostRecruitmentStatus,
    val title: String,
    val recruitmentType: RecruitmentPostType?,
    val capacity: Int?,
    val progressMethod: RecruitmentPostProgressMethod?,
    val activityDurationMonths: Int?,
    val technologyStacks: List<String>,
    val summary: String?,
    val content: String?,
    val eligibilityAndSelectionProcess: String?,
    val recruitmentStartDate: LocalDate?,
    val recruitmentEndDate: LocalDate?,
    val positions: List<RecruitmentPostPosition>,
    val contactMethod: RecruitmentPostContactMethod?,
    val contactValue: String?,
    val agreedToPolicy: Boolean,
) {
    companion object {
        internal fun from(post: RecruitmentPost): RecruitmentPostFormResult = RecruitmentPostFormResult(
            postId = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." },
            status = when (post.publicationStatus) {
                RecruitmentPostPublicationStatus.DRAFT -> RecruitmentPostManagementStatus.DRAFT
                RecruitmentPostPublicationStatus.PUBLISHED -> RecruitmentPostManagementStatus.PUBLISHED
                RecruitmentPostPublicationStatus.HIDDEN -> RecruitmentPostManagementStatus.HIDDEN
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
            page: RecruitmentPostManagementPageDto,
            metrics: Map<Long, RecruitmentPostMetricDto>,
            applicationCounts: Map<Long, Long>,
        ): RecruitmentPostManagementPageResult = RecruitmentPostManagementPageResult(
            items = page.posts.map { post ->
                val postId = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." }
                RecruitmentPostManagementItemResult.from(
                    post = post,
                    metric = metrics[postId] ?: RecruitmentPostMetricDto.EMPTY,
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
    val recruitmentType: RecruitmentPostType?,
    val progressMethod: RecruitmentPostProgressMethod?,
    val activityDurationMonths: Int?,
    val recruitmentStatus: RecruitmentPostRecruitmentStatus?,
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
            metric: RecruitmentPostMetricDto,
            applicationCount: Long,
        ): RecruitmentPostManagementItemResult {
            val isDraft = post.publicationStatus == RecruitmentPostPublicationStatus.DRAFT
            return RecruitmentPostManagementItemResult(
                postId = checkNotNull(post.id) { "조회된 모집글 식별자가 없습니다." },
                status = when (post.publicationStatus) {
                    RecruitmentPostPublicationStatus.DRAFT -> RecruitmentPostManagementStatus.DRAFT
                    RecruitmentPostPublicationStatus.PUBLISHED -> RecruitmentPostManagementStatus.PUBLISHED
                    RecruitmentPostPublicationStatus.HIDDEN -> RecruitmentPostManagementStatus.HIDDEN
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
