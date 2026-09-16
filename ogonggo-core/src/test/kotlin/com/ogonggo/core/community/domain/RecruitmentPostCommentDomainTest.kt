package com.ogonggo.core.community.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("모집글 댓글 도메인")
class RecruitmentPostCommentDomainTest {

    @Test
    @DisplayName("댓글을 생성하면 모집글과 작성자 및 내용을 보존한다")
    fun createComment() {
        // given
        // when
        val comment = RecruitmentPostComment.create(
            postId = 1L,
            parentId = null,
            userId = 17L,
            content = "참여하고 싶습니다.",
        )

        // then
        assertEquals(1L, comment.postId)
        assertEquals(17L, comment.userId)
        assertEquals("참여하고 싶습니다.", comment.content)
        assertFalse(comment.isReply())
    }

    @Test
    @DisplayName("공백인 댓글은 생성할 수 없다")
    fun rejectBlankContent() {
        // given
        // when
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RecruitmentPostComment.create(
                postId = 1L,
                parentId = null,
                userId = 17L,
                content = "   ",
            )
        }

        // then
        assertEquals("댓글 내용은 공백일 수 없습니다.", exception.message)
    }

    @Test
    @DisplayName("부모 댓글을 지정하면 대댓글로 생성한다")
    fun createReply() {
        // given
        // when
        val reply = RecruitmentPostComment.create(
            postId = 1L,
            parentId = 2L,
            userId = 17L,
            content = "답변드립니다.",
        )

        // then
        assertTrue(reply.isReply())
        assertEquals(2L, reply.parentId)
    }

    private fun postFixture(): RecruitmentPost = RecruitmentPost(
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
