package com.ogonggo.core.notice.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.notice.domain.NoticeManagementSearchCondition
import com.ogonggo.core.notice.error.NoticeErrorCode
import com.ogonggo.core.notice.implement.dto.NoticeAppendDto
import com.ogonggo.core.notice.persistence.NoticeQueryRepository
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
@Import(NoticeReader::class, NoticeAppender::class, NoticeManager::class, NoticeQueryRepository::class)
internal class NoticePersistenceTest @Autowired constructor(
    private val noticeReader: NoticeReader,
    private val noticeAppender: NoticeAppender,
    private val noticeManager: NoticeManager,
) {

    @Test
    fun `사용자 목록은 노출 중인 공지만 고정 공지를 먼저 두고 최신순으로 준다`() {
        // given
        val oldPinned = append("고정 공지", pinned = true)
        val older = append("오래된 공지")
        val newer = append("새 공지")
        append("비노출 공지", published = false)
        val deleted = append("삭제한 공지")
        noticeManager.delete(deleted, NOW)

        // when
        val result = noticeReader.readPublicPage(page = 0, size = 10)

        // then
        assertEquals(listOf(oldPinned.id, newer.id, older.id), result.notices.map { it.id })
        assertEquals(3, result.totalElements)
    }

    @Test
    fun `비노출이거나 삭제된 공지는 사용자 상세에서 없는 공지와 같다`() {
        val hidden = checkNotNull(append("비노출 공지", published = false).id)
        val deleted = append("삭제한 공지")
        noticeManager.delete(deleted, NOW)

        listOf(hidden, checkNotNull(deleted.id)).forEach { noticeId ->
            val exception = assertThrows(EntityNotFoundException::class.java) { noticeReader.readPublic(noticeId) }
            assertEquals(NoticeErrorCode.NOTICE_NOT_FOUND, exception.errorCode)
        }
    }

    @Test
    fun `관리자 목록은 비노출 공지를 포함하고 노출·고정·제목으로 거른다`() {
        // given
        val hiddenPinned = append("점검 안내", pinned = true, published = false)
        append("이벤트 안내")
        append("점검 완료", published = true)

        // when
        val hidden = noticeReader.readManagementPage(NoticeManagementSearchCondition(published = false), 0, 20)
        val pinned = noticeReader.readManagementPage(NoticeManagementSearchCondition(pinned = true), 0, 20)
        val keyword = noticeReader.readManagementPage(NoticeManagementSearchCondition(keyword = "점검"), 0, 20)

        // then
        assertEquals(listOf(hiddenPinned.id), hidden.notices.map { it.id })
        assertEquals(listOf(hiddenPinned.id), pinned.notices.map { it.id })
        assertEquals(2, keyword.totalElements)
    }

    @Test
    fun `삭제한 공지도 다시 삭제하려고 찾을 수 있다`() {
        val notice = append("삭제할 공지")
        noticeManager.delete(notice, NOW)

        assertEquals(NOW, noticeReader.readForDelete(checkNotNull(notice.id)).deletedAt)
    }

    private fun append(title: String, pinned: Boolean = false, published: Boolean = true) =
        noticeAppender.append(NoticeAppendDto(title = title, content = CONTENT, pinned = pinned, published = published))

    private companion object {
        const val CONTENT = """{"root":{"children":[],"type":"root","version":1}}"""
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 22, 10, 0)
    }
}
