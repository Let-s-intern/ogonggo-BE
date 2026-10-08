package com.ogonggo.userapi.concern.business

import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.domain.ConcernComment
import com.ogonggo.core.concern.implement.ConcernCommentAppender
import com.ogonggo.core.concern.implement.ConcernCommentLikeManager
import com.ogonggo.core.concern.implement.ConcernCommentLikeReader
import com.ogonggo.core.concern.implement.ConcernCommentReader
import com.ogonggo.core.concern.implement.ConcernCommentRemover
import com.ogonggo.core.concern.implement.ConcernMetricManager
import com.ogonggo.core.concern.implement.ConcernReader
import com.ogonggo.core.concern.implement.dto.ConcernCommentAppendDto
import com.ogonggo.core.concern.implement.dto.ConcernCommentPageDto
import com.ogonggo.core.error.BusinessException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ConcernCommentServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val userProfileReader = Mockito.mock(UserProfileReader::class.java)
    private val concernReader = Mockito.mock(ConcernReader::class.java)
    private val concernMetricManager = Mockito.mock(ConcernMetricManager::class.java)
    private val commentReader = Mockito.mock(ConcernCommentReader::class.java)
    private val commentAppender = Mockito.mock(ConcernCommentAppender::class.java)
    private val commentRemover = Mockito.mock(ConcernCommentRemover::class.java)
    private val likeReader = Mockito.mock(ConcernCommentLikeReader::class.java)
    private val likeManager = Mockito.mock(ConcernCommentLikeManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-10-08T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val now = LocalDateTime.now(clock)
    private val service = ConcernCommentService(
        userReader,
        userProfileReader,
        concernReader,
        concernMetricManager,
        commentReader,
        commentAppender,
        commentRemover,
        likeReader,
        likeManager,
        clock,
    )

    @Test
    fun `관리자가 쓴 답변은 운영자 답변으로 남고 답변 수를 늘린다`() {
        // given
        val dto = appendDto(parentId = null, official = true)
        val saved = comment(parentId = null)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(account(UserRole.ADMIN))
        Mockito.`when`(concernReader.readForUpdate(CONCERN_ID)).thenReturn(Mockito.mock(Concern::class.java))
        Mockito.`when`(commentAppender.append(dto)).thenReturn(saved)

        // when
        val commentId = service.create(USER_ID, CONCERN_ID, CreateConcernCommentCommand(parentId = null, content = CONTENT))

        // then
        assertEquals(COMMENT_ID, commentId)
        Mockito.verify(commentAppender).append(dto)
        Mockito.verify(concernMetricManager).increaseCommentCount(CONCERN_ID, now)
    }

    @Test
    fun `답글은 답변 수에 세지 않는다`() {
        // given
        val dto = appendDto(parentId = PARENT_ID, official = false)
        val parent = comment(parentId = null)
        val saved = comment(parentId = PARENT_ID)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(account(UserRole.USER))
        Mockito.`when`(concernReader.readForUpdate(CONCERN_ID)).thenReturn(Mockito.mock(Concern::class.java))
        Mockito.`when`(commentReader.readParent(CONCERN_ID, PARENT_ID)).thenReturn(parent)
        Mockito.`when`(commentAppender.append(dto)).thenReturn(saved)

        // when
        service.create(USER_ID, CONCERN_ID, CreateConcernCommentCommand(parentId = PARENT_ID, content = CONTENT))

        // then
        Mockito.verifyNoInteractions(concernMetricManager)
    }

    @Test
    fun `답글에는 다시 답글을 달 수 없다`() {
        // given
        val reply = comment(parentId = 1L)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(account(UserRole.USER))
        Mockito.`when`(concernReader.readForUpdate(CONCERN_ID)).thenReturn(Mockito.mock(Concern::class.java))
        Mockito.`when`(commentReader.readParent(CONCERN_ID, PARENT_ID)).thenReturn(reply)

        // when
        val exception = assertThrows(BusinessException::class.java) {
            service.create(USER_ID, CONCERN_ID, CreateConcernCommentCommand(parentId = PARENT_ID, content = CONTENT))
        }

        // then
        assertEquals("CONCERN_COMMENT_NESTING_NOT_ALLOWED", exception.errorCode.code)
        Mockito.verifyNoInteractions(commentAppender)
    }

    @Test
    fun `남의 댓글은 지울 수 없다`() {
        // given
        val comment = comment(parentId = null, userId = OTHER_USER_ID)
        Mockito.`when`(concernReader.readForUpdate(CONCERN_ID)).thenReturn(Mockito.mock(Concern::class.java))
        Mockito.`when`(commentReader.readForUpdate(CONCERN_ID, COMMENT_ID)).thenReturn(comment)

        // when
        val exception = assertThrows(BusinessException::class.java) { service.delete(USER_ID, CONCERN_ID, COMMENT_ID) }

        // then
        assertEquals("CONCERN_COMMENT_PERMISSION_DENIED", exception.errorCode.code)
        Mockito.verifyNoInteractions(commentRemover)
    }

    @Test
    fun `답변을 지우면 답변 수를 줄인다`() {
        // given
        val comment = comment(parentId = null)
        Mockito.`when`(concernReader.readForUpdate(CONCERN_ID)).thenReturn(Mockito.mock(Concern::class.java))
        Mockito.`when`(commentReader.readForUpdate(CONCERN_ID, COMMENT_ID)).thenReturn(comment)

        // when
        service.delete(USER_ID, CONCERN_ID, COMMENT_ID)

        // then
        Mockito.verify(commentRemover).remove(comment, now)
        Mockito.verify(concernMetricManager).decreaseCommentCount(CONCERN_ID, now)
    }

    @Test
    fun `삭제된 답변은 삭제 문구로 바꾸고 좋아요 수와 내가 누른 여부를 채운다`() {
        // given
        val deleted = comment(parentId = null, deletedAt = now)
        Mockito.`when`(concernReader.read(CONCERN_ID)).thenReturn(Mockito.mock(Concern::class.java))
        Mockito.`when`(commentReader.readRootPage(CONCERN_ID, 0, 10))
            .thenReturn(ConcernCommentPageDto(listOf(deleted), page = 0, size = 10, totalElements = 1, totalPages = 1))
        Mockito.`when`(commentReader.readReplyPreviews(CONCERN_ID, listOf(COMMENT_ID), 5)).thenReturn(emptyMap())
        Mockito.`when`(userProfileReader.readAll(setOf(USER_ID))).thenReturn(emptyMap())
        Mockito.`when`(likeReader.countAll(listOf(COMMENT_ID))).thenReturn(mapOf(COMMENT_ID to 3L))
        Mockito.`when`(likeReader.readLikedCommentIds(USER_ID, listOf(COMMENT_ID))).thenReturn(setOf(COMMENT_ID))

        // when
        val result = service.readComments(USER_ID, CONCERN_ID, page = 0, size = 10).items.single()

        // then
        assertEquals("삭제된 댓글입니다", result.comment.content)
        assertTrue(result.comment.deleted)
        assertEquals(3L, result.comment.likeCount)
        assertTrue(result.comment.liked)
        assertEquals(0L, result.replies.totalElements)
    }

    private fun comment(parentId: Long?, userId: Long = USER_ID, deletedAt: LocalDateTime? = null): ConcernComment {
        val comment = Mockito.mock(ConcernComment::class.java)
        Mockito.`when`(comment.id).thenReturn(if (parentId == null) COMMENT_ID else 102L)
        Mockito.`when`(comment.parentId).thenReturn(parentId)
        Mockito.`when`(comment.isReply()).thenReturn(parentId != null)
        Mockito.`when`(comment.userId).thenReturn(userId)
        Mockito.`when`(comment.isWrittenBy(USER_ID)).thenReturn(userId == USER_ID)
        Mockito.`when`(comment.content).thenReturn(CONTENT)
        Mockito.`when`(comment.deletedAt).thenReturn(deletedAt)
        Mockito.`when`(comment.createdAt).thenReturn(now)
        Mockito.`when`(comment.updatedAt).thenReturn(now)
        return comment
    }

    private fun appendDto(parentId: Long?, official: Boolean) = ConcernCommentAppendDto(
        concernId = CONCERN_ID,
        parentId = parentId,
        userId = USER_ID,
        content = CONTENT,
        official = official,
    )

    private fun account(role: UserRole) = UserAccountDto(
        userId = USER_ID,
        letsCareerUserId = 100L,
        email = null,
        status = UserStatus.ACTIVE,
        role = role,
        joinedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
    )

    companion object {
        private const val USER_ID = 17L
        private const val OTHER_USER_ID = 18L
        private const val CONCERN_ID = 5L
        private const val COMMENT_ID = 101L
        private const val PARENT_ID = 90L
        private const val CONTENT = "팀마다 편차가 커요."
    }
}
