package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.persistence.RecruitmentPostApplicationJpaRepository
import com.ogonggo.core.community.persistence.RecruitmentPostApplicationQueryRepository
import com.ogonggo.core.community.persistence.RecruitmentPostApplicationRow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime

class RecruitmentPostApplicationReaderTest {

    private val applicationQueryRepository = Mockito.mock(RecruitmentPostApplicationQueryRepository::class.java)
    private val reader = RecruitmentPostApplicationReader(
        applicationQueryRepository,
        Mockito.mock(RecruitmentPostApplicationJpaRepository::class.java),
    )

    @Test
    fun `페이지 번호와 크기를 검증한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            reader.readPage(USER_ID, null, null, null, page = -1, size = 10)
        }
        assertThrows(IllegalArgumentException::class.java) {
            reader.readPage(USER_ID, null, null, null, page = 0, size = 101)
        }
        Mockito.verifyNoInteractions(applicationQueryRepository)
    }

    @Test
    fun `모집글과 지원 이력을 페이지로 읽는다`() {
        val row = RecruitmentPostApplicationRow(
            applicationId = 1L,
            postId = POST_ID,
            title = "Kotlin 팀원 모집",
            recruitmentType = RecruitmentType.SIDE_PROJECT,
            recruitmentStatus = RecruitmentStatus.RECRUITING,
            recruitmentEndDate = LocalDate.of(2026, 9, 30),
            lastClickedAt = CLICKED_AT,
            authorUserId = 33L,
        )
        Mockito.`when`(
            applicationQueryRepository.findPage(
                userId = USER_ID,
                publicationStatus = PublicationStatus.PUBLISHED,
                recruitmentStatus = RecruitmentStatus.RECRUITING,
                recruitmentType = RecruitmentType.SIDE_PROJECT,
                keyword = "Kotlin",
                pageable = PageRequest.of(1, 10),
            ),
        ).thenReturn(PageImpl(listOf(row), PageRequest.of(1, 10), 11L))

        val result = reader.readPage(
            userId = USER_ID,
            recruitmentStatus = RecruitmentStatus.RECRUITING,
            recruitmentType = RecruitmentType.SIDE_PROJECT,
            keyword = "Kotlin",
            page = 1,
            size = 10,
        )

        assertEquals(1, result.page)
        assertEquals(10, result.size)
        assertEquals(11L, result.totalElements)
        assertEquals(2, result.totalPages)
        assertEquals(POST_ID, result.items.single().postId)
        assertEquals(33L, result.items.single().authorUserId)
    }

    companion object {
        private const val USER_ID = 17L
        private const val POST_ID = 12L
        private val CLICKED_AT = LocalDateTime.of(2026, 9, 16, 9, 0)
    }
}
