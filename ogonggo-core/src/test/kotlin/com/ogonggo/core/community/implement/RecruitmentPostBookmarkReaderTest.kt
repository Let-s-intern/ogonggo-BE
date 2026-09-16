package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.persistence.RecruitmentPostBookmarkCursorRow
import com.ogonggo.core.community.persistence.RecruitmentPostBookmarkJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class RecruitmentPostBookmarkReaderTest {

    private val bookmarkRepository = Mockito.mock(RecruitmentPostBookmarkJpaRepository::class.java)
    private val reader = RecruitmentPostBookmarkReader(bookmarkRepository)

    @Test
    fun `북마크 목록 크기는 1 이상 100 이하여야 한다`() {
        listOf(0, 101).forEach { size ->
            assertThrows(IllegalArgumentException::class.java) {
                reader.readBookmarkedPublishedCursorPage(USER_ID, cursor = null, size = size)
            }
        }

        Mockito.verifyNoInteractions(bookmarkRepository)
    }

    @Test
    fun `조회 결과가 요청 크기보다 많으면 다음 커서가 있는 페이지를 반환한다`() {
        val cursor = RecruitmentPostBookmarkCursor(UPDATED_AT, id = 20L)
        val firstPost = Mockito.mock(RecruitmentPost::class.java)
        val secondPost = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(
            bookmarkRepository.findBookmarkedPublishedCursorPage(
                userId = USER_ID,
                publicationStatus = PublicationStatus.PUBLISHED,
                cursorUpdatedAt = UPDATED_AT,
                cursorId = 20L,
                pageable = PageRequest.of(0, 2),
            ),
        ).thenReturn(
            listOf(
                RecruitmentPostBookmarkCursorRow(firstPost, NEXT_UPDATED_AT, bookmarkId = 19L),
                RecruitmentPostBookmarkCursorRow(secondPost, UPDATED_AT.minusMinutes(1), bookmarkId = 18L),
            ),
        )

        val page = reader.readBookmarkedPublishedCursorPage(USER_ID, cursor, size = 1)

        assertEquals(listOf(firstPost), page.items.map { it.post })
        assertEquals(RecruitmentPostBookmarkCursor(NEXT_UPDATED_AT, id = 19L), page.items.single().cursor)
        assertTrue(page.hasNext)
    }

    companion object {
        private const val USER_ID = 17L
        private val UPDATED_AT = LocalDateTime.of(2026, 9, 15, 9, 0)
        private val NEXT_UPDATED_AT = LocalDateTime.of(2026, 9, 15, 8, 0)
    }
}
