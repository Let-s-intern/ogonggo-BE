package com.ogonggo.userapi.recruitmentpost.business

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationProgressStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostContactMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.error.RecruitmentPostErrorCode
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostApplicationManager
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostApplicationReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostReader
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostApplicationItemDto
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostApplicationPageDto
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.user.implement.requireActive
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecruitmentPostApplicationService(
    private val userReader: UserReader,
    private val postReader: RecruitmentPostReader,
    private val applicationManager: RecruitmentPostApplicationManager,
    private val applicationReader: RecruitmentPostApplicationReader,
    private val userProfileReader: UserProfileReader,
    private val clock: Clock,
) {

    @Transactional
    fun createApplication(userId: Long, postId: Long): RecruitmentPostApplicationCreateResult {
        verifyActiveUser(userId)
        val post = postReader.readPublishedForUpdate(postId)
        if (post.recruitmentStatus == RecruitmentPostRecruitmentStatus.CLOSED) {
            throw ConflictException(RecruitmentPostErrorCode.RECRUITMENT_POST_CLOSED)
        }

        val clickedAt = LocalDateTime.now(clock)
        applicationManager.recordClick(
            postId = postId,
            userId = userId,
            clickedAt = clickedAt,
        )
        return RecruitmentPostApplicationCreateResult(
            postId = postId,
            contactMethod = checkNotNull(post.contactMethod),
            contactValue = checkNotNull(post.contactValue),
            clickedAt = clickedAt,
        )
    }

    @Transactional(readOnly = true)
    fun getApplications(
        userId: Long,
        recruitmentStatus: RecruitmentPostRecruitmentStatus?,
        recruitmentType: RecruitmentPostType?,
        keyword: String?,
        page: Int,
        size: Int,
        applicationStatus: RecruitmentPostApplicationProgressStatus? = null,
        sort: RecruitmentPostApplicationSortType = RecruitmentPostApplicationSortType.LATEST,
    ): RecruitmentPostApplicationPageResult {
        verifyActiveUser(userId)
        val result = applicationReader.readPage(
            userId = userId,
            recruitmentStatus = recruitmentStatus,
            applicationStatus = applicationStatus,
            recruitmentType = recruitmentType,
            keyword = keyword,
            page = page,
            size = size,
            sort = sort,
        )
        val authorIds = result.items.map(RecruitmentPostApplicationItemDto::authorUserId).distinct()
        val authorsByUserId = userProfileReader.readAll(authorIds).mapValues { (authorId, profile) ->
            RecruitmentPostAuthorResult.from(authorId, profile)
        }
        return RecruitmentPostApplicationPageResult.from(result, authorsByUserId)
    }

    @Transactional
    fun changeApplicationStatus(
        userId: Long,
        postId: Long,
        status: RecruitmentPostApplicationProgressStatus,
    ) {
        verifyActiveUser(userId)
        applicationManager.changeStatus(postId = postId, userId = userId, status = status)
    }

    @Transactional
    fun deleteApplication(userId: Long, postId: Long) {
        verifyActiveUser(userId)
        applicationManager.delete(
            postId = postId,
            userId = userId,
            deletedAt = LocalDateTime.now(clock),
        )
    }

    private fun verifyActiveUser(userId: Long) {
        userReader.read(userId).status.requireActive()
    }
}

data class RecruitmentPostApplicationCreateResult(
    val postId: Long,
    val contactMethod: RecruitmentPostContactMethod,
    val contactValue: String,
    val clickedAt: LocalDateTime,
)

data class RecruitmentPostApplicationPageResult(
    val items: List<RecruitmentPostApplicationItemResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val countsByRecruitmentType: Map<RecruitmentPostType, Long> = emptyMap(),
) {
    companion object {
        internal fun from(
            page: RecruitmentPostApplicationPageDto,
            authorsByUserId: Map<Long, RecruitmentPostAuthorResult>,
        ): RecruitmentPostApplicationPageResult = RecruitmentPostApplicationPageResult(
            items = page.items.map { item ->
                RecruitmentPostApplicationItemResult(
                    postId = item.postId,
                    title = item.title,
                    recruitmentType = item.recruitmentType,
                    recruitmentStatus = item.recruitmentStatus,
                    recruitmentEndDate = item.recruitmentEndDate,
                    progressMethod = item.progressMethod,
                    activityDurationMonths = item.activityDurationMonths,
                    applicationStatus = item.applicationStatus,
                    lastClickedAt = item.lastClickedAt,
                    author = authorsByUserId[item.authorUserId]
                        ?: RecruitmentPostAuthorResult.from(item.authorUserId, null),
                )
            },
            page = page.page,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            countsByRecruitmentType = page.countsByRecruitmentType,
        )
    }
}

data class RecruitmentPostApplicationItemResult(
    val postId: Long,
    val title: String,
    val recruitmentType: RecruitmentPostType,
    val recruitmentStatus: RecruitmentPostRecruitmentStatus,
    val recruitmentEndDate: LocalDate,
    val progressMethod: com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod =
        com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod.ONLINE,
    val activityDurationMonths: Int = 0,
    val applicationStatus: RecruitmentPostApplicationProgressStatus = RecruitmentPostApplicationProgressStatus.PREPARING,
    val lastClickedAt: LocalDateTime,
    val author: RecruitmentPostAuthorResult,
)
