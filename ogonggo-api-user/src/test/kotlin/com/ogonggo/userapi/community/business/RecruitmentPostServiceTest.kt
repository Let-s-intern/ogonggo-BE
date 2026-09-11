package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.Post
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.implement.PostAppendCommand
import com.ogonggo.core.community.implement.PostAppender
import com.ogonggo.core.community.implement.PostReader
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.LocalDateTime

class RecruitmentPostServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val postAppender = Mockito.mock(PostAppender::class.java)
    private val postReader = Mockito.mock(PostReader::class.java)
    private val service = RecruitmentPostService(userReader, postAppender, postReader)

    @Test
    fun `공개 모집글 상세 조회 결과를 변환한다`() {
        val post = Mockito.mock(Post::class.java)
        Mockito.`when`(postReader.readPublished(12L)).thenReturn(post)
        Mockito.`when`(post.id).thenReturn(12L)
        Mockito.`when`(post.authorUserId).thenReturn(USER_ID)
        Mockito.`when`(post.title).thenReturn("스터디 모집")
        Mockito.`when`(post.recruitmentType).thenReturn(RecruitmentType.STUDY)
        Mockito.`when`(post.recruitmentStatus).thenReturn(RecruitmentStatus.RECRUITING)
        Mockito.`when`(post.recruitmentStartDate).thenReturn(LocalDate.of(2026, 9, 1))
        Mockito.`when`(post.recruitmentEndDate).thenReturn(LocalDate.of(2026, 9, 30))
        Mockito.`when`(post.progressMethod).thenReturn(ProgressMethod.ONLINE)
        Mockito.`when`(post.capacity).thenReturn(6)
        Mockito.`when`(post.activityDurationMonths).thenReturn(3)
        Mockito.`when`(post.technologyStacks).thenReturn(mutableListOf("Kotlin", "Spring"))
        Mockito.`when`(post.positions).thenReturn(mutableListOf(RecruitmentPosition.BACKEND))
        Mockito.`when`(post.contactMethod).thenReturn(ContactMethod.EMAIL)
        Mockito.`when`(post.contactValue).thenReturn("team@example.com")
        Mockito.`when`(post.summary).thenReturn("함께 공부할 분을 모집합니다.")
        Mockito.`when`(post.content).thenReturn("<p>상세 내용</p>")
        Mockito.`when`(post.eligibilityAndSelectionProcess).thenReturn(null)

        val result = service.getRecruitmentPost(12L)

        assertEquals(12L, result.id)
        assertEquals(USER_ID, result.author.userId)
        assertEquals(listOf(RecruitmentPosition.BACKEND), result.positions)
        assertEquals(ContactMethod.EMAIL, result.contact.method)
        assertEquals("<p>상세 내용</p>", result.content)
        Mockito.verify(postReader).readPublished(12L)
    }

    @Test
    fun `활성 사용자가 생성한 모집글의 식별자를 반환한다`() {
        val command = createCommand(authorUserId = 999L)
        val savedPost = Mockito.mock(Post::class.java)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postAppender.append(command.copy(authorUserId = USER_ID))).thenReturn(savedPost)
        Mockito.`when`(savedPost.id).thenReturn(12L)

        val postId = service.create(USER_ID, command)

        assertEquals(12L, postId)
        Mockito.verify(postAppender).append(command.copy(authorUserId = USER_ID))
    }

    private fun activeUser(): UserAccountDto = UserAccountDto(
        userId = USER_ID,
        letsCareerUserId = 100L,
        email = null,
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
        joinedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
    )

    private fun createCommand(authorUserId: Long): PostAppendCommand = PostAppendCommand(
        authorUserId = authorUserId,
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

    companion object {
        private const val USER_ID = 17L
    }
}
