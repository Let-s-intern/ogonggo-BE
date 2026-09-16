package com.ogonggo.core.community.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.community.persistence.PostMetricJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(PostMetricManager::class, PostMetricRegistrar::class)
internal class PostMetricPersistenceTest @Autowired constructor(
    private val postMetricManager: PostMetricManager,
    private val postMetricRepository: PostMetricJpaRepository,
) {

    @Test
    fun `지표 행을 초기화하면 기본값으로 저장한다`() {
        postMetricManager.initialize(POST_ID)

        val metric = postMetricRepository.findByPostId(POST_ID)
        assertEquals(0L, metric?.viewCount)
        assertEquals(0L, metric?.commentCount)
        assertEquals(0L, metric?.bookmarkCount)
    }

    @Test
    fun `조회 수는 지표 행이 없으면 생성하고 반복 조회마다 원자적으로 증가한다`() {
        postMetricManager.increaseViewCount(POST_ID, NOW)
        postMetricManager.increaseViewCount(POST_ID, NOW)

        assertEquals(2, postMetricRepository.findByPostId(POST_ID)?.viewCount)
    }

    @Test
    fun `댓글 수는 지표 행이 없으면 생성하고 원자적으로 증가·감소한다`() {
        postMetricManager.increaseCommentCount(POST_ID, NOW)
        postMetricManager.increaseCommentCount(POST_ID, NOW)
        postMetricManager.decreaseCommentCount(POST_ID, 2, NOW)

        assertEquals(0, postMetricRepository.findByPostId(POST_ID)?.commentCount)
    }

    companion object {
        private const val POST_ID = 12L
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 12, 0, 5)
    }
}
