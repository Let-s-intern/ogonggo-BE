package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.community.implement.RecruitmentPostApplicationItem
import com.ogonggo.core.community.implement.RecruitmentPostApplicationPage
import com.ogonggo.core.community.implement.RecruitmentPostApplicationReader
import com.ogonggo.core.community.implement.RecruitmentPostApplicationManager
import com.ogonggo.core.community.implement.RecruitmentPostReader
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RecruitmentPostApplicationServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val postReader = Mockito.mock(RecruitmentPostReader::class.java)
    private val applicationManager = Mockito.mock(RecruitmentPostApplicationManager::class.java)
    private val applicationReader = Mockito.mock(RecruitmentPostApplicationReader::class.java)
    private val userProfileReader = Mockito.mock(UserProfileReader::class.java)
    private val service = RecruitmentPostApplicationService(
        userReader = userReader,
        postReader = postReader,
        applicationManager = applicationManager,
        applicationReader = applicationReader,
        userProfileReader = userProfileReader,
        clock = Clock.fixed(Instant.parse("2026-09-16T00:00:00Z"), ZONE),
    )

    @Test
    fun `모집 중인 글의 지원 연락처를 반환하고 이력을 기록한다`() {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(user(UserStatus.ACTIVE))
        val post = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(post.recruitmentStatus).thenReturn(RecruitmentStatus.RECRUITING)
        Mockito.`when`(post.contactMethod).thenReturn(ContactMethod.EMAIL)
        Mockito.`when`(post.contactValue).thenReturn("team@example.com")
        Mockito.`when`(postReader.readPublishedForUpdate(POST_ID)).thenReturn(post)

        val result = service.createApplication(USER_ID, POST_ID)

        assertEquals(POST_ID, result.postId)
        assertEquals(ContactMethod.EMAIL, result.contactMethod)
        assertEquals("team@example.com", result.contactValue)
        assertEquals(NOW, result.clickedAt)
        Mockito.verify(applicationManager).recordClick(POST_ID, USER_ID, NOW)
    }

    @Test
    fun `마감된 글은 지원 이력을 기록하지 않는다`() {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(user(UserStatus.ACTIVE))
        val post = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(post.recruitmentStatus).thenReturn(RecruitmentStatus.CLOSED)
        Mockito.`when`(postReader.readPublishedForUpdate(POST_ID)).thenReturn(post)

        val exception = assertThrows(ConflictException::class.java) {
            service.createApplication(USER_ID, POST_ID)
        }

        assertEquals(RecruitmentPostErrorCode.RECRUITMENT_POST_CLOSED, exception.errorCode)
        Mockito.verifyNoInteractions(applicationManager)
    }

    @Test
    fun `정지 사용자는 지원 이력을 만들 수 없다`() {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(user(UserStatus.SUSPENDED))

        val exception = assertThrows(ForbiddenException::class.java) {
            service.createApplication(USER_ID, POST_ID)
        }

        assertEquals(UserErrorCode.USER_SUSPENDED, exception.errorCode)
        Mockito.verifyNoInteractions(postReader, applicationManager)
    }

    @Test
    fun `목록 작성자 프로필을 한 번에 조회하고 없으면 null로 반환한다`() {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(user(UserStatus.ACTIVE))
        Mockito.`when`(applicationReader.readPage(USER_ID, null, null, null, 0, 10)).thenReturn(
            RecruitmentPostApplicationPage(
                items = listOf(
                    RecruitmentPostApplicationItem(
                        postId = POST_ID,
                        title = "Kotlin 팀원 모집",
                        recruitmentType = RecruitmentType.SIDE_PROJECT,
                        recruitmentStatus = RecruitmentStatus.RECRUITING,
                        recruitmentEndDate = LocalDate.of(2026, 9, 30),
                        lastClickedAt = NOW,
                        authorUserId = AUTHOR_ID,
                    ),
                ),
                page = 0,
                size = 10,
                totalElements = 1,
                totalPages = 1,
            ),
        )
        Mockito.`when`(userProfileReader.readAll(listOf(AUTHOR_ID))).thenReturn(emptyMap())

        val result = service.getApplications(USER_ID, null, null, null, page = 0, size = 10)

        assertEquals(AUTHOR_ID, result.items.single().author.userId)
        assertNull(result.items.single().author.nickname)
        assertNull(result.items.single().author.profileImageUrl)
        Mockito.verify(userProfileReader).readAll(listOf(AUTHOR_ID))
    }

    private fun user(status: UserStatus) = UserAccountDto(
        userId = USER_ID,
        letsCareerUserId = 100L,
        email = null,
        status = status,
        role = UserRole.USER,
        joinedAt = NOW,
    )

    companion object {
        private const val USER_ID = 17L
        private const val POST_ID = 12L
        private const val AUTHOR_ID = 33L
        private val ZONE = ZoneId.of("Asia/Seoul")
        private val NOW = LocalDateTime.of(2026, 9, 16, 9, 0)
    }
}
