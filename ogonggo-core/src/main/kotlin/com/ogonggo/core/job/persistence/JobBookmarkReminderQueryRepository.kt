package com.ogonggo.core.job.persistence

import com.ogonggo.core.job.domain.JobApplicationStatus
import com.ogonggo.core.job.domain.JobBookmarkReminderScheduleStatus
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.QJob.job
import com.ogonggo.core.job.domain.QJobBookmark.jobBookmark
import com.ogonggo.core.job.domain.QJobBookmarkReminderSchedule.jobBookmarkReminderSchedule
import com.ogonggo.core.job.implement.dto.JobBookmarkReminderCandidateDto
import com.ogonggo.core.user.domain.QUser.user
import com.ogonggo.core.user.domain.QUserProfile.userProfile
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal class JobBookmarkReminderQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findEligibleCandidates(
        jobId: Long,
        recruitmentEndAt: LocalDateTime,
        scheduledAt: LocalDateTime,
        now: LocalDateTime,
        afterBookmarkId: Long?,
        limit: Int,
    ): List<JobBookmarkReminderCandidateDto> {
        // Entity 전체를 로딩하지 않고 알림 적재에 필요한 값만 projection으로 조회한다.
        // afterBookmarkId 커서와 ID 정렬을 함께 사용해 페이지 재개 시 앞서 처리한 스크랩을 건너뛴다.
        return queryFactory.select(
            jobBookmark.id,
            jobBookmark.jobId,
            jobBookmark.userId,
            job.recruitmentEndAt,
            jobBookmarkReminderSchedule.reminderAt,
            userProfile.phoneNum,
            userProfile.name,
            job.title,
        )
            .from(jobBookmark)
            .join(job).on(job.id.eq(jobBookmark.jobId))
            .join(jobBookmarkReminderSchedule).on(
                jobBookmarkReminderSchedule.jobId.eq(job.id)
                    .and(jobBookmarkReminderSchedule.recruitmentEndAt.eq(job.recruitmentEndAt)),
            )
            .join(user).on(user.id.eq(jobBookmark.userId))
            .join(userProfile).on(userProfile.userId.eq(user.id))
            .where(
                // 일정이 대기 중이고, 공고·모집·스크랩·계정이 모두 발송 자격을 유지하는지 재검증한다.
                job.id.eq(jobId),
                jobBookmarkReminderSchedule.jobId.eq(jobId),
                job.recruitmentEndAt.eq(recruitmentEndAt),
                jobBookmarkReminderSchedule.reminderAt.eq(scheduledAt),
                jobBookmarkReminderSchedule.status.eq(JobBookmarkReminderScheduleStatus.PENDING),
                jobBookmark.activeSince.loe(scheduledAt),
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
                // null이면 첫 페이지, 값이 있으면 커서 뒤만 읽는다. 지난 ID는 자격이 바뀌어도 재평가하지 않는다.
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
                    recruitmentEndAt = checkNotNull(row.get(job.recruitmentEndAt)),
                    recipientNo = checkNotNull(row.get(userProfile.phoneNum)),
                    recipientName = checkNotNull(row.get(userProfile.name)),
                    postingTitle = checkNotNull(row.get(job.title)),
                )
            }
    }
}
