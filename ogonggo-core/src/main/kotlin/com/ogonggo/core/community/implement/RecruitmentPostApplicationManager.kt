package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPostApplication
import com.ogonggo.core.community.domain.RecruitmentApplicationProgressStatus
import com.ogonggo.core.community.persistence.RecruitmentPostApplicationJpaRepository
import com.ogonggo.core.community.error.RecruitmentPostApplicationErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RecruitmentPostApplicationManager internal constructor(
    private val applicationRepository: RecruitmentPostApplicationJpaRepository,
) {

    /** 같은 사용자·모집글 이력은 하나로 유지하고 재접근 시 최근 시각만 갱신한다. */
    fun recordClick(
        postId: Long,
        userId: Long,
        clickedAt: LocalDateTime,
    ) {
        val application = applicationRepository.findByPostIdAndUserId(postId, userId)
            ?: RecruitmentPostApplication(
                postId = postId,
                userId = userId,
                firstClickedAt = clickedAt,
                lastClickedAt = clickedAt,
            )

        application.recordClick(clickedAt)
        applicationRepository.save(application)
    }

    /**
     * 스크랩한 모집글을 지원 준비 중 이력으로 만든다. 지운 이력이 있으면 새로 만들지 않고 되살린다.
     * 활성 이력이 있는지는 호출하는 유스케이스가 먼저 확인한다.
     */
    fun startPreparation(
        postId: Long,
        userId: Long,
        now: LocalDateTime,
    ) {
        val application = applicationRepository.findByPostIdAndUserId(postId, userId)
            ?: RecruitmentPostApplication(
                postId = postId,
                userId = userId,
                firstClickedAt = now,
                lastClickedAt = now,
            )
        application.restoreAsPreparing()
        applicationRepository.save(application)
    }

    fun changeStatus(
        postId: Long,
        userId: Long,
        status: RecruitmentApplicationProgressStatus,
    ) {
        readActive(postId, userId).changeStatus(status)
    }

    fun delete(
        postId: Long,
        userId: Long,
        deletedAt: LocalDateTime,
    ) {
        readActive(postId, userId).delete(deletedAt)
    }

    private fun readActive(postId: Long, userId: Long): RecruitmentPostApplication =
        applicationRepository.findByPostIdAndUserIdAndDeletedAtIsNull(postId, userId)
            ?: throw EntityNotFoundException(RecruitmentPostApplicationErrorCode.RECRUITMENT_POST_APPLICATION_NOT_FOUND)
}
