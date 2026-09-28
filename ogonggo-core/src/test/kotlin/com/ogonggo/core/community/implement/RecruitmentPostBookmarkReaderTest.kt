package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.RecruitmentPostBookmarkSearchCondition
import com.ogonggo.core.community.persistence.RecruitmentPostBookmarkJpaRepository
import com.ogonggo.core.community.persistence.RecruitmentPostQueryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class RecruitmentPostBookmarkReaderTest {

    private val bookmarkRepository = Mockito.mock(RecruitmentPostBookmarkJpaRepository::class.java)
    private val postQueryRepository = Mockito.mock(RecruitmentPostQueryRepository::class.java)
    private val reader = RecruitmentPostBookmarkReader(bookmarkRepository, postQueryRepository)

    @Test
    fun `북마크 목록 크기는 1 이상 100 이하여야 한다`() {
        listOf(0, 101).forEach { size ->
            assertThrows(IllegalArgumentException::class.java) {
                reader.readBookmarkedPublishedPage(USER_ID, page = 0, size = size)
            }
        }

        Mockito.verifyNoInteractions(postQueryRepository)
    }

    @Test
    fun `페이지 번호에 해당하는 북마크 목록과 페이지 정보를 반환한다`() {
        val firstPost = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(
            postQueryRepository.findBookmarkedPublishedPage(
                userId = USER_ID,
                condition = RecruitmentPostBookmarkSearchCondition.NONE,
                pageable = PageRequest.of(1, 1),
            ),
        ).thenReturn(
            PageImpl(
                listOf(firstPost),
                PageRequest.of(1, 1),
                2,
            ),
        )

        val page = reader.readBookmarkedPublishedPage(USER_ID, page = 1, size = 1)

        assertEquals(listOf(firstPost), page.items.map { it.post })
        assertEquals(1, page.page)
        assertEquals(1, page.size)
        assertEquals(2, page.totalElements)
        assertEquals(2, page.totalPages)
    }

    companion object {
        private const val USER_ID = 17L
    }
}
