package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostMetric
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostMetricJpaRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
internal class RecruitmentPostMetricAppender(
    private val recruitmentPostMetricRepository: RecruitmentPostMetricJpaRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun create(postId: Long) {
        recruitmentPostMetricRepository.saveAndFlush(RecruitmentPostMetric(postId = postId))
    }
}

@Component
class RecruitmentPostMetricManager internal constructor(
    private val recruitmentPostMetricRepository: RecruitmentPostMetricJpaRepository,
    private val recruitmentPostMetricAppender: RecruitmentPostMetricAppender,
) {

    @Transactional
    fun increaseViewCount(postId: Long, now: LocalDateTime) {
        updateOrCreate(postId) { recruitmentPostMetricRepository.increaseViewCount(postId, 1, now) }
    }

    /** 공개 모집글 생성·게시 시점에 지표 행을 같은 트랜잭션으로 준비한다. */
    @Transactional
    fun initialize(postId: Long) {
        if (recruitmentPostMetricRepository.findByPostId(postId) == null) {
            recruitmentPostMetricRepository.save(RecruitmentPostMetric(postId = postId))
        }
    }

    @Transactional
    fun increaseCommentCount(postId: Long, now: LocalDateTime) {
        updateOrCreate(postId) { recruitmentPostMetricRepository.increaseCommentCount(postId, now) }
    }

    /**
     * 카운터가 실제 수보다 작아 줄일 수 없으면 값을 두고 경고만 남긴다.
     * 지표는 화면 표시용이라, 어긋났다고 사용자의 댓글 삭제나 북마크 해제를 실패시키지 않는다.
     */
    @Transactional
    fun decreaseCommentCount(postId: Long, amount: Int, now: LocalDateTime) {
        require(amount > 0) { "감소할 댓글 수는 양수여야 합니다." }
        val updated = recruitmentPostMetricRepository.decreaseCommentCount(postId, amount, now)
        if (updated > 0 || recruitmentPostMetricRepository.findByPostId(postId) == null) return
        log.warn("댓글 카운터가 실제 댓글 수보다 작아 줄이지 않았습니다. postId={}, amount={}", postId, amount)
    }

    @Transactional
    fun increaseBookmarkCount(postId: Long, now: LocalDateTime) =
        updateOrCreate(postId) { recruitmentPostMetricRepository.increaseBookmarkCount(postId, now) }

    /** [decreaseCommentCount]와 같이 줄일 수 없으면 값을 두고 경고만 남긴다. */
    @Transactional
    fun decreaseBookmarkCount(postId: Long, now: LocalDateTime) {
        if (recruitmentPostMetricRepository.decreaseBookmarkCount(postId, now) > 0) return
        log.warn("북마크 카운터가 실제 북마크 수보다 작아 줄이지 않았습니다. postId={}", postId)
    }

    private fun updateOrCreate(postId: Long, update: () -> Int) {
        // update로 없는 행을 먼저 잠그면 REQUIRES_NEW insert가 gap lock에 막힐 수 있다.
        ensureMetricExists(postId)
        check(update() > 0) { "모집글 지표 행을 갱신하지 못했습니다. postId=$postId" }
    }

    private fun ensureMetricExists(postId: Long) {
        if (recruitmentPostMetricRepository.findByPostId(postId) != null) return

        try {
            recruitmentPostMetricAppender.create(postId)
        } catch (_: DataIntegrityViolationException) {
            // 다른 요청이 만든 지표 행을 그대로 사용한다.
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(RecruitmentPostMetricManager::class.java)
    }
}
