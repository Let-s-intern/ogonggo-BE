package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.PostMetric
import com.ogonggo.core.community.persistence.PostMetricJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class PostMetricDto(
    val viewCount: Long,
    val commentCount: Long,
) {
    companion object {
        val EMPTY = PostMetricDto(viewCount = 0, commentCount = 0)

        internal fun from(metric: PostMetric): PostMetricDto = PostMetricDto(
            viewCount = metric.viewCount,
            commentCount = metric.commentCount,
        )
    }
}

@Component
internal class PostMetricRegistrar(
    private val postMetricRepository: PostMetricJpaRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun create(postId: Long) {
        postMetricRepository.saveAndFlush(PostMetric(postId = postId))
    }
}

@Component
class PostMetricReader internal constructor(
    private val postMetricRepository: PostMetricJpaRepository,
) {

    fun read(postId: Long): PostMetricDto =
        postMetricRepository.findByPostId(postId)?.let(PostMetricDto::from) ?: PostMetricDto.EMPTY

    fun readAll(postIds: Collection<Long>): Map<Long, PostMetricDto> {
        if (postIds.isEmpty()) return emptyMap()

        val metrics = postMetricRepository.findAllByPostIdIn(postIds.toSet())
            .associateBy(PostMetric::postId)

        return postIds.associateWith { postId ->
            metrics[postId]?.let(PostMetricDto::from) ?: PostMetricDto.EMPTY
        }
    }
}

@Component
class PostMetricManager internal constructor(
    private val postMetricRepository: PostMetricJpaRepository,
    private val postMetricRegistrar: PostMetricRegistrar,
) {

    @Transactional
    fun increaseViewCount(postId: Long, now: LocalDateTime) {
        updateOrCreate(postId) { postMetricRepository.increaseViewCount(postId, 1, now) }
    }

    fun increaseCommentCount(postId: Long, now: LocalDateTime) {
        updateOrCreate(postId) { postMetricRepository.increaseCommentCount(postId, now) }
    }

    fun decreaseCommentCount(postId: Long, amount: Int, now: LocalDateTime) {
        require(amount > 0) { "감소할 댓글 수는 양수여야 합니다." }
        val updated = postMetricRepository.decreaseCommentCount(postId, amount, now)
        if (updated > 0 || postMetricRepository.findByPostId(postId) == null) return
        error("댓글 카운터가 실제 댓글 수보다 작습니다. postId=$postId, amount=$amount")
    }

    private fun updateOrCreate(postId: Long, update: () -> Int) {
        if (update() > 0) return

        try {
            postMetricRegistrar.create(postId)
        } catch (_: DataIntegrityViolationException) {
            // 다른 요청이 만든 지표 행을 그대로 사용한다.
        }
        check(update() > 0) { "모집글 지표 행을 갱신하지 못했습니다. postId=$postId" }
    }
}
