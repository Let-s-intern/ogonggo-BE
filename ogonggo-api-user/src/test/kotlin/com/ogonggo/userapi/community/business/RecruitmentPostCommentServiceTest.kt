package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.domain.Post
import com.ogonggo.core.community.domain.RecruitmentPostComment
import com.ogonggo.core.community.implement.PostReader
import com.ogonggo.core.community.implement.RecruitmentPostCommentAppender
import com.ogonggo.core.community.implement.RecruitmentPostCommentAppendCommand
import com.ogonggo.core.community.implement.RecruitmentPostCommentReader
import com.ogonggo.core.error.BusinessException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime

class RecruitmentPostCommentServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val postReader = Mockito.mock(PostReader::class.java)
    private val commentReader = Mockito.mock(RecruitmentPostCommentReader::class.java)
    private val commentAppender = Mockito.mock(RecruitmentPostCommentAppender::class.java)
    private val service = RecruitmentPostCommentService(
        userReader,
        postReader,
        commentReader,
        commentAppender,
    )

    @Test
    fun `활성 사용자가 모집글에 댓글을 작성하면 댓글 식별자를 반환한다`() {
        // given
        val command = command()
        val post = Mockito.mock(Post::class.java)
        val savedComment = Mockito.mock(RecruitmentPostComment::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readPublished(POST_ID)).thenReturn(post)
        Mockito.`when`(commentAppender.append(normalizedCommand(post))).thenReturn(savedComment)
        Mockito.`when`(savedComment.id).thenReturn(COMMENT_ID)

        // when
        val commentId = service.create(USER_ID, POST_ID, command)

        // then
        assertEquals(COMMENT_ID, commentId)
        Mockito.verify(postReader).readPublished(POST_ID)
        Mockito.verify(commentAppender).append(normalizedCommand(post))
    }

    @Test
    fun `대댓글을 작성할 때 부모 댓글이 같은 모집글인지 검증한다`() {
        // given
        val post = Mockito.mock(Post::class.java)
        val otherPost = Mockito.mock(Post::class.java)
        Mockito.`when`(post.id).thenReturn(POST_ID)
        Mockito.`when`(otherPost.id).thenReturn(44L)
        val parent = RecruitmentPostComment.create(
            post = otherPost,
            parent = null,
            userId = 2L,
            content = "다른 모집글의 댓글입니다.",
        )
        val command = command(parentId = 101L)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readPublished(POST_ID)).thenReturn(post)
        Mockito.`when`(commentReader.read(101L)).thenReturn(parent)

        // when
        val exception = assertThrows(BusinessException::class.java) {
            service.create(USER_ID, POST_ID, command)
        }

        // then
        assertEquals("RECRUITMENT_POST_COMMENT_PARENT_TARGET_MISMATCH", exception.errorCode.code)
        Mockito.verifyNoInteractions(commentAppender)
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

    private fun normalizedCommand(post: Post) = RecruitmentPostCommentAppendCommand(
        post = post,
        parent = null,
        userId = USER_ID,
        content = "참여하고 싶습니다.",
    )

    companion object {
        private const val USER_ID = 17L
        private const val POST_ID = 12L
        private const val COMMENT_ID = 101L
    }
}
