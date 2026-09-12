package com.ogonggo.core.community.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("모집글 댓글 도메인")
class RecruitmentPostCommentDomainTest {

    @Test
    @DisplayName("댓글을 생성하면 모집글과 작성자 및 내용을 보존한다")
    fun createComment() {
        // given
        val post = postFixture()

        // when
        val comment = RecruitmentPostComment.create(
            post = post,
            parent = null,
            userId = 17L,
            content = "참여하고 싶습니다.",
        )

        // then
        assertEquals(post, comment.post)
        assertEquals(17L, comment.userId)
        assertEquals("참여하고 싶습니다.", comment.content)
        assertFalse(comment.isReply())
        assertFalse(comment.isDeleted())
    }

    @Test
    @DisplayName("공백인 댓글은 생성할 수 없다")
    fun rejectBlankContent() {
        // given
        val post = postFixture()

        // when
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RecruitmentPostComment.create(
                post = post,
                parent = null,
                userId = 17L,
                content = "   ",
            )
        }

        // then
        assertEquals("댓글 내용은 공백일 수 없습니다.", exception.message)
    }

    @Test
    @DisplayName("댓글을 삭제하면 최초 삭제 시각을 유지한다")
    fun deleteComment() {
        // given
        val comment = RecruitmentPostComment.create(
            post = postFixture(),
            parent = null,
            userId = 17L,
            content = "참여하고 싶습니다.",
        )
        val firstDeletedAt = LocalDateTime.of(2026, 9, 12, 10, 0)

        // when
        comment.delete(firstDeletedAt)
        comment.delete(firstDeletedAt.plusHours(1))

        // then
        assertTrue(comment.isDeleted())
        assertEquals(firstDeletedAt, comment.deletedAt)
    }

    @Test
    @DisplayName("부모 댓글을 지정하면 대댓글로 생성한다")
    fun createReply() {
        // given
        val post = postFixture()
        val parent = RecruitmentPostComment.create(
            post = post,
            parent = null,
            userId = 1L,
            content = "질문이 있습니다.",
        )

        // when
        val reply = RecruitmentPostComment.create(
            post = post,
            parent = parent,
            userId = 17L,
            content = "답변드립니다.",
        )

        // then
        assertTrue(reply.isReply())
        assertEquals(parent, reply.parent)
    }

    private fun postFixture(): Post = Post(
        authorUserId = 1L,
        title = "사이드 프로젝트 팀원 모집",
        recruitmentType = RecruitmentType.SIDE_PROJECT,
        capacity = 4,
        progressMethod = ProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin", "Spring"),
        summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
        content = "<p>모집 상세 내용입니다.</p>",
        eligibilityAndSelectionProcess = null,
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        positions = listOf(RecruitmentPosition.BACKEND),
        contactMethod = ContactMethod.EMAIL,
        contactValue = "team@example.com",
    )
}
