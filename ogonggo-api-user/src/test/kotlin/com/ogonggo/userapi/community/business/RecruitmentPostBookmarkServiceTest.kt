package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.implement.PostMetricReader
import com.ogonggo.core.community.implement.RecruitmentPostBookmarkManager
import com.ogonggo.core.community.implement.RecruitmentPostBookmarkReader
import com.ogonggo.core.community.implement.RecruitmentPostReader
import com.ogonggo.core.user.implement.UserProfileReader
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
    private val postMetricReader = Mockito.mock(PostMetricReader::class.java)
    private val userProfileReader = Mockito.mock(UserProfileReader::class.java)
    private val service = RecruitmentPostBookmarkService(
        userReader,
        postReader,
        bookmarkReader,
        bookmarkManager,
        postMetricReader,
        Clock.fixed(Instant.parse("2026-09-15T00:00:00Z"), ZONE),
        userProfileReader,
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
    fun `탈퇴 사용자는 북마크 목록을 조회할 수 없다`() {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(user(UserStatus.WITHDRAWN))

        val exception = assertThrows(ForbiddenException::class.java) {
            service.getBookmarks(USER_ID, cursor = null, size = 10)
        }

        assertEquals(UserErrorCode.USER_WITHDRAWN, exception.errorCode)
        Mockito.verifyNoInteractions(bookmarkReader)
    }

    @Test
    fun `비공개 또는 삭제된 모집글도 북마크를 해제할 수 있다`() {
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(user(UserStatus.ACTIVE))
        Mockito.`when`(postReader.readIncludingDeleted(POST_ID)).thenReturn(Mockito.mock(RecruitmentPost::class.java))

        service.deleteBookmark(USER_ID, POST_ID)

        Mockito.verify(postReader).readIncludingDeleted(POST_ID)
        Mockito.verify(bookmarkManager).delete(USER_ID, POST_ID, NOW)
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
