package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.jpa.ensureRowCreated
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostMetric
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostMetricJpaRepository
import java.time.LocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

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
     * 지표는 화면 표시용이라, 어긋났다고 사용자의 댓글 삭제를 실패시키지 않는다.
     */
    @Transactional
    fun decreaseCommentCount(postId: Long, amount: Int, now: LocalDateTime) {
        require(amount > 0) { "감소할 댓글 수는 양수여야 합니다." }
        val updated = recruitmentPostMetricRepository.decreaseCommentCount(postId, amount, now)
        if (updated > 0 || recruitmentPostMetricRepository.findByPostId(postId) == null) return
        log.warn("댓글 카운터가 실제 댓글 수보다 작아 줄이지 않았습니다. postId={}, amount={}", postId, amount)
    }

    /**
     * 북마크 수를 증감하지 않고 활성 북마크를 다시 세어 맞춘다.
     * 몇 번을 실행해도 결과가 같으므로 갱신을 한 번 놓쳐도 다음 갱신에서 값이 스스로 복구된다.
     */
    @Transactional
    fun syncBookmarkCount(postId: Long, now: LocalDateTime) =
        updateOrCreate(postId) { recruitmentPostMetricRepository.syncBookmarkCount(postId, now) }

    private fun updateOrCreate(postId: Long, update: () -> Int) {
        ensureMetricExists(postId)
        check(update() > 0) { "모집글 지표 행을 갱신하지 못했습니다. postId=$postId" }
    }

    private fun ensureMetricExists(postId: Long) = ensureRowCreated(
        exists = { recruitmentPostMetricRepository.findByPostId(postId) != null },
        create = { recruitmentPostMetricAppender.create(postId) },
    )

    private companion object {
        val log: Logger = LoggerFactory.getLogger(RecruitmentPostMetricManager::class.java)
    }
}
