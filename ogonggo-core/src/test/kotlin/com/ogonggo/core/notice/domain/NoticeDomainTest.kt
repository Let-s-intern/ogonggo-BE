package com.ogonggo.core.notice.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NoticeDomainTest {

    @Test
    fun `제목이 비었거나 본문이 에디터 JSON이 아니면 공지를 만들 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) { notice(title = " ") }
        assertThrows(IllegalArgumentException::class.java) { notice(title = "가".repeat(256)) }
        assertThrows(IllegalArgumentException::class.java) { notice(content = "평문 본문") }
    }

    @Test
    fun `수정은 넘어온 값만 바꾼다`() {
        // given
        val notice = notice()

        // when
        notice.edit(title = "바뀐 제목", content = null)

        // then
        assertEquals("바뀐 제목", notice.title)
        assertEquals(CONTENT, notice.content)
    }

    @Test
    fun `다시 삭제해도 최초 삭제 일시를 유지하고 삭제된 공지는 고칠 수 없다`() {
        // given
        val notice = notice()

        // when
        notice.delete(NOW)
        notice.delete(NOW.plusDays(1))

        // then
        assertEquals(NOW, notice.deletedAt)
        assertThrows(IllegalStateException::class.java) { notice.edit(title = "바뀐 제목", content = null) }
        assertThrows(IllegalStateException::class.java) { notice.publish() }
        assertThrows(IllegalStateException::class.java) { notice.pin(true) }
    }

    private fun notice(title: String = "서비스 점검 안내", content: String = CONTENT): Notice =
        Notice(title = title, content = content, pinned = false, published = true)

    private companion object {
        const val CONTENT = """{"root":{"children":[],"type":"root","version":1}}"""
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 22, 10, 0)
    }
}
