package com.ogonggo.core.job.persistence

import com.ogonggo.core.job.domain.JobApplicationStatus
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.QJob.job
import com.ogonggo.core.job.domain.QJobBookmark.jobBookmark
import com.ogonggo.core.job.implement.dto.JobBookmarkReminderCandidateDto
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.QNotification.notification
import com.ogonggo.core.user.domain.QUser.user
import com.ogonggo.core.user.domain.QUserProfile.userProfile
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal class JobBookmarkReminderQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findEligibleCandidates(
        now: LocalDateTime,
        afterBookmarkId: Long?,
        limit: Int,
    ): List<JobBookmarkReminderCandidateDto> {
        // Entity 전체를 로딩하지 않고 알림 적재에 필요한 값만 projection으로 조회한다.
        // 이미 적재된 알림은 재실행 시 제외해 별도 일정 커서 없이 다음 대상으로 진행한다.
        val reminderAt = Expressions.dateTimeTemplate(
            LocalDateTime::class.java,
            "timestampadd(hour, -24, {0})",
            job.recruitmentEndAt,
        )
        val jobNotificationPrefix = Expressions.stringTemplate(
            "concat('clip-remind:job:', {0}, ':')",
            job.id,
        )
        val notificationNotAlreadyQueued = JPAExpressions.selectOne()
            .from(notification)
            .where(
                notification.recipientUserId.eq(jobBookmark.userId),
                notification.channel.eq(NotificationChannel.KAKAO),
                notification.scheduledAt.eq(reminderAt),
                notification.deduplicationKey.startsWith(jobNotificationPrefix),
            )
            .notExists()

        return queryFactory.select(
            jobBookmark.id,
            jobBookmark.jobId,
            jobBookmark.userId,
            reminderAt,
            userProfile.phoneNum,
            userProfile.name,
            job.title,
        )
            .from(jobBookmark)
            .join(job).on(job.id.eq(jobBookmark.jobId))
            .join(user).on(user.id.eq(jobBookmark.userId))
            .join(userProfile).on(userProfile.userId.eq(user.id))
            .where(
                // 공고·모집·스크랩·계정의 발송 자격은 매 실행 시 다시 확인한다.
                job.recruitmentEndAt.isNotNull,
                job.recruitmentEndAt.loe(now.plusHours(24)),
                jobBookmark.activeSince.loe(reminderAt),
                jobBookmark.deletedAt.isNull,
                jobBookmark.applicationStatus.`in`(JobApplicationStatus.SCRAPPED, JobApplicationStatus.PREPARING),
                job.publicationStatus.eq(JobPublicationStatus.PUBLISHED),
                job.recruitmentStatus.eq(JobRecruitmentStatus.RECRUITING),
                job.recruitmentEndAt.goe(now),
                job.deletedAt.isNull,
                user.role.eq(UserRole.USER),
                user.status.eq(UserStatus.ACTIVE),
                userProfile.name.isNotNull,
                userProfile.name.trim().ne(""),
                userProfile.phoneNum.isNotNull,
                userProfile.phoneNum.trim().ne(""),
                notificationNotAlreadyQueued,
                // 이 커서는 한 번의 실행 안에서 페이지를 이동할 때만 사용한다.
                afterBookmarkId?.let(jobBookmark.id::gt),
            )
            .orderBy(jobBookmark.id.asc())
            .limit(limit.toLong())
            .fetch()
            .map { row ->
                JobBookmarkReminderCandidateDto(
                    bookmarkId = checkNotNull(row.get(jobBookmark.id)),
                    jobId = checkNotNull(row.get(jobBookmark.jobId)),
                    userId = checkNotNull(row.get(jobBookmark.userId)),
                    reminderAt = checkNotNull(row.get(reminderAt)),
                    recipientNo = checkNotNull(row.get(userProfile.phoneNum)),
                    recipientName = checkNotNull(row.get(userProfile.name)),
                    postingTitle = checkNotNull(row.get(job.title)),
                )
            }
    }
}
