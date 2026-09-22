package com.ogonggo.core.job.implement

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.domain.JobApplicationStatus
import com.ogonggo.core.job.domain.JobBookmark
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.persistence.JobBookmarkJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class JobBookmarkManager internal constructor(
    private val jobBookmarkRepository: JobBookmarkJpaRepository,
) {

    /**
     * 소프트 삭제된 행이 유니크 제약을 계속 차지하므로 재등록은 새 행이 아니라 기존 행 복구로 처리한다.
     * 조회한 상태로 분기하면 동시에 들어온 해제 요청과 순서가 뒤집힐 수 있어 조건을 UPDATE에 맡긴다.
     * 복구할 행이 없으면 새로 저장하고, 이미 활성이면 유니크 제약이 막으므로 중복 등록으로 본다.
     */
    fun append(userId: Long, jobId: Long, now: LocalDateTime) {
        if (jobBookmarkRepository.restore(jobId = jobId, userId = userId, now = now) > 0) {
            return
        }

        try {
            jobBookmarkRepository.saveAndFlush(JobBookmark(jobId = jobId, userId = userId))
        } catch (exception: DataIntegrityViolationException) {
            throw ConflictException(JobErrorCode.JOB_BOOKMARK_ALREADY_EXISTS)
        }
    }

    /** 활성 북마크가 없으면 갱신 대상이 없어 그대로 끝나므로 여러 번 해제해도 결과가 같다. */
    fun delete(userId: Long, jobId: Long, now: LocalDateTime) {
        jobBookmarkRepository.softDelete(jobId = jobId, userId = userId, now = now)
    }

    /**
     * 활성 북마크를 지원·신청 관리의 다른 단계로 옮긴다. 이미 그 단계에 있으면 그대로 끝나므로 여러 번 옮겨도 결과가 같다.
     * 옮기지 못했을 때만 북마크가 있는지 읽어, 없는 북마크와 이미 목표 단계인 북마크를 구분한다.
     */
    fun changeApplicationStatus(userId: Long, jobId: Long, target: JobApplicationStatus, now: LocalDateTime) {
        if (jobBookmarkRepository.changeApplicationStatus(jobId, userId, target, now) > 0) {
            return
        }
        jobBookmarkRepository.findActiveApplicationStatus(jobId, userId)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_BOOKMARK_NOT_FOUND)
    }
}
