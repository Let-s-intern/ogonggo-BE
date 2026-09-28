package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.domain.RecruitmentApplicationProgressStatus
import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.error.RecruitmentPostApplicationErrorCode
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.community.implement.PostMetricManager
import com.ogonggo.core.community.implement.PostMetricReader
import com.ogonggo.core.community.implement.RecruitmentPostApplicationManager
import com.ogonggo.core.community.implement.RecruitmentPostApplicationReader
import com.ogonggo.core.community.implement.RecruitmentPostBookmarkManager
import com.ogonggo.core.community.implement.RecruitmentPostBookmarkReader
import com.ogonggo.core.community.implement.RecruitmentPostReader
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class RecruitmentPostBookmarkServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val postReader = Mockito.mock(RecruitmentPostReader::class.java)
    private val bookmarkReader = Mockito.mock(RecruitmentPostBookmarkReader::class.java)
    private val bookmarkManager = Mockito.mock(RecruitmentPostBookmarkManager::class.java)
    private val postMetricManager = Mockito.mock(PostMetricManager::class.java)
    private val postMetricReader = Mockito.mock(PostMetricReader::class.java)
    private val userProfileReader = Mockito.mock(UserProfileReader::class.java)
    private val applicationReader = Mockito.mock(RecruitmentPostApplicationReader::class.java)
    private val applicationManager = Mockito.mock(RecruitmentPostApplicationManager::class.java)
    private val service = RecruitmentPostBookmarkService(
        userReader,
        postReader,
        bookmarkReader,
        bookmarkManager,
        postMetricManager,
        postMetricReader,
        Clock.fixed(Instant.parse("2026-09-15T00:00:00Z"), ZONE),
        userProfileReader,
        applicationReader,
        applicationManager,
    )

    @Test
    fun `정지 사용자는 북마크를 등록할 수 없다`() {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(user(UserStatus.SUSPENDED))

        val exception = assertThrows(ForbiddenException::class.java) {
            service.addBookmark(USER_ID, POST_ID)
        }

        assertEquals(UserErrorCode.USER_SUSPENDED, exception.errorCode)
        Mockito.verifyNoInteractions(postReader, bookmarkManager)
    }

    @Test
    fun `북마크가 등록되면 같은 요청에서 북마크 수를 증가시킨다`() {
        // given
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(user(UserStatus.ACTIVE))
        Mockito.`when`(postReader.readPublished(POST_ID)).thenReturn(Mockito.mock(RecruitmentPost::class.java))
        Mockito.`when`(bookmarkManager.append(USER_ID, POST_ID, NOW)).thenReturn(true)

        // when
        service.addBookmark(USER_ID, POST_ID)

        // then
        Mockito.verify(bookmarkManager).append(USER_ID, POST_ID, NOW)
        Mockito.verify(postMetricManager).increaseBookmarkCount(POST_ID, NOW)
    }

    @Test
    fun `탈퇴 사용자는 북마크 목록을 조회할 수 없다`() {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(user(UserStatus.WITHDRAWN))

        val exception = assertThrows(ForbiddenException::class.java) {
            service.getBookmarks(USER_ID, page = 0, size = 10)
        }

        assertEquals(UserErrorCode.USER_WITHDRAWN, exception.errorCode)
        Mockito.verifyNoInteractions(bookmarkReader)
    }

    @Test
    fun `비공개 또는 삭제된 모집글도 북마크를 해제할 수 있다`() {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(user(UserStatus.ACTIVE))
        Mockito.`when`(postReader.readIncludingDeleted(POST_ID)).thenReturn(Mockito.mock(RecruitmentPost::class.java))
        Mockito.`when`(bookmarkManager.delete(USER_ID, POST_ID, NOW)).thenReturn(true)

        service.deleteBookmark(USER_ID, POST_ID)

        Mockito.verify(postReader).readIncludingDeleted(POST_ID)
        Mockito.verify(bookmarkManager).delete(USER_ID, POST_ID, NOW)
        Mockito.verify(postMetricManager).decreaseBookmarkCount(POST_ID, NOW)
    }

    @Test
    fun `이미 해제된 북마크를 다시 해제해도 북마크 수는 감소하지 않는다`() {
        // given
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(user(UserStatus.ACTIVE))
        Mockito.`when`(postReader.readIncludingDeleted(POST_ID)).thenReturn(Mockito.mock(RecruitmentPost::class.java))
        Mockito.`when`(bookmarkManager.delete(USER_ID, POST_ID, NOW)).thenReturn(false)

        // when
        service.deleteBookmark(USER_ID, POST_ID)

        // then
        Mockito.verifyNoInteractions(postMetricManager)
    }

    @Test
    fun `스크랩한 모집글을 지원 준비 중으로 옮기면 북마크를 해제하고 지원 이력을 만든다`() {
        // given
        givenActivePublishedPost()
        Mockito.`when`(bookmarkManager.delete(USER_ID, POST_ID, NOW)).thenReturn(true)

        // when
        service.prepare(USER_ID, POST_ID)

        // then
        Mockito.verify(postMetricManager).decreaseBookmarkCount(POST_ID, NOW)
        Mockito.verify(applicationManager).startPreparation(POST_ID, USER_ID, NOW)
    }

    @Test
    fun `이미 지원 준비 중이면 북마크만 정리하고 지원 이력은 그대로 둔다`() {
        // given
        givenActivePublishedPost()
        Mockito.`when`(applicationReader.readActiveStatus(POST_ID, USER_ID))
            .thenReturn(RecruitmentApplicationProgressStatus.PREPARING)
        Mockito.`when`(bookmarkManager.delete(USER_ID, POST_ID, NOW)).thenReturn(false)

        // when
        service.prepare(USER_ID, POST_ID)

        // then
        Mockito.verifyNoInteractions(applicationManager, postMetricManager)
    }

    @Test
    fun `북마크도 지원 이력도 없으면 지원 준비 중으로 옮기지 못한다`() {
        // given
        givenActivePublishedPost()
        Mockito.`when`(bookmarkManager.delete(USER_ID, POST_ID, NOW)).thenReturn(false)

        // when
        val exception = assertThrows(EntityNotFoundException::class.java) { service.prepare(USER_ID, POST_ID) }

        // then
        assertEquals(RecruitmentPostErrorCode.RECRUITMENT_POST_BOOKMARK_NOT_FOUND, exception.errorCode)
        Mockito.verifyNoInteractions(applicationManager)
    }

    @Test
    fun `지원 완료 이후 단계의 모집글은 지원 준비 중이나 스크랩으로 옮기지 못한다`() {
        // given
        givenActivePublishedPost()
        Mockito.`when`(applicationReader.readActiveStatus(POST_ID, USER_ID))
            .thenReturn(RecruitmentApplicationProgressStatus.COMPLETED)

        // when
        val prepare = assertThrows(ConflictException::class.java) { service.prepare(USER_ID, POST_ID) }
        val cancel = assertThrows(ConflictException::class.java) { service.cancelPreparation(USER_ID, POST_ID) }

        // then
        listOf(prepare, cancel).forEach {
            assertEquals(RecruitmentPostApplicationErrorCode.INVALID_RECRUITMENT_APPLICATION_STATUS_TRANSITION, it.errorCode)
        }
        Mockito.verifyNoInteractions(bookmarkManager, applicationManager)
    }

    @Test
    fun `지원 준비 중을 스크랩으로 되돌리면 지원 이력을 지우고 다시 북마크한다`() {
        // given
        givenActivePublishedPost()
        Mockito.`when`(applicationReader.readActiveStatus(POST_ID, USER_ID))
            .thenReturn(RecruitmentApplicationProgressStatus.PREPARING)
        Mockito.`when`(bookmarkManager.append(USER_ID, POST_ID, NOW)).thenReturn(true)

        // when
        service.cancelPreparation(USER_ID, POST_ID)

        // then
        Mockito.verify(applicationManager).delete(POST_ID, USER_ID, NOW)
        Mockito.verify(postMetricManager).increaseBookmarkCount(POST_ID, NOW)
    }

    @Test
    fun `이미 스크랩 칸에만 있으면 되돌려도 아무것도 바꾸지 않는다`() {
        // given
        givenActivePublishedPost()
        Mockito.`when`(bookmarkReader.isBookmarked(USER_ID, POST_ID)).thenReturn(true)

        // when
        service.cancelPreparation(USER_ID, POST_ID)

        // then
        Mockito.verifyNoInteractions(bookmarkManager, applicationManager, postMetricManager)
    }

    private fun givenActivePublishedPost() {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(user(UserStatus.ACTIVE))
        Mockito.`when`(postReader.readPublished(POST_ID)).thenReturn(Mockito.mock(RecruitmentPost::class.java))
    }

    private fun user(status: UserStatus) = UserAccountDto(
        userId = USER_ID,
        letsCareerUserId = 100L,
        email = null,
        status = status,
        role = UserRole.USER,
        joinedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
    )

    companion object {
        private const val USER_ID = 17L
        private const val POST_ID = 12L
        private val ZONE = ZoneId.of("Asia/Seoul")
        private val NOW = LocalDateTime.of(2026, 9, 15, 9, 0)
    }
}
