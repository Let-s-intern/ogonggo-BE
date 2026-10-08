package com.ogonggo.core.concern.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ConcernDomainTest {

    @Test
    fun `고민글을 수정하면 카테고리와 제목과 본문을 바꾼다`() {
        // given
        val concern = concern()

        // when
        concern.update(ConcernCategory.CAREER, "바뀐 제목", "바뀐 본문")

        // then
        assertEquals(ConcernCategory.CAREER, concern.category)
        assertEquals("바뀐 제목", concern.title)
        assertEquals("바뀐 본문", concern.content)
    }

    @Test
    fun `삭제된 고민글은 수정할 수 없다`() {
        // given
        val concern = concern()
        concern.delete(NOW)

        // when
        // then
        assertThrows(IllegalStateException::class.java) {
            concern.update(ConcernCategory.ETC, "제목", "본문")
        }
    }

    @Test
    fun `이미 삭제된 고민글을 다시 삭제해도 처음 삭제 일시를 유지한다`() {
        // given
        val concern = concern()
        concern.delete(NOW)

        // when
        concern.delete(NOW.plusDays(1))

        // then
        assertEquals(NOW, concern.deletedAt)
    }

    @Test
    fun `제목이 100자를 넘으면 고민글을 만들 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            Concern(authorUserId = 1L, category = ConcernCategory.ETC, title = "가".repeat(101), content = "본문")
        }
    }

    private fun concern() = Concern(
        authorUserId = 1L,
        category = ConcernCategory.JOB_POSTING,
        title = "제목",
        content = "본문",
    )

    companion object {
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 8, 12, 0)
    }
}
