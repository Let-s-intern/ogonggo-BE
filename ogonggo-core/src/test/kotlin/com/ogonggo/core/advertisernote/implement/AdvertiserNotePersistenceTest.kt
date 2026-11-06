package com.ogonggo.core.advertisernote.implement

import com.ogonggo.core.advertisernote.error.AdvertiserNoteErrorCode
import com.ogonggo.core.advertisernote.implement.dto.AdvertiserNoteAppendDto
import com.ogonggo.core.advertisernote.implement.dto.AdvertiserNoteUpdateDto
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(AdvertiserNoteReader::class, AdvertiserNoteAppender::class, AdvertiserNoteManager::class)
internal class AdvertiserNotePersistenceTest @Autowired constructor(
    private val reader: AdvertiserNoteReader,
    private val appender: AdvertiserNoteAppender,
    private val manager: AdvertiserNoteManager,
    private val entityManager: EntityManager,
) {

    @Test
    fun `광고주의 메모는 나중에 쓴 것이 먼저이고 지운 메모와 다른 광고주의 메모는 뺀다`() {
        // given
        val older = append(createdAt = NOW.minusDays(1))
        val newer = append(createdAt = NOW)
        val deleted = append(createdAt = NOW.plusDays(1))
        append(advertiserId = ADVERTISER_ID + 1)
        manager.delete(reader.readForDelete(deleted), NOW)
        entityManager.flush()

        // when
        val notes = reader.readAllByAdvertiser(ADVERTISER_ID)

        // then
        assertEquals(listOf(newer, older), notes.map { it.id })
    }

    @Test
    fun `잠가 읽은 메모의 수정이 저장되고 지운 메모는 찾을 수 없다`() {
        // given
        val id = append()

        // when
        manager.update(reader.readForUpdate(id), AdvertiserNoteUpdateDto(title = "새 제목"))
        entityManager.flush()
        entityManager.clear()

        // then
        val note = reader.read(id)
        assertEquals("새 제목", note.title)
        assertEquals("12월 홈 배너 관심", note.content)
        assertEquals(ADMIN_USER_ID, note.authorUserId)
        manager.delete(reader.readForDelete(id), NOW)
        entityManager.flush()
        val exception = assertThrows(EntityNotFoundException::class.java) { reader.readForUpdate(id) }
        assertEquals(AdvertiserNoteErrorCode.ADVERTISER_NOTE_NOT_FOUND, exception.errorCode)
    }

    private fun append(advertiserId: Long = ADVERTISER_ID, createdAt: LocalDateTime? = null): Long {
        val id = checkNotNull(
            appender.append(
                AdvertiserNoteAppendDto(
                    advertiserId = advertiserId,
                    title = "통화 기록",
                    content = "12월 홈 배너 관심",
                    authorUserId = ADMIN_USER_ID,
                ),
            ).id,
        )
        entityManager.flush()
        if (createdAt != null) {
            entityManager.createQuery("update AdvertiserNote n set n.createdAt = :createdAt where n.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", id)
                .executeUpdate()
            entityManager.clear()
        }
        return id
    }

    private companion object {
        const val ADVERTISER_ID = 10L
        const val ADMIN_USER_ID = 3L
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 10, 0)
    }
}
