package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostMetricJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(RecruitmentPostMetricManager::class, RecruitmentPostMetricAppender::class, RecruitmentPostBookmarkManager::class)
internal class RecruitmentPostMetricPersistenceTest @Autowired constructor(
    private val recruitmentPostMetricManager: RecruitmentPostMetricManager,
    private val recruitmentPostMetricRepository: RecruitmentPostMetricJpaRepository,
    private val bookmarkManager: RecruitmentPostBookmarkManager,
) {

    @Test
    fun `지표 행을 초기화하면 기본값으로 저장한다`() {
        recruitmentPostMetricManager.initialize(POST_ID)

        val metric = recruitmentPostMetricRepository.findByPostId(POST_ID)
        assertEquals(0L, metric?.viewCount)
        assertEquals(0L, metric?.commentCount)
        assertEquals(0L, metric?.bookmarkCount)
    }

    @Test
    fun `조회 수는 지표 행이 없으면 생성하고 반복 조회마다 원자적으로 증가한다`() {
        recruitmentPostMetricManager.increaseViewCount(POST_ID, NOW)
        recruitmentPostMetricManager.increaseViewCount(POST_ID, NOW)

        assertEquals(2, recruitmentPostMetricRepository.findByPostId(POST_ID)?.viewCount)
    }

    @Test
    fun `댓글 수는 지표 행이 없으면 생성하고 원자적으로 증가·감소한다`() {
        recruitmentPostMetricManager.increaseCommentCount(POST_ID, NOW)
        recruitmentPostMetricManager.increaseCommentCount(POST_ID, NOW)
        recruitmentPostMetricManager.decreaseCommentCount(POST_ID, 2, NOW)

        assertEquals(0, recruitmentPostMetricRepository.findByPostId(POST_ID)?.commentCount)
    }

    @Test
    fun `북마크 수는 해제하지 않은 북마크를 다시 세어 맞춘다`() {
        // given: 세 명이 북마크하고 한 명이 해제했다
        listOf(1L, 2L, 3L).forEach { userId -> bookmarkManager.append(userId, POST_ID, NOW) }
        bookmarkManager.delete(3L, POST_ID, NOW)

        // when: 몇 번을 다시 세어도 결과가 같다
        recruitmentPostMetricManager.syncBookmarkCount(POST_ID, NOW)
        recruitmentPostMetricManager.syncBookmarkCount(POST_ID, NOW)

        // then
        assertEquals(2L, recruitmentPostMetricRepository.findByPostId(POST_ID)?.bookmarkCount)
    }

    @Test
    fun `댓글 카운터가 실제 수보다 작아 줄일 수 없어도 예외 없이 0을 유지한다`() {
        // given: 지표 행이 댓글보다 늦게 만들어져 카운터가 0인 상태
        recruitmentPostMetricManager.initialize(POST_ID)

        // when
        recruitmentPostMetricManager.decreaseCommentCount(POST_ID, 1, NOW)

        // then
        assertEquals(0L, recruitmentPostMetricRepository.findByPostId(POST_ID)?.commentCount)
    }

    companion object {
        private const val POST_ID = 12L
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 12, 0, 5)
    }
}
