package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPostBookmark
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.community.persistence.RecruitmentPostBookmarkJpaRepository
import com.ogonggo.core.error.ConflictException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime

class RecruitmentPostBookmarkManagerTest {

    private val bookmarkRepository = Mockito.mock(RecruitmentPostBookmarkJpaRepository::class.java)
    private val manager = RecruitmentPostBookmarkManager(bookmarkRepository)

    @Test
    fun `동시 등록으로 유니크 제약이 위반되면 북마크 중복 충돌로 변환한다`() {
        Mockito.`when`(bookmarkRepository.restore(POST_ID, USER_ID, NOW)).thenReturn(0)
        Mockito.doThrow(DataIntegrityViolationException("duplicate bookmark"))
            .`when`(bookmarkRepository)
            .saveAndFlush(Mockito.any(RecruitmentPostBookmark::class.java))

        val exception = assertThrows(ConflictException::class.java) {
            manager.append(USER_ID, POST_ID, NOW)
        }

        assertEquals(RecruitmentPostErrorCode.RECRUITMENT_POST_BOOKMARK_ALREADY_EXISTS, exception.errorCode)
    }

    companion object {
        private const val USER_ID = 17L
        private const val POST_ID = 12L
        private val NOW = LocalDateTime.of(2026, 9, 15, 9, 0)
    }
}
