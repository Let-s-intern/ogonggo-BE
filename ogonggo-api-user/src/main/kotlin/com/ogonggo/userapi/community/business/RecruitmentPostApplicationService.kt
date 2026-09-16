package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.RecruitmentApplicationProgressStatus
import com.ogonggo.core.community.domain.RecruitmentApplicationSortType
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.community.implement.RecruitmentPostApplicationPage
import com.ogonggo.core.community.implement.RecruitmentPostApplicationItem
import com.ogonggo.core.community.implement.RecruitmentPostApplicationReader
import com.ogonggo.core.community.implement.RecruitmentPostApplicationManager
import com.ogonggo.core.community.implement.RecruitmentPostReader
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

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
        if (post.recruitmentStatus == RecruitmentStatus.CLOSED) {
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
        recruitmentStatus: RecruitmentStatus?,
        recruitmentType: RecruitmentType?,
        keyword: String?,
        page: Int,
        size: Int,
        applicationStatus: RecruitmentApplicationProgressStatus? = null,
        sort: RecruitmentApplicationSortType = RecruitmentApplicationSortType.LATEST,
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
        val authorIds = result.items.map(RecruitmentPostApplicationItem::authorUserId).distinct()
        val authorsByUserId = userProfileReader.readAll(authorIds).mapValues { (authorId, profile) ->
            RecruitmentPostAuthorResult.from(authorId, profile)
        }
        return RecruitmentPostApplicationPageResult.from(result, authorsByUserId)
    }

    @Transactional
    fun changeApplicationStatus(
        userId: Long,
        postId: Long,
        status: RecruitmentApplicationProgressStatus,
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
        when (userReader.read(userId).status) {
            UserStatus.ACTIVE -> Unit
            UserStatus.SUSPENDED -> throw ForbiddenException(UserErrorCode.USER_SUSPENDED)
            UserStatus.WITHDRAWN -> throw ForbiddenException(UserErrorCode.USER_WITHDRAWN)
        }
    }
}

data class RecruitmentPostApplicationCreateResult(
    val postId: Long,
    val contactMethod: ContactMethod,
    val contactValue: String,
    val clickedAt: LocalDateTime,
)

data class RecruitmentPostApplicationPageResult(
    val items: List<RecruitmentPostApplicationItemResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val countsByRecruitmentType: Map<RecruitmentType, Long> = emptyMap(),
) {
    companion object {
        internal fun from(
            page: RecruitmentPostApplicationPage,
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
    val recruitmentType: RecruitmentType,
    val recruitmentStatus: RecruitmentStatus,
    val recruitmentEndDate: LocalDate,
    val progressMethod: com.ogonggo.core.community.domain.ProgressMethod =
        com.ogonggo.core.community.domain.ProgressMethod.ONLINE,
    val activityDurationMonths: Int = 0,
    val applicationStatus: RecruitmentApplicationProgressStatus = RecruitmentApplicationProgressStatus.PREPARING,
    val lastClickedAt: LocalDateTime,
    val author: RecruitmentPostAuthorResult,
)
