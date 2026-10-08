package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernComment
import com.ogonggo.core.concern.domain.ConcernSortType
import com.ogonggo.core.concern.implement.dto.ConcernAppendDto
import com.ogonggo.core.concern.implement.dto.ConcernCommentAppendDto
import com.ogonggo.core.concern.persistence.ConcernQueryRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    ConcernAppender::class,
    ConcernReader::class,
    ConcernQueryRepository::class,
    ConcernMetricManager::class,
    ConcernMetricReader::class,
    ConcernCommentAppender::class,
    ConcernCommentReader::class,
    ConcernCommentRemover::class,
    ConcernCommentHelpfulVoteAppender::class,
    ConcernCommentHelpfulVoteManager::class,
    ConcernCommentHelpfulVoteReader::class,
)
internal class ConcernImplementPersistenceTest @Autowired constructor(
    private val concernAppender: ConcernAppender,
    private val concernReader: ConcernReader,
    private val metricManager: ConcernMetricManager,
    private val metricReader: ConcernMetricReader,
    private val commentAppender: ConcernCommentAppender,
    private val commentReader: ConcernCommentReader,
    private val commentRemover: ConcernCommentRemover,
    private val voteManager: ConcernCommentHelpfulVoteManager,
    private val voteReader: ConcernCommentHelpfulVoteReader,
    private val entityManager: EntityManager,
) {

    @Test
    fun `고민글을 등록하면 0으로 시작하는 지표 행을 함께 만든다`() {
        // given
        // when
        val concernId = appendConcern(ConcernCategory.ETC)
        metricManager.increaseViewCount(concernId, NOW)

        // then
        assertEquals(1L, metricReader.read(concernId).viewCount)
        assertEquals(0L, metricReader.read(concernId).commentCount)
    }

    @Test
    fun `목록은 삭제된 고민글을 빼고 카테고리로 거르며 조회 많은 순으로 정렬한다`() {
        // given
        val lessViewed = appendConcern(ConcernCategory.JOB_CAREER)
        val mostViewed = appendConcern(ConcernCategory.JOB_CAREER)
        val otherCategory = appendConcern(ConcernCategory.ETC)
        val deleted = appendConcern(ConcernCategory.JOB_CAREER)
        repeat(3) { metricManager.increaseViewCount(mostViewed, NOW) }
        metricManager.increaseViewCount(lessViewed, NOW)
        repeat(5) { metricManager.increaseViewCount(otherCategory, NOW) }
        concernReader.read(deleted).delete(NOW)
        entityManager.flush()

        // when
        val page = concernReader.readPage(ConcernCategory.JOB_CAREER, ConcernSortType.VIEW_COUNT, page = 0, size = 10)

        // then
        assertEquals(listOf(mostViewed, lessViewed), page.concerns.map { it.id })
        assertEquals(2L, page.totalElements)
    }

    @Test
    fun `답변은 관리자 답변을 먼저 두고 먼저 단 순서로 주며 답글이 없는 삭제 답변은 뺀다`() {
        // given
        val concernId = appendConcern(ConcernCategory.ETC)
        val first = appendComment(concernId, official = false)
        val deletedWithoutReply = appendComment(concernId, official = false)
        val deletedWithReply = appendComment(concernId, official = false)
        appendComment(concernId, parentId = deletedWithReply.id, official = false)
        val official = appendComment(concernId, official = true)
        commentRemover.remove(deletedWithoutReply, NOW)
        commentRemover.remove(deletedWithReply, NOW)
        entityManager.flush()

        // when
        val page = commentReader.readRootPage(concernId, page = 0, size = 10)

        // then
        assertEquals(listOf(official.id, first.id, deletedWithReply.id), page.comments.map { it.id })
        assertEquals(3L, page.totalElements)
    }

    @Test
    fun `답글 미리보기는 답변마다 앞의 답글만 담고 전체 답글 수를 함께 준다`() {
        // given
        val concernId = appendConcern(ConcernCategory.ETC)
        val parent = appendComment(concernId, official = false)
        val replies = (1..3).map { appendComment(concernId, parentId = parent.id, official = false) }
        entityManager.flush()

        // when
        val previews = commentReader.readReplyPreviews(concernId, listOf(checkNotNull(parent.id)), size = 2)

        // then
        val preview = checkNotNull(previews[parent.id])
        assertEquals(replies.take(2).map { it.id }, preview.comments.map { it.id })
        assertEquals(3L, preview.totalElements)
        assertEquals(2, preview.totalPages)
    }

    @Test
    fun `삭제되지 않은 관리자 답변이 있는 고민글만 고른다`() {
        // given
        val answered = appendConcern(ConcernCategory.ETC)
        val deletedAnswer = appendConcern(ConcernCategory.ETC)
        val userOnly = appendConcern(ConcernCategory.ETC)
        appendComment(answered, official = true)
        commentRemover.remove(appendComment(deletedAnswer, official = true), NOW)
        appendComment(userOnly, official = false)
        entityManager.flush()

        // when
        val result = commentReader.readConcernIdsWithOfficialComment(listOf(answered, deletedAnswer, userOnly))

        // then
        assertEquals(setOf(answered), result)
    }

    @Test
    fun `도움돼요는 반복해 눌러도 한 번으로 세고 취소 후 다시 누르면 되살린다`() {
        // given
        val commentId = 9_001L

        // when
        voteManager.vote(commentId, USER_ID, NOW)
        voteManager.vote(commentId, USER_ID, NOW)
        voteManager.vote(commentId, OTHER_USER_ID, NOW)
        voteManager.cancel(commentId, OTHER_USER_ID, NOW)
        voteManager.cancel(commentId, OTHER_USER_ID, NOW)
        voteManager.cancel(commentId, USER_ID, NOW)
        voteManager.vote(commentId, USER_ID, NOW)

        // then
        assertEquals(mapOf(commentId to 1L), voteReader.countAll(listOf(commentId)))
        assertEquals(setOf(commentId), voteReader.readVotedCommentIds(USER_ID, listOf(commentId)))
        assertEquals(emptySet<Long>(), voteReader.readVotedCommentIds(OTHER_USER_ID, listOf(commentId)))
    }

    private fun appendConcern(category: ConcernCategory): Long = checkNotNull(
        concernAppender.append(
            ConcernAppendDto(authorUserId = USER_ID, category = category, title = "제목", content = "본문"),
        ).id,
    )

    private fun appendComment(concernId: Long, parentId: Long? = null, official: Boolean): ConcernComment =
        commentAppender.append(
            ConcernCommentAppendDto(
                concernId = concernId,
                parentId = parentId,
                userId = USER_ID,
                content = "답변입니다.",
                official = official,
            ),
        )

    companion object {
        private const val USER_ID = 17L
        private const val OTHER_USER_ID = 18L
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 8, 12, 0)
    }
}
