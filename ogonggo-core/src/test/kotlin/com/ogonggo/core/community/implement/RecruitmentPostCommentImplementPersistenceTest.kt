package com.ogonggo.core.community.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.persistence.RecruitmentPostJpaRepository
import com.ogonggo.core.community.persistence.RecruitmentPostCommentJpaRepository
import com.ogonggo.core.community.persistence.RecruitmentPostCommentReportJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import jakarta.persistence.EntityManager
import java.time.LocalDate

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    RecruitmentPostAppender::class,
    RecruitmentPostCommentAppender::class,
    RecruitmentPostCommentRemover::class,
    RecruitmentPostCommentReader::class,
    RecruitmentPostCommentReportAppender::class,
)
internal class RecruitmentPostCommentImplementPersistenceTest @Autowired constructor(
    private val postAppender: RecruitmentPostAppender,
    private val commentAppender: RecruitmentPostCommentAppender,
    private val commentRemover: RecruitmentPostCommentRemover,
    private val commentReader: RecruitmentPostCommentReader,
    private val commentRepository: RecruitmentPostCommentJpaRepository,
    private val reportAppender: RecruitmentPostCommentReportAppender,
    private val reportRepository: RecruitmentPostCommentReportJpaRepository,
    private val postRepository: RecruitmentPostJpaRepository,
    private val entityManager: EntityManager,
) {

    @Test
    fun `모집글과 댓글을 post_id 외래키로 함께 저장한다`() {
        // given
        val post = postAppender.append(postCommand())
        val command = RecruitmentPostCommentAppendCommand(
            postId = checkNotNull(post.id),
            parentId = null,
            userId = 17L,
            content = "참여하고 싶습니다.",
        )

        // when
        val savedComment = commentAppender.append(command)
        val commentId = checkNotNull(savedComment.id)
        commentRepository.flush()
        val reloadedComment = commentRepository.findById(commentId).orElseThrow()

        // then
        assertNotNull(savedComment.createdAt)
        assertEquals(checkNotNull(post.id), reloadedComment.postId)
        assertEquals(17L, reloadedComment.userId)
        assertEquals("참여하고 싶습니다.", reloadedComment.content)
        assertEquals(1, postRepository.count())
    }

    @Test
    fun `부모 댓글과 대댓글을 페이지로 조회하고 대댓글 미리보기는 5개에서 끊는다`() {
        // given
        val post = postAppender.append(postCommand())
        val root = commentAppender.append(
            RecruitmentPostCommentAppendCommand(
                postId = checkNotNull(post.id),
                parentId = null,
                userId = 17L,
                content = "부모 댓글입니다.",
            ),
        )
        commentAppender.append(
            RecruitmentPostCommentAppendCommand(
                postId = checkNotNull(post.id),
                parentId = null,
                userId = 18L,
                content = "두 번째 부모 댓글입니다.",
            ),
        )
        repeat(6) { index ->
            commentAppender.append(
                RecruitmentPostCommentAppendCommand(
                    postId = checkNotNull(post.id),
                    parentId = checkNotNull(root.id),
                    userId = 19L,
                    content = "대댓글 $index",
                ),
            )
        }
        commentRepository.flush()
        val postId = checkNotNull(post.id)
        val rootId = checkNotNull(root.id)

        // when
        val rootPage = commentReader.readRootPage(
            postId = postId,
            page = 0,
            size = 1,
        )
        val previews = commentReader.readReplyPreviews(
            postId = postId,
            parentIds = listOf(rootId),
            size = 5,
        )
        val replies = commentReader.readReplyPage(
            postId = postId,
            parentId = rootId,
            page = 0,
            size = 5,
        )
        val nextRootPage = commentReader.readRootPage(
            postId = postId,
            page = 1,
            size = 1,
        )
        val nextReplies = commentReader.readReplyPage(
            postId = postId,
            parentId = rootId,
            page = 1,
            size = 5,
        )

        // then
        assertEquals(1, rootPage.comments.size)
        assertEquals(0, rootPage.page)
        assertEquals(2, rootPage.totalElements)
        assertEquals(2, rootPage.totalPages)
        assertEquals(5, previews.getValue(rootId).comments.size)
        assertEquals(6, previews.getValue(rootId).totalElements)
        assertEquals(2, previews.getValue(rootId).totalPages)
        assertEquals(5, replies.comments.size)
        assertEquals(0, replies.page)
        assertEquals(6, replies.totalElements)
        assertEquals(2, replies.totalPages)
        assertEquals(1, nextRootPage.comments.size)
        assertEquals(1, nextRootPage.page)
        assertEquals(2, nextRootPage.totalPages)
        assertEquals(1, nextReplies.comments.size)
        assertEquals(1, nextReplies.page)
        assertEquals(2, nextReplies.totalPages)
        assertEquals(8, commentRepository.count())
    }

    @Test
    fun `부모 댓글을 물리 삭제하면 대댓글도 함께 삭제한다`() {
        // given
        val post = postAppender.append(postCommand())
        val parent = commentAppender.append(
            RecruitmentPostCommentAppendCommand(
                postId = checkNotNull(post.id),
                parentId = null,
                userId = 17L,
                content = "부모 댓글입니다.",
            ),
        )
        val reply = commentAppender.append(
            RecruitmentPostCommentAppendCommand(
                postId = checkNotNull(post.id),
                parentId = checkNotNull(parent.id),
                userId = 18L,
                content = "대댓글입니다.",
            ),
        )
        commentRepository.flush()
        val postId = checkNotNull(post.id)
        val parentId = checkNotNull(parent.id)
        val replyId = checkNotNull(reply.id)

        // when
        commentRemover.remove(parent)
        commentRepository.flush()
        entityManager.clear()

        // then
        assertEquals(false, commentRepository.findById(parentId).isPresent)
        assertEquals(false, commentRepository.findById(replyId).isPresent)
        assertEquals(true, commentReader.readRootPage(postId, page = 0, size = 10).comments.isEmpty())
        assertThrows(EntityNotFoundException::class.java) {
            commentReader.readInPost(postId, parentId)
        }
    }

    @Test
    fun `대댓글을 물리 삭제해도 부모 댓글은 유지한다`() {
        // given
        val post = postAppender.append(postCommand())
        val parent = commentAppender.append(
            RecruitmentPostCommentAppendCommand(
                postId = checkNotNull(post.id),
                parentId = null,
                userId = 17L,
                content = "부모 댓글입니다.",
            ),
        )
        val reply = commentAppender.append(
            RecruitmentPostCommentAppendCommand(
                postId = checkNotNull(post.id),
                parentId = checkNotNull(parent.id),
                userId = 18L,
                content = "대댓글입니다.",
            ),
        )
        commentRepository.flush()
        val parentId = checkNotNull(parent.id)
        val replyId = checkNotNull(reply.id)

        // when
        commentRemover.remove(reply)
        commentRepository.flush()
        entityManager.clear()

        // then
        assertEquals(true, commentRepository.findById(parentId).isPresent)
        assertEquals(false, commentRepository.findById(replyId).isPresent)
    }

    @Test
    fun `동일 사용자가 같은 댓글을 중복 신고할 수 있다`() {
        val post = postAppender.append(postCommand())
        val comment = commentAppender.append(
            RecruitmentPostCommentAppendCommand(
                postId = checkNotNull(post.id),
                parentId = null,
                userId = 17L,
                content = "신고 대상 댓글입니다.",
            ),
        )
        val commentId = checkNotNull(comment.id)

        reportAppender.append(
            RecruitmentPostCommentReportAppendCommand(
                commentId = commentId,
                userId = 17L,
                reason = null,
            ),
        )
        reportAppender.append(
            RecruitmentPostCommentReportAppendCommand(
                commentId = commentId,
                userId = 17L,
                reason = "반복 신고",
            ),
        )
        reportRepository.flush()

        assertEquals(2L, reportRepository.count())
    }

    private fun postCommand() = RecruitmentPostAppendCommand(
        authorUserId = 1L,
        title = "사이드 프로젝트 팀원 모집",
        recruitmentType = RecruitmentType.SIDE_PROJECT,
        capacity = 4,
        progressMethod = ProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin", "Spring"),
        summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
        content = "{\"root\":{\"children\":[]}}",
        eligibilityAndSelectionProcess = null,
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        positions = listOf(RecruitmentPosition.BACKEND),
        contactMethod = ContactMethod.EMAIL,
        contactValue = "team@example.com",
    )
}
