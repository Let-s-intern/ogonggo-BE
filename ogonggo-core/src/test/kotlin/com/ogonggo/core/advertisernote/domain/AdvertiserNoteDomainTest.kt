package com.ogonggo.core.advertisernote.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AdvertiserNoteDomainTest {

    @Test
    fun `제목은 1~100자, 내용은 1~2000자다`() {
        assertThrows(IllegalArgumentException::class.java) { note(title = " ") }
        assertThrows(IllegalArgumentException::class.java) { note(title = "가".repeat(101)) }
        assertThrows(IllegalArgumentException::class.java) { note(content = "가".repeat(2001)) }
        assertEquals(2000, note(content = "가".repeat(2000)).content.length)
    }

    @Test
    fun `수정은 보낸 칸만 바꾸고 삭제한 메모는 고칠 수 없다`() {
        // given
        val note = note()

        // when
        note.edit(title = null, content = "다시 연락하기로 함")

        // then
        assertEquals("통화 기록", note.title)
        assertEquals("다시 연락하기로 함", note.content)
        note.delete(LocalDateTime.of(2026, 11, 6, 10, 0))
        assertThrows(IllegalStateException::class.java) { note.edit(title = "새 제목", content = null) }
    }

    private fun note(title: String = "통화 기록", content: String = "12월 홈 배너 관심") =
        AdvertiserNote(advertiserId = 1, title = title, content = content, authorUserId = 3)
}
