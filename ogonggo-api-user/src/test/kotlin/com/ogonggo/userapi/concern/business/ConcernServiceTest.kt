package com.ogonggo.userapi.concern.business

import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.implement.ConcernAppender
import com.ogonggo.core.concern.implement.ConcernCommentReader
import com.ogonggo.core.concern.implement.ConcernManager
import com.ogonggo.core.concern.implement.ConcernMetricReader
import com.ogonggo.core.concern.implement.ConcernReader
import com.ogonggo.core.concern.implement.dto.ConcernMetricDto
import com.ogonggo.core.error.BusinessException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ConcernServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val userProfileReader = Mockito.mock(UserProfileReader::class.java)
    private val concernReader = Mockito.mock(ConcernReader::class.java)
    private val concernAppender = Mockito.mock(ConcernAppender::class.java)
    private val concernManager = Mockito.mock(ConcernManager::class.java)
    private val concernMetricReader = Mockito.mock(ConcernMetricReader::class.java)
    private val commentReader = Mockito.mock(ConcernCommentReader::class.java)
    private val eventPublisher = Mockito.mock(ApplicationEventPublisher::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-10-08T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = ConcernService(
        userReader,
        userProfileReader,
        concernReader,
        concernAppender,
        concernManager,
        concernMetricReader,
        commentReader,
        eventPublisher,
        clock,
    )

    @Test
    fun `상세를 조회하면 조회 사실을 발행하고 내가 쓴 글인지 알려 준다`() {
        // given
        val concern = concern(authorUserId = USER_ID)
        Mockito.`when`(concernReader.read(CONCERN_ID)).thenReturn(concern)
        Mockito.`when`(concernMetricReader.read(CONCERN_ID)).thenReturn(ConcernMetricDto(viewCount = 7, commentCount = 2))
        Mockito.`when`(commentReader.readConcernIdsWithOfficialComment(listOf(CONCERN_ID))).thenReturn(setOf(CONCERN_ID))

        // when
        val result = service.readConcern(USER_ID, CONCERN_ID)

        // then
        assertTrue(result.mine)
        assertTrue(result.hasOfficialComment)
        assertEquals(7L, result.viewCount)
        Mockito.verify(eventPublisher).publishEvent(ConcernViewedEvent(CONCERN_ID))
    }

    @Test
    fun `남의 고민글은 수정할 수 없다`() {
        // given
        val concern = concern(authorUserId = OTHER_USER_ID)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(concernReader.readForUpdate(CONCERN_ID)).thenReturn(concern)

        // when
        val exception = assertThrows(BusinessException::class.java) {
            service.update(USER_ID, CONCERN_ID, SaveConcernCommand(ConcernCategory.ETC, "제목", "본문"))
        }

        // then
        assertEquals("CONCERN_PERMISSION_DENIED", exception.errorCode.code)
        Mockito.verifyNoInteractions(concernManager)
    }

    @Test
    fun `이미 지운 내 고민글을 다시 지우면 아무것도 바꾸지 않고 성공한다`() {
        // given
        val concern = concern(authorUserId = USER_ID, deleted = true)
        Mockito.`when`(concernReader.readIncludingDeletedForUpdate(CONCERN_ID)).thenReturn(concern)

        // when
        service.delete(USER_ID, CONCERN_ID)

        // then
        Mockito.verifyNoInteractions(concernManager)
    }

    @Test
    fun `지워진 남의 고민글도 지울 수 없다`() {
        // given
        val concern = concern(authorUserId = OTHER_USER_ID, deleted = true)
        Mockito.`when`(concernReader.readIncludingDeletedForUpdate(CONCERN_ID)).thenReturn(concern)

        // when
        val exception = assertThrows(BusinessException::class.java) { service.delete(USER_ID, CONCERN_ID) }

        // then
        assertEquals("CONCERN_PERMISSION_DENIED", exception.errorCode.code)
    }

    private fun concern(authorUserId: Long, deleted: Boolean = false): Concern {
        val concern = Mockito.mock(Concern::class.java)
        Mockito.`when`(concern.id).thenReturn(CONCERN_ID)
        Mockito.`when`(concern.authorUserId).thenReturn(authorUserId)
        Mockito.`when`(concern.isWrittenBy(USER_ID)).thenReturn(authorUserId == USER_ID)
        Mockito.`when`(concern.isDeleted()).thenReturn(deleted)
        Mockito.`when`(concern.category).thenReturn(ConcernCategory.CAREER)
        Mockito.`when`(concern.title).thenReturn("제목")
        Mockito.`when`(concern.content).thenReturn("본문")
        Mockito.`when`(concern.createdAt).thenReturn(NOW)
        Mockito.`when`(concern.updatedAt).thenReturn(NOW)
        return concern
    }

    private fun activeUser() = UserAccountDto(
        userId = USER_ID,
        letsCareerUserId = 100L,
        email = null,
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
        joinedAt = NOW,
    )

    companion object {
        private const val USER_ID = 17L
        private const val OTHER_USER_ID = 18L
        private const val CONCERN_ID = 5L
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 8, 12, 0)
    }
}
