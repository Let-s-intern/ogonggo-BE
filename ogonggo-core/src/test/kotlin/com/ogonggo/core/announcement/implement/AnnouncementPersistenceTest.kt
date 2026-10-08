package com.ogonggo.core.announcement.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.announcement.domain.AnnouncementManagementSearchCondition
import com.ogonggo.core.announcement.error.AnnouncementErrorCode
import com.ogonggo.core.announcement.implement.dto.AnnouncementAppendDto
import com.ogonggo.core.announcement.persistence.AnnouncementQueryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(AnnouncementReader::class, AnnouncementAppender::class, AnnouncementManager::class, AnnouncementQueryRepository::class)
internal class AnnouncementPersistenceTest @Autowired constructor(
    private val announcementReader: AnnouncementReader,
    private val announcementAppender: AnnouncementAppender,
    private val announcementManager: AnnouncementManager,
) {

    @Test
    fun `사용자 목록은 노출 중인 공지만 고정 공지를 먼저 두고 최신순으로 준다`() {
        // given
        val oldPinned = append("고정 공지", pinned = true)
        val older = append("오래된 공지")
        val newer = append("새 공지")
        append("비노출 공지", published = false)
        val deleted = append("삭제한 공지")
        announcementManager.delete(deleted, NOW)

        // when
        val result = announcementReader.readPublicPage(page = 0, size = 10)

        // then
        assertEquals(listOf(oldPinned.id, newer.id, older.id), result.announcements.map { it.id })
        assertEquals(3, result.totalElements)
    }

    @Test
    fun `비노출이거나 삭제된 공지는 사용자 상세에서 없는 공지와 같다`() {
        val hidden = checkNotNull(append("비노출 공지", published = false).id)
        val deleted = append("삭제한 공지")
        announcementManager.delete(deleted, NOW)

        listOf(hidden, checkNotNull(deleted.id)).forEach { announcementId ->
            val exception = assertThrows(EntityNotFoundException::class.java) { announcementReader.readPublic(announcementId) }
            assertEquals(AnnouncementErrorCode.ANNOUNCEMENT_NOT_FOUND, exception.errorCode)
        }
    }

    @Test
    fun `관리자 목록은 비노출 공지를 포함하고 노출·고정·제목으로 거른다`() {
        // given
        val hiddenPinned = append("점검 안내", pinned = true, published = false)
        append("이벤트 안내")
        append("점검 완료", published = true)

        // when
        val hidden = announcementReader.readManagementPage(AnnouncementManagementSearchCondition(published = false), 0, 20)
        val pinned = announcementReader.readManagementPage(AnnouncementManagementSearchCondition(pinned = true), 0, 20)
        val keyword = announcementReader.readManagementPage(AnnouncementManagementSearchCondition(keyword = "점검"), 0, 20)

        // then
        assertEquals(listOf(hiddenPinned.id), hidden.announcements.map { it.id })
        assertEquals(listOf(hiddenPinned.id), pinned.announcements.map { it.id })
        assertEquals(2, keyword.totalElements)
    }

    @Test
    fun `삭제한 공지도 다시 삭제하려고 찾을 수 있다`() {
        val announcement = append("삭제할 공지")
        announcementManager.delete(announcement, NOW)

        assertEquals(NOW, announcementReader.readForDelete(checkNotNull(announcement.id)).deletedAt)
    }

    private fun append(title: String, pinned: Boolean = false, published: Boolean = true) =
        announcementAppender.append(AnnouncementAppendDto(title = title, content = CONTENT, pinned = pinned, published = published))

    private companion object {
        const val CONTENT = """{"root":{"children":[],"type":"root","version":1}}"""
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 22, 10, 0)
    }
}
