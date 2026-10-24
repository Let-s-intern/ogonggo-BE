package com.ogonggo.core.usernotification.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.usernotification.domain.UserNotification
import com.ogonggo.core.usernotification.domain.UserNotificationType
import com.ogonggo.core.usernotification.implement.dto.UserNotificationAppendDto
import com.ogonggo.core.usernotification.implement.dto.UserNotificationCursorDto
import com.ogonggo.core.usernotification.persistence.UserNotificationJpaRepository
import com.ogonggo.core.usernotification.persistence.UserNotificationQueryRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(UserNotificationAppender::class, UserNotificationReader::class, UserNotificationManager::class, UserNotificationQueryRepository::class)
internal class UserNotificationPersistenceTest @Autowired constructor(
    private val appender: UserNotificationAppender,
    private val reader: UserNotificationReader,
    private val manager: UserNotificationManager,
    private val repository: UserNotificationJpaRepository,
    private val queries: UserNotificationQueryRepository,
    private val entityManager: EntityManager,
    transactionManager: PlatformTransactionManager,
) {
    private val tx = TransactionTemplate(transactionManager)

    @Test
    @Tag("concurrency")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `모두 읽음 트랜잭션 중 다른 트랜잭션이 커밋한 새 알림은 읽지 않는다`() {
        // given
        val userId = 70000L
        val existing = tx.execute { append(1, userId) }!!
        val boundarySelected = CountDownLatch(1)
        val inserted = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val writer = executor.submit<Long> {
                check(boundarySelected.await(10, TimeUnit.SECONDS))
                try { tx.execute { append(2, userId) }!! } finally { inserted.countDown() }
            }
            // when
            tx.executeWithoutResult {
                val boundary = checkNotNull(queries.findReadBoundary(userId, SINCE, NOW))
                boundarySelected.countDown()
                check(inserted.await(10, TimeUnit.SECONDS))
                queries.markAllRead(userId, boundary, SINCE, NOW)
            }
            val incoming = writer.get(10, TimeUnit.SECONDS)
            // then
            assertEquals(NOW, repository.findById(existing).orElseThrow().readAt)
            assertNull(repository.findById(incoming).orElseThrow().readAt)
            assertEquals(1, reader.countUnread(userId, SINCE))
        } finally {
            executor.shutdownNow()
            // 이 테스트만 트랜잭션을 커밋하므로 임시 H2 fixture를 정리한다.
            tx.executeWithoutResult {
                entityManager.createNativeQuery("delete from user_notifications where recipient_user_id = :id")
                    .setParameter("id", userId).executeUpdate()
            }
        }
    }

    @Test
    fun `같은 시각의 알림은 ID 역순으로 이어지고 중간 새 알림과 타인 알림을 섞지 않는다`() {
        // given
        val first = append(1)
        val second = append(2)
        val third = append(3)
        append(4, userId = 20)
        val page = reader.readPage(10, SINCE, null, 2)
        assertEquals(listOf(third, second), page.map { it.id })
        append(5)
        // when
        val next = reader.readPage(10, SINCE, UserNotificationCursorDto(page.last().createdAt, second), 2)
        // then
        assertEquals(listOf(first), next.map { it.id })
    }

    @Test
    fun `30일 경계는 포함하고 기간 경과와 소프트 삭제는 목록 개수 읽음에서 제외한다`() {
        // given
        val boundary = append(1, at = SINCE)
        val expired = append(2, at = SINCE.minusNanos(1000))
        val deleted = append(3)
        repository.findById(deleted).orElseThrow().delete(NOW)
        repository.flush()
        entityManager.clear()
        // when, then
        assertEquals(listOf(boundary), reader.readPage(10, SINCE, null, 20).map { it.id })
        assertEquals(1, reader.countUnread(10, SINCE))
        listOf(expired, deleted).forEach { id ->
            assertEquals("USER_NOTIFICATION_NOT_FOUND", assertThrows(EntityNotFoundException::class.java) {
                manager.markRead(10, id, SINCE, NOW)
            }.errorCode.code)
        }
    }

    @Test
    fun `읽음 재요청은 최초 읽은 시각을 보존하며 타인 알림은 404다`() {
        // given
        val id = append(1)
        val another = append(2, userId = 20)
        // when
        manager.markRead(10, id, SINCE, NOW)
        manager.markRead(10, id, SINCE, NOW.plusMinutes(1))
        entityManager.clear()
        // then
        assertEquals(NOW, repository.findById(id).orElseThrow().readAt)
        assertEquals(0, reader.countUnread(10, SINCE))
        assertEquals(1, reader.countUnread(20, SINCE))
        assertThrows(EntityNotFoundException::class.java) { manager.markRead(10, another, SINCE, NOW) }
        assertThrows(EntityNotFoundException::class.java) { manager.markRead(10, Long.MAX_VALUE, SINCE, NOW) }
    }

    @Test
    fun `모두 읽음의 경계 뒤에 삽입한 같은 시각 알림은 안 읽음으로 유지한다`() {
        // given
        val existing = append(1)
        val alreadyRead = append(2)
        manager.markRead(10, alreadyRead, SINCE, NOW.minusMinutes(1))
        val boundary = checkNotNull(queries.findReadBoundary(10, SINCE, NOW))
        val incoming = append(3)
        val another = append(4, userId = 20)
        val expired = append(5, at = SINCE.minusSeconds(1))
        // when
        queries.markAllRead(10, boundary, SINCE, NOW)
        entityManager.clear()
        // then
        assertEquals(NOW, repository.findById(existing).orElseThrow().readAt)
        assertEquals(NOW.minusMinutes(1), repository.findById(alreadyRead).orElseThrow().readAt)
        listOf(incoming, another, expired).forEach { assertNull(repository.findById(it).orElseThrow().readAt) }
        assertEquals(1, reader.countUnread(10, SINCE))
    }

    @Test
    fun `모두 읽음은 반복해도 성공하며 타인과 만료된 알림을 변경하지 않는다`() {
        // given
        append(1)
        append(2, userId = 20)
        append(3, at = SINCE.minusSeconds(1))
        // when
        manager.markAllRead(10, SINCE, NOW)
        manager.markAllRead(10, SINCE, NOW.plusSeconds(1))
        entityManager.clear()
        // then
        assertEquals(0, reader.countUnread(10, SINCE))
        assertEquals(1, reader.countUnread(20, SINCE))
        assertEquals(1, repository.findAll().count { it.readAt != null })
        assertEquals(NOW, repository.findAll().single { it.readAt != null }.readAt)
    }

    @Test
    fun `발생 종류 새 댓글 수신자가 같으면 소프트 삭제 후에도 재생성하지 않는다`() {
        // given
        val id = append(1)
        repository.findById(id).orElseThrow().delete(NOW)
        repository.flush()
        // when, then
        assertNull(appender.append(dto(1)))
        assertEquals(1, repository.count())
        assertNotNull(appender.append(dto(1, userId = 20)))
        assertNotNull(appender.append(dto(1).copy(type = UserNotificationType.SIDE_STUDY_COMMENT_REPLY)))
    }

    @Test
    fun `동일 발생 건의 유일성은 DB에서도 보장한다`() {
        // given
        append(1)
        // when, then
        assertThrows(DataIntegrityViolationException::class.java) {
            repository.saveAndFlush(UserNotification(10, UserNotificationType.CONCERN_COMMENT, 30, 100, 1, "회원", null, "제목", "미리보기", null))
        }
    }

    @Test
    @DisplayName("관리자 알림은 발송 묶음과 수신자별 한 건이며 다른 묶음·회원은 독립적이다")
    fun `댓글이 없는 관리자 알림의 중복도 저장소가 방지한다`() {
        // given
        val dto = UserNotificationAppendDto(
            10, UserNotificationType.ADMIN_NOTIFICATION, 42, null, null, "오공고", null, "제목", "본문", null,
            "00000000-0000-0000-0000-000000000001", "https://www.ogonggo.co.kr",
        )
        val id = checkNotNull(appender.append(dto))

        // when
        val duplicate = appender.append(dto)
        val anotherBatch = appender.append(dto.copy(batchId = "00000000-0000-0000-0000-000000000002"))
        val anotherUser = appender.append(dto.copy(recipientUserId = 20))

        // then
        assertNull(duplicate)
        assertNotNull(anotherBatch)
        assertNotNull(anotherUser)
        assertEquals(3, repository.count())
        assertEquals(dto.url, reader.readPage(10, SINCE, null, 20).single { it.id == id }.url)
        assertThrows(DataIntegrityViolationException::class.java) {
            repository.saveAndFlush(UserNotification(
                dto.recipientUserId, dto.type, dto.actorUserId, null, null, dto.actorNickname,
                null, dto.postTitle, dto.preview, null, dto.batchId, dto.url,
            ))
        }
    }

    private fun append(commentId: Long, userId: Long = 10, at: LocalDateTime = NOW): Long {
        val id = checkNotNull(appender.append(dto(commentId, userId)))
        entityManager.createQuery("update UserNotification n set n.createdAt = :at where n.id = :id")
            .setParameter("at", at).setParameter("id", id).executeUpdate()
        entityManager.clear()
        return id
    }

    private fun dto(commentId: Long, userId: Long = 10) = UserNotificationAppendDto(
        userId, UserNotificationType.CONCERN_COMMENT, 30, 100, commentId, "회원", null, "제목", "미리보기", null,
    )

    private companion object {
        val NOW = LocalDateTime.of(2026, 10, 21, 12, 0)
        val SINCE = NOW.minusDays(30)
    }
}
