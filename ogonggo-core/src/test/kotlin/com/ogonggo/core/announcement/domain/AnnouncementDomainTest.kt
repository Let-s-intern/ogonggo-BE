package com.ogonggo.core.announcement.domain

import com.ogonggo.core.error.ConflictException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `요약이 300자를 넘으면 공지를 만들거나 고칠 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            Announcement(title = "공지", content = CONTENT, pinned = false, published = true, summary = "가".repeat(301))
        }
        assertThrows(IllegalArgumentException::class.java) { announcement().changeSummary("가".repeat(301)) }
    }

    @Test
    fun `썸네일과 요약은 빈 글자를 주면 지운다`() {
        // given
        val announcement = Announcement(
            title = "공지",
            content = CONTENT,
            pinned = false,
            published = true,
            thumbnailUrl = "https://example.com/thumb.png",
            summary = "요약",
        )

        // when
        announcement.changeThumbnailUrl("")
        announcement.changeSummary(" ")

        // then
        assertEquals(null, announcement.thumbnailUrl)
        assertEquals(null, announcement.summary)
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
        assertThrows(IllegalStateException::class.java) { announcement.changeCategory(AnnouncementCategory.NEW_JOB_NOTE) }
    }

    @Test
    fun `푸시는 노출 중이고 대상이 정해졌고 아직 보내지 않았을 때만 보낼 차례다`() {
        // given
        val hidden = Announcement(title = "공지", content = CONTENT, pinned = false, published = false, pushTarget = AnnouncementPushTarget.ALL)
        val noTarget = announcement()

        // when & then
        assertFalse(hidden.isPushDue())
        assertFalse(noTarget.isPushDue())
        hidden.publish()
        assertTrue(hidden.isPushDue())
        noTarget.changePushTarget(AnnouncementPushTarget.GENERAL_MEMBERS)
        assertTrue(noTarget.isPushDue())
    }

    @Test
    fun `푸시를 보낸 뒤에는 다시 노출해도 보낼 차례가 아니고 대상을 바꿀 수 없다`() {
        // given
        val announcement = Announcement(title = "공지", content = CONTENT, pinned = false, published = true, pushTarget = AnnouncementPushTarget.ALL)

        // when
        announcement.markPushed("batch-1", NOW)
        announcement.hide()
        announcement.publish()

        // then
        assertEquals("batch-1", announcement.pushBatchId)
        assertEquals(NOW, announcement.pushedAt)
        assertFalse(announcement.isPushDue())
        assertThrows(ConflictException::class.java) { announcement.changePushTarget(AnnouncementPushTarget.NONE) }
        announcement.changePushTarget(AnnouncementPushTarget.ALL)
        assertThrows(IllegalStateException::class.java) { announcement.markPushed("batch-2", NOW) }
    }

    private fun announcement(title: String = "서비스 점검 안내", content: String = CONTENT): Announcement =
        Announcement(title = title, content = content, pinned = false, published = true)

    private companion object {
        const val CONTENT = """{"root":{"children":[],"type":"root","version":1}}"""
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 22, 10, 0)
    }
}
