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
    val bookmarkCount: Long = 0,
) {
    companion object {
        val EMPTY = PostMetricDto(viewCount = 0, commentCount = 0, bookmarkCount = 0)

        internal fun from(metric: PostMetric): PostMetricDto = PostMetricDto(
            viewCount = metric.viewCount,
            commentCount = metric.commentCount,
            bookmarkCount = metric.bookmarkCount,
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

    /** 공개 모집글 생성·게시 시점에 지표 행을 같은 트랜잭션으로 준비한다. */
    @Transactional
    fun initialize(postId: Long) {
        if (postMetricRepository.findByPostId(postId) == null) {
            postMetricRepository.save(PostMetric(postId = postId))
        }
    }

    @Transactional
    fun increaseCommentCount(postId: Long, now: LocalDateTime) {
        updateOrCreate(postId) { postMetricRepository.increaseCommentCount(postId, now) }
    }

    @Transactional
    fun decreaseCommentCount(postId: Long, amount: Int, now: LocalDateTime) {
        require(amount > 0) { "감소할 댓글 수는 양수여야 합니다." }
        val updated = postMetricRepository.decreaseCommentCount(postId, amount, now)
        if (updated > 0 || postMetricRepository.findByPostId(postId) == null) return
        error("댓글 카운터가 실제 댓글 수보다 작습니다. postId=$postId, amount=$amount")
    }

    @Transactional
    fun increaseBookmarkCount(postId: Long, now: LocalDateTime) =
        updateOrCreate(postId) { postMetricRepository.increaseBookmarkCount(postId, now) }

    @Transactional
    fun decreaseBookmarkCount(postId: Long, now: LocalDateTime) {
        check(postMetricRepository.decreaseBookmarkCount(postId, now) > 0) {
            "북마크 카운터가 실제 북마크 수보다 작습니다. postId=$postId"
        }
    }

    private fun updateOrCreate(postId: Long, update: () -> Int) {
        // update로 없는 행을 먼저 잠그면 REQUIRES_NEW insert가 gap lock에 막힐 수 있다.
        ensureMetricExists(postId)
        check(update() > 0) { "모집글 지표 행을 갱신하지 못했습니다. postId=$postId" }
    }

    private fun ensureMetricExists(postId: Long) {
        if (postMetricRepository.findByPostId(postId) != null) return

        try {
            postMetricRegistrar.create(postId)
        } catch (_: DataIntegrityViolationException) {
            // 다른 요청이 만든 지표 행을 그대로 사용한다.
        }
    }
}
