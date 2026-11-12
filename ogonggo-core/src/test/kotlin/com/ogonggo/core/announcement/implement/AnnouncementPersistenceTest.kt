package com.ogonggo.core.announcement.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.announcement.domain.AnnouncementCategory
import com.ogonggo.core.announcement.domain.AnnouncementManagementSearchCondition
import com.ogonggo.core.announcement.domain.AnnouncementPublicSearchCondition
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
        val result = announcementReader.readPublicPage(AnnouncementPublicSearchCondition(), page = 0, size = 10)

        // then
        assertEquals(listOf(oldPinned.id, newer.id, older.id), result.announcements.map { it.id })
        assertEquals(3, result.totalElements)
    }

    @Test
    fun `사용자 목록은 카테고리와 고정 여부로 거르고 검색어는 제목·요약·본문에서 찾는다`() {
        // given
        val byTitle = append("삼성전자 신입 공채 정리", category = AnnouncementCategory.NEW_JOB_NOTE)
        val bySummary = append("이번 주 새 공고", category = AnnouncementCategory.NEW_JOB_NOTE, summary = "삼성 계열사 12건")
        val byContent = append("주간 공고 노트", category = AnnouncementCategory.NEW_JOB_NOTE, content = contentWith("삼성바이오로직스"))
        val pinnedUpdate = append("새 기능 안내", pinned = true)
        append("삼성 비노출", published = false)

        // when
        val keyword = announcementReader.readPublicPage(AnnouncementPublicSearchCondition(keyword = "삼성"), 0, 10)
        val notes = announcementReader.readPublicPage(AnnouncementPublicSearchCondition(category = AnnouncementCategory.NEW_JOB_NOTE), 0, 10)
        val pinned = announcementReader.readPublicPage(AnnouncementPublicSearchCondition(pinned = true), 0, 10)

        // then
        assertEquals(setOf(byTitle.id, bySummary.id, byContent.id), keyword.announcements.map { it.id }.toSet())
        assertEquals(3, notes.totalElements)
        assertEquals(listOf(pinnedUpdate.id), pinned.announcements.map { it.id })
    }

    @Test
    fun `카테고리 칸은 모든 카테고리를 순서대로 주고 노출 중인 공지 수와 최근 공지를 싣는다`() {
        // given
        val updates = (1..4).map { append("업데이트 $it") }
        append("숨긴 업데이트", published = false)
        val note = append("공고 노트", category = AnnouncementCategory.NEW_JOB_NOTE)

        // when
        val digests = announcementReader.readPublicCategoryDigests(recentLimit = 3)

        // then
        assertEquals(AnnouncementCategory.entries, digests.map { it.category })
        assertEquals(listOf(4L, 1L, 0L), digests.map { it.count })
        assertEquals(updates.takeLast(3).reversed().map { it.id }, digests[0].recentAnnouncements.map { it.id })
        assertEquals(listOf(note.id), digests[1].recentAnnouncements.map { it.id })
        assertEquals(emptyList<Any>(), digests[2].recentAnnouncements)
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

    private fun append(
        title: String,
        pinned: Boolean = false,
        published: Boolean = true,
        category: AnnouncementCategory = AnnouncementCategory.OGONGGO_UPDATE,
        summary: String? = null,
        content: String = CONTENT,
    ) = announcementAppender.append(
        AnnouncementAppendDto(
            title = title,
            content = content,
            pinned = pinned,
            published = published,
            category = category,
            summary = summary,
        ),
    )

    private fun contentWith(text: String) =
        """{"root":{"children":[{"children":[{"text":"$text","type":"text","version":1}],"type":"paragraph","version":1}],"type":"root","version":1}}"""

    private companion object {
        const val CONTENT = """{"root":{"children":[],"type":"root","version":1}}"""
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 22, 10, 0)
    }
}
