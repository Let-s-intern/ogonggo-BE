package com.ogonggo.core.community.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.persistence.PostJpaRepository
import com.ogonggo.core.community.persistence.RecruitmentPostCommentJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(PostAppenderImpl::class, RecruitmentPostCommentAppenderImpl::class)
internal class RecruitmentPostCommentImplementPersistenceTest @Autowired constructor(
    private val postAppender: PostAppender,
    private val commentAppender: RecruitmentPostCommentAppender,
    private val commentRepository: RecruitmentPostCommentJpaRepository,
    private val postRepository: PostJpaRepository,
) {

    @Test
    fun `모집글과 댓글을 post_id 외래키로 함께 저장한다`() {
        // given
        val post = postAppender.append(postCommand())
        val command = RecruitmentPostCommentAppendCommand(
            post = post,
            parent = null,
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
        assertEquals(checkNotNull(post.id), checkNotNull(reloadedComment.post.id))
        assertEquals(17L, reloadedComment.userId)
        assertEquals("참여하고 싶습니다.", reloadedComment.content)
        assertEquals(1, postRepository.count())
    }

    private fun postCommand() = PostAppendCommand(
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
