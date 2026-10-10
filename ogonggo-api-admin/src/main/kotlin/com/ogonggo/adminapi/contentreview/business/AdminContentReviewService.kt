package com.ogonggo.adminapi.contentreview.business

import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.contentreview.domain.ContentReviewTargetType
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 기업회원이 올린 채용공고와 부트캠프를 검수한다.
 * 사유를 기업회원에게 전달하는 경로(메일·알림)는 아직 정하지 않아 반려 기록만 남긴다.
 */
@Service
class AdminContentReviewService(
    private val jobReader: JobReader,
    private val jobManager: JobManager,
    private val bootcampReader: BootcampReader,
    private val bootcampManager: BootcampManager,
    private val bootcampContentReader: BootcampContentReader,
    private val clock: Clock,
) {

    /**
     * 페이지를 나누지 않는다. 운영자가 앞뒤로 오가며 판정하므로 경계에서 다음 페이지를 기다리면 흐름이 끊긴다.
     * 밀린 것부터 처리하도록 등록일이 오래된 순으로 두 종류를 합친다.
     */
    fun getQueue(): List<AdminContentReviewItem> {
        val jobs = jobReader.readPendingReviews().map(AdminContentReviewItem::from)
        val bootcamps = bootcampReader.readPendingReviews()
        val curriculums = bootcampContentReader.readCurriculums(bootcamps.map { checkNotNull(it.id) })
        val bootcampItems = bootcamps.map { AdminContentReviewItem.from(it, curriculums[checkNotNull(it.id)].orEmpty()) }
        return (jobs + bootcampItems).sortedWith(
            compareBy<AdminContentReviewItem>({ it.registeredAt }, { it.type }, { it.id }),
        )
    }

    @Transactional
    fun approve(type: ContentReviewTargetType, contentId: Long): AdminContentReviewDecisionResult {
        val now = LocalDateTime.now(clock)
        val reviewStatus = when (type) {
            ContentReviewTargetType.JOB -> jobReader.readForUpdate(contentId)
                .also { jobManager.approveReview(it, now) }
                .reviewStatus

            ContentReviewTargetType.BOOTCAMP -> bootcampReader.readForUpdate(contentId)
                .also { bootcampManager.approveReview(it, now) }
                .reviewStatus
        }
        return decisionResult(type, contentId, reviewStatus)
    }

    /** 같은 대상을 다시 반려하면 반려 기록을 새로 만들지 않고 사유만 바꾼다. */
    @Transactional
    fun reject(type: ContentReviewTargetType, contentId: Long, reason: String): AdminContentReviewDecisionResult {
        val now = LocalDateTime.now(clock)
        val reviewStatus = when (type) {
            ContentReviewTargetType.JOB -> jobReader.readForUpdate(contentId)
                .also { jobManager.rejectReview(it, reason, now) }
                .reviewStatus

            ContentReviewTargetType.BOOTCAMP -> bootcampReader.readForUpdate(contentId)
                .also { bootcampManager.rejectReview(it, reason, now) }
                .reviewStatus
        }
        return decisionResult(type, contentId, reviewStatus)
    }

    /**
     * 저장한 판정을 검수 대기로 되돌린다. 키 하나로 통과시키는 화면이라 오조작이 실제로 일어난다.
     * 승인으로 노출됐던 콘텐츠는 다시 비노출이 되고 반려 기록은 지운다.
     */
    @Transactional
    fun undo(type: ContentReviewTargetType, contentId: Long): AdminContentReviewDecisionResult {
        val now = LocalDateTime.now(clock)
        val reviewStatus = when (type) {
            ContentReviewTargetType.JOB -> jobReader.readForUpdate(contentId)
                .also { jobManager.requestReview(it, now) }
                .reviewStatus

            ContentReviewTargetType.BOOTCAMP -> bootcampReader.readForUpdate(contentId)
                .also { bootcampManager.requestReview(it, now) }
                .reviewStatus
        }
        return decisionResult(type, contentId, reviewStatus)
    }

    /** 건수 조회는 같은 트랜잭션에서 방금 바꾼 상태를 먼저 반영한 뒤 센다. */
    private fun decisionResult(
        type: ContentReviewTargetType,
        contentId: Long,
        reviewStatus: ContentReviewStatus?,
    ): AdminContentReviewDecisionResult = AdminContentReviewDecisionResult(
        type = type,
        id = contentId,
        reviewStatus = checkNotNull(reviewStatus) { "검수한 콘텐츠의 검수 상태가 없습니다." },
        remaining = jobReader.countPendingReviews() + bootcampReader.countPendingReviews(),
    )
}
