package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.RecruitmentPostComment
import com.ogonggo.core.community.implement.RecruitmentPostReader
import com.ogonggo.core.community.implement.PostMetricManager
import com.ogonggo.core.community.implement.RecruitmentPostCommentAppender
import com.ogonggo.core.community.implement.RecruitmentPostCommentAppendCommand
import com.ogonggo.core.community.implement.RecruitmentPostCommentReader
import com.ogonggo.core.community.implement.RecruitmentPostCommentRemover
import com.ogonggo.core.community.implement.RecruitmentPostCommentReportAppender
import com.ogonggo.core.community.implement.RecruitmentPostCommentReportAppendCommand
import com.ogonggo.core.error.BusinessException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class RecruitmentPostCommentServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val postReader = Mockito.mock(RecruitmentPostReader::class.java)
    private val commentReader = Mockito.mock(RecruitmentPostCommentReader::class.java)
    private val commentAppender = Mockito.mock(RecruitmentPostCommentAppender::class.java)
    private val commentRemover = Mockito.mock(RecruitmentPostCommentRemover::class.java)
    private val userProfileReader = Mockito.mock(UserProfileReader::class.java)
    private val postMetricManager = Mockito.mock(PostMetricManager::class.java)
    private val reportAppender = Mockito.mock(RecruitmentPostCommentReportAppender::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZONE)
    private val service = RecruitmentPostCommentService(
        userReader,
        postReader,
        commentReader,
        commentAppender,
        commentRemover,
        userProfileReader,
        postMetricManager,
        reportAppender,
        clock,
    )

    @Test
    fun `활성 사용자가 모집글에 댓글을 작성하면 댓글 식별자를 반환한다`() {
        // given
        val command = command()
        val savedComment = Mockito.mock(RecruitmentPostComment::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readPublishedForUpdate(POST_ID)).thenReturn(Mockito.mock(RecruitmentPost::class.java))
        Mockito.`when`(commentAppender.append(normalizedCommand())).thenReturn(savedComment)
        Mockito.`when`(savedComment.id).thenReturn(COMMENT_ID)

        // when
        val commentId = service.create(USER_ID, POST_ID, command)

        // then
        assertEquals(COMMENT_ID, commentId)
        Mockito.verify(postReader).readPublishedForUpdate(POST_ID)
        Mockito.verify(commentAppender).append(normalizedCommand())
        Mockito.verify(postMetricManager).increaseCommentCount(POST_ID, LocalDateTime.now(clock))
    }

    @Test
    fun `대댓글을 작성할 때 부모 댓글이 같은 모집글인지 검증한다`() {
        // given
        val parent = RecruitmentPostComment.create(
            postId = 44L,
            parentId = null,
            userId = 2L,
            content = "다른 모집글의 댓글입니다.",
        )
        val command = command(parentId = 101L)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readPublishedForUpdate(POST_ID)).thenReturn(Mockito.mock(RecruitmentPost::class.java))
        Mockito.`when`(commentReader.readForUpdate(101L)).thenReturn(parent)

        // when
        val exception = assertThrows(BusinessException::class.java) {
            service.create(USER_ID, POST_ID, command)
        }

        // then
        assertEquals("RECRUITMENT_POST_COMMENT_PARENT_TARGET_MISMATCH", exception.errorCode.code)
        Mockito.verifyNoInteractions(commentAppender)
    }

    @Test
    fun `작성자가 자신의 댓글을 삭제하면 물리 삭제를 위임한다`() {
        // given
        val comment = Mockito.mock(RecruitmentPostComment::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readPublishedForUpdate(POST_ID)).thenReturn(Mockito.mock(RecruitmentPost::class.java))
        Mockito.`when`(commentReader.readInPostForUpdate(POST_ID, COMMENT_ID)).thenReturn(comment)
        Mockito.`when`(comment.userId).thenReturn(USER_ID)
        Mockito.`when`(commentRemover.remove(comment)).thenReturn(1)

        // when
        service.delete(USER_ID, POST_ID, COMMENT_ID)

        // then
        Mockito.verify(commentRemover).remove(comment)
        Mockito.verify(postReader).readPublishedForUpdate(POST_ID)
        Mockito.verify(commentReader).readInPostForUpdate(POST_ID, COMMENT_ID)
        Mockito.verify(postMetricManager).decreaseCommentCount(POST_ID, 1, LocalDateTime.now(clock))
    }

    @Test
    fun `다른 사용자는 댓글을 삭제할 수 없다`() {
        // given
        val comment = Mockito.mock(RecruitmentPostComment::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readPublishedForUpdate(POST_ID)).thenReturn(Mockito.mock(RecruitmentPost::class.java))
        Mockito.`when`(commentReader.readInPostForUpdate(POST_ID, COMMENT_ID)).thenReturn(comment)
        Mockito.`when`(comment.userId).thenReturn(999L)

        // when
        val exception = assertThrows(BusinessException::class.java) {
            service.delete(USER_ID, POST_ID, COMMENT_ID)
        }

        // then
        assertEquals("RECRUITMENT_POST_COMMENT_PERMISSION_DENIED", exception.errorCode.code)
        Mockito.verifyNoInteractions(commentRemover)
    }

    @Test
    fun `활성 사용자가 사유 없이 댓글을 신고할 수 있다`() {
        val comment = Mockito.mock(RecruitmentPostComment::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(commentReader.readInPost(POST_ID, COMMENT_ID)).thenReturn(comment)
        Mockito.`when`(comment.id).thenReturn(COMMENT_ID)

        service.report(USER_ID, POST_ID, COMMENT_ID, reason = null)

        Mockito.verify(reportAppender).append(
            RecruitmentPostCommentReportAppendCommand(
                commentId = COMMENT_ID,
                userId = USER_ID,
                reason = null,
            ),
        )
    }

    @Test
    fun `정지 사용자는 댓글을 신고할 수 없다`() {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(
            activeUser().copy(status = UserStatus.SUSPENDED),
        )

        val exception = assertThrows(BusinessException::class.java) {
            service.report(USER_ID, POST_ID, COMMENT_ID, reason = "신고 사유")
        }

        assertEquals("USER_SUSPENDED", exception.errorCode.code)
        Mockito.verifyNoInteractions(postReader, commentReader, reportAppender)
    }

    private fun activeUser(): UserAccountDto = UserAccountDto(
        userId = USER_ID,
        letsCareerUserId = 100L,
        email = null,
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
        joinedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
    )

    private fun command(parentId: Long? = null) = CreateRecruitmentPostCommentCommand(
        parentId = parentId,
        content = "참여하고 싶습니다.",
    )

    private fun normalizedCommand() = RecruitmentPostCommentAppendCommand(
        postId = POST_ID,
        parentId = null,
        userId = USER_ID,
        content = "참여하고 싶습니다.",
    )

    companion object {
        private const val USER_ID = 17L
        private const val POST_ID = 12L
        private const val COMMENT_ID = 101L
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
