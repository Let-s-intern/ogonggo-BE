package com.ogonggo.core.announcement.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AnnouncementDomainTest {

    @Test
    fun `제목이 비었거나 본문이 에디터 JSON이 아니면 공지를 만들 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) { announcement(title = " ") }
        assertThrows(IllegalArgumentException::class.java) { announcement(title = "가".repeat(256)) }
        assertThrows(IllegalArgumentException::class.java) { announcement(content = "평문 본문") }
    }

    @Test
    fun `수정은 넘어온 값만 바꾼다`() {
        // given
        val announcement = announcement()

        // when
        announcement.edit(title = "바뀐 제목", content = null)

        // then
        assertEquals("바뀐 제목", announcement.title)
        assertEquals(CONTENT, announcement.content)
    }

    @Test
    fun `다시 삭제해도 최초 삭제 일시를 유지하고 삭제된 공지는 고칠 수 없다`() {
        // given
        val announcement = announcement()

        // when
        announcement.delete(NOW)
        announcement.delete(NOW.plusDays(1))

        // then
        assertEquals(NOW, announcement.deletedAt)
        assertThrows(IllegalStateException::class.java) { announcement.edit(title = "바뀐 제목", content = null) }
        assertThrows(IllegalStateException::class.java) { announcement.publish() }
        assertThrows(IllegalStateException::class.java) { announcement.pin(true) }
    }

    private fun announcement(title: String = "서비스 점검 안내", content: String = CONTENT): Announcement =
        Announcement(title = title, content = content, pinned = false, published = true)

    private companion object {
        const val CONTENT = """{"root":{"children":[],"type":"root","version":1}}"""
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 22, 10, 0)
    }
}
