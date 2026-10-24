package com.ogonggo.userapi.usernotification

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.implement.ConcernAppender
import com.ogonggo.core.concern.implement.dto.ConcernAppendDto
import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.recruitmentpost.domain.*
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostAppender
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostAppendDto
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.dto.UserAppendDto
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenManager
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenReader
import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRegisterDto
import com.ogonggo.core.usernotification.domain.UserNotificationType
import com.ogonggo.core.usernotification.implement.dto.UserNotificationAppendDto
import com.ogonggo.core.usernotification.implement.UserNotificationAppender
import com.ogonggo.userapi.concern.business.ConcernCommentService
import com.ogonggo.userapi.concern.business.ConcernCommentCreatedEvent
import com.ogonggo.userapi.concern.business.CreateConcernCommentCommand
import com.ogonggo.userapi.notification.delivery.implement.NotificationDispatcher
import com.ogonggo.userapi.notification.channel.fcm.FcmNotificationSender
import com.ogonggo.userapi.recruitmentpost.business.CreateRecruitmentPostCommentCommand
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostCommentService
import com.ogonggo.userapi.usernotification.business.UserNotificationService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.ApplicationEventPublisher
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:user-notifications;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "ogonggo.auth.jwt.secret=b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA==",
    "ogonggo.letscareer.base-url=http://localhost:8090", "ogonggo.letscareer.internal-api-key=test-key", "spring.mail.host=localhost",
])
@AutoConfigureMockMvc
@Import(UserNotificationIntegrationTest.FixedTime::class)
class UserNotificationIntegrationTest @Autowired constructor(
    private val users: UserAppender,
    private val concerns: ConcernAppender,
    private val posts: RecruitmentPostAppender,
    private val concernComments: ConcernCommentService,
    private val postComments: RecruitmentPostCommentService,
    private val tokens: UserFcmTokenManager,
    private val tokenReader: UserFcmTokenReader,
    private val notifications: NotificationManager,
    private val inbox: UserNotificationService,
    private val eventPublisher: ApplicationEventPublisher,
    private val jdbc: JdbcTemplate,
    private val tx: TransactionTemplate,
    private val mapper: ObjectMapper,
    private val mvc: MockMvc,
) {
    @MockBean private lateinit var dispatcher: NotificationDispatcher
    @SpyBean private lateinit var deliveryAppender: NotificationAppender
    @SpyBean private lateinit var inboxAppender: UserNotificationAppender

    @Test
    @DisplayName("관리자 알림은 토큰과 댓글 없이 목록·안 읽은 개수에 포함되고 URL 조회와 읽음 처리가 가능하다")
    fun `관리자 알림도 본인 알림함 API로 조회하고 읽는다`() {
        // given
        val recipient = user()
        val another = user()
        val url = "https://www.ogonggo.co.kr/announcements/10"
        val id = checkNotNull(inboxAppender.append(UserNotificationAppendDto(
            recipientUserId = recipient, type = UserNotificationType.ADMIN_NOTIFICATION,
            actorUserId = 42, postId = null, commentId = null,
            actorNickname = "오공고", actorProfileImageUrl = null, postTitle = "운영 안내", preview = "새 소식입니다",
            pushSkipReason = com.ogonggo.core.usernotification.domain.UserNotificationPushSkipReason.NO_ACTIVE_TOKEN,
            batchId = UUID.randomUUID().toString(), url = url,
        )))

        // when
        val response = mvc.perform(get(PATH).with(auth(recipient)))

        // then
        response.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(id))
            .andExpect(jsonPath("$.data.items[0].type").value("ADMIN_NOTIFICATION"))
            .andExpect(jsonPath("$.data.items[0].postTitle").value("운영 안내"))
            .andExpect(jsonPath("$.data.items[0].preview").value("새 소식입니다"))
            .andExpect(jsonPath("$.data.items[0].url").value(url))
            .andExpect(jsonPath("$.data.items[0].postId").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].commentId").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].isRead").value(false))
        assertEquals(1, inbox.countUnread(recipient))
        mvc.perform(patch("$PATH/$id/read").with(auth(another))).andExpect(status().isNotFound)
        mvc.perform(patch("$PATH/$id/read").with(auth(recipient))).andExpect(status().isOk)
        assertEquals(0, inbox.countUnread(recipient))
        assertEquals(NOW, inbox.list(recipient, null, 20).items.single().readAt)
    }

    @Test
    fun `취준고민 댓글 하나는 알림함 하나와 두 설치의 동일 알림 payload를 만들고 발송 실패와 독립적이다`() {
        // given
        val recipient = user()
        val actor = user()
        val concern = concern(recipient)
        register(recipient, "desktop")
        register(recipient, "mobile")
        // when
        val comment = concernComments.create(actor, concern, CreateConcernCommentCommand(null, "답변\n  내용"))
        val item = inbox.list(recipient, null, 20).items.single()
        val deliveries = notifications.findDue(NOW, NOW.minusMinutes(8), 100).filter { it.userNotificationId == item.id }
        // then
        assertEquals(UserNotificationType.CONCERN_COMMENT, item.type)
        assertEquals("회원", item.actorNickname)
        assertNull(item.actorProfileImageUrl)
        assertEquals("답변 내용", item.preview)
        assertEquals(setOf("desktop", "mobile"), deliveries.map { it.recipientAddress }.toSet())
        assertEquals(2, deliveries.size)
        deliveries.forEach {
            val payload = mapper.readTree(it.payloadJson)
            assertEquals("내 고민에 댓글이 달렸어요", payload["title"].asText())
            assertEquals("회원: 답변 내용", payload["body"].asText())
            assertEquals(mapOf("notificationId" to item.id.toString(), "type" to item.type.name, "postId" to concern.toString(), "commentId" to comment.toString()),
                payload["data"].fields().asSequence().associate { field -> field.key to field.value.asText() })
            assertTrue(payload["data"].all { value -> value.isTextual })
        }
        // Firebase 대역으로 두 토큰의 실제 sender 변환·호출까지 확인한다.
        val firebase = Mockito.mock(FirebaseMessaging::class.java)
        var sent = 0
        Mockito.doAnswer { sent++; "provider-$sent" }.`when`(firebase).send(Mockito.any(Message::class.java))
        val sender = FcmNotificationSender(firebase, mapper, tokenReader, tokens, CLOCK)
        deliveries.forEach { assertTrue(sender.send(it) is NotificationDeliveryResult.Sent) }
        assertEquals(2, sent)
        notifications.complete(deliveries.first().notificationId, NotificationDeliveryResult.Failed("TEST_REJECTED", NotificationFailureCategory.CLIENT_ERROR), NOW)
        assertEquals(1, inbox.countUnread(recipient))
        assertNull(inbox.list(recipient, null, 20).items.single().readAt)
        assertEquals(item.id, jdbc.queryForObject("select user_notification_id from notifications where id = ?", Long::class.java, deliveries.first().notificationId))
        assertEquals("TEST_REJECTED", jdbc.queryForObject("select result_code from notifications where id = ?", String::class.java, deliveries.first().notificationId))
        assertNull(jdbc.queryForObject("select push_skip_reason from user_notifications where id = ?", String::class.java, item.id))
    }

    @Test
    fun `토큰이 없어도 알림을 보관하고 재처리와 읽음 재요청은 멱등적이다`() {
        // given
        val recipient = user()
        val actor = user()
        val concern = concern(recipient)
        val comment = concernComments.create(actor, concern, CreateConcernCommentCommand(null, "가".repeat(120)))
        val item = inbox.list(recipient, null, 20).items.single()
        // when
        tx.executeWithoutResult {
            eventPublisher.publishEvent(ConcernCommentCreatedEvent(
                concern, recipient, "제목", comment, null, actor, "변경된 내용", NOW,
            ))
        }
        repeat(2) { inbox.markRead(recipient, item.id) }
        // then
        assertEquals("가".repeat(100), item.preview)
        assertEquals("NO_ACTIVE_TOKEN", jdbc.queryForObject("select push_skip_reason from user_notifications where id = ?", String::class.java, item.id))
        assertEquals(0, jdbc.queryForObject("select count(*) from notifications where user_notification_id = ?", Long::class.java, item.id))
        assertEquals(1, inbox.list(recipient, null, 20).items.size)
        assertEquals(0, inbox.countUnread(recipient))
        assertEquals(NOW, inbox.list(recipient, null, 20).items.single().readAt)
    }

    @ParameterizedTest
    @EnumSource(RecruitmentPostType::class)
    fun `모집글 최상위 댓글은 제외하고 대댓글은 글 작성자가 아닌 부모 댓글 작성자에게 보낸다`(type: RecruitmentPostType) {
        // given
        val author = user()
        val parentAuthor = user()
        val actor = user()
        val post = post(author, type)
        register(parentAuthor, "reply-$type")
        val parent = postComments.create(parentAuthor, post, CreateRecruitmentPostCommentCommand(null, "부모 댓글"))
        assertEquals(0, inbox.countUnread(author))
        assertEquals(0, inbox.countUnread(parentAuthor))
        // when
        val reply = postComments.create(actor, post, CreateRecruitmentPostCommentCommand(parent, "답글"))
        postComments.create(parentAuthor, post, CreateRecruitmentPostCommentCommand(parent, "본인 답글"))
        // then
        val item = inbox.list(parentAuthor, null, 20).items.single()
        assertEquals(UserNotificationType.SIDE_STUDY_COMMENT_REPLY, item.type)
        assertEquals(reply, item.commentId)
        val delivery = notifications.findDue(NOW, NOW.minusMinutes(8), 100).single { it.userNotificationId == item.id }
        assertEquals("내 댓글에 답글이 달렸어요", mapper.readTree(delivery.payloadJson)["title"].asText())
        assertEquals("긴".repeat(255), item.postTitle)
        assertEquals(0, inbox.countUnread(author))
        assertEquals(0, inbox.countUnread(actor))
    }

    @Test
    fun `본인 고민 댓글과 고민 대댓글 및 정지 탈퇴 수신자는 알림을 만들지 않는다`() {
        // given
        val author = user()
        val actor = user()
        val concern = concern(author)
        // when
        val root = concernComments.create(author, concern, CreateConcernCommentCommand(null, "본인 댓글"))
        concernComments.create(actor, concern, CreateConcernCommentCommand(root, "대댓글 제외"))
        listOf("SUSPENDED", "WITHDRAWN").forEach { state ->
            val recipient = user()
            val target = concern(recipient)
            jdbc.update("update users set status = ? where id = ?", state, recipient)
            concernComments.create(actor, target, CreateConcernCommentCommand(null, "상태 제외"))
            assertEquals(0, jdbc.queryForObject("select count(*) from user_notifications where recipient_user_id = ?", Long::class.java, recipient))
        }
        // then
        assertEquals(0, inbox.countUnread(author))
        assertEquals(0, inbox.countUnread(actor))
    }

    @Test
    fun `발송 대기열 저장 뒤 실패하면 댓글 지표 알림함 대기열을 모두 롤백한다`() {
        // given
        val recipient = user()
        val actor = user()
        val concern = concern(recipient)
        register(recipient, "rollback-token")
        Mockito.doAnswer { invocation -> invocation.callRealMethod(); throw IllegalStateException("강제 적재 실패") }
            .`when`(deliveryAppender).appendAll(Mockito.anyCollection())
        // when
        assertThrows(InternalServerException::class.java) { concernComments.create(actor, concern, CreateConcernCommentCommand(null, "롤백 댓글")) }
        // then
        assertEquals(0, jdbc.queryForObject("select count(*) from concern_comments where concern_id = ?", Long::class.java, concern))
        assertEquals(0, jdbc.queryForObject("select comment_count from concern_metrics where concern_id = ?", Long::class.java, concern))
        assertEquals(0, inbox.countUnread(recipient))
        assertEquals(0, jdbc.queryForObject("select count(*) from notifications where recipient_user_id = ?", Long::class.java, recipient))
    }

    @Test
    fun `알림함 저장 뒤 실패해도 모집글 답글과 지표를 함께 롤백한다`() {
        // given
        val recipient = user()
        val actor = user()
        val post = post(recipient)
        val parent = postComments.create(recipient, post, CreateRecruitmentPostCommentCommand(null, "부모"))
        Mockito.doAnswer { invocation -> invocation.callRealMethod(); throw IllegalStateException("강제 알림함 실패") }
            .`when`(inboxAppender).append(Mockito.any(UserNotificationAppendDto::class.java) ?: UserNotificationAppendDto(recipient, UserNotificationType.SIDE_STUDY_COMMENT_REPLY, actor, post, 1, "회원", null, "제목", "답글", null))
        // when
        assertThrows(InternalServerException::class.java) { postComments.create(actor, post, CreateRecruitmentPostCommentCommand(parent, "실패 답글")) }
        // then
        assertEquals(1, jdbc.queryForObject("select count(*) from recruitment_post_comments where post_id = ?", Long::class.java, post))
        assertEquals(1, jdbc.queryForObject("select comment_count from recruitment_post_metrics where post_id = ?", Long::class.java, post))
        assertEquals(0, inbox.countUnread(recipient))
    }

    @Test
    fun `HTTP 목록과 개수 읽음은 본인만 접근하며 삭제 비공개된 원글 권한을 우회하지 않는다`() {
        // given
        val recipient = user()
        val actor = user()
        val concern = concern(recipient)
        val comment = concernComments.create(actor, concern, CreateConcernCommentCommand(null, "답변"))
        val id = inbox.list(recipient, null, 20).items.single().id
        // when, then
        mvc.perform(get(PATH).with(auth(recipient))).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].isRead").value(false))
            .andExpect(jsonPath("$.data.items[0].actorProfileImageUrl").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].createdAt").value("2026-10-21T12:00:00"))
        mvc.perform(get(PATH).with(auth(actor))).andExpect(status().isOk).andExpect(jsonPath("$.data.items").isEmpty)
        mvc.perform(get("$PATH/unread-count").with(auth(actor))).andExpect(jsonPath("$.data.count").value(0))
        mvc.perform(patch("$PATH/$id/read").with(auth(actor))).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("USER_NOTIFICATION_NOT_FOUND"))
        mvc.perform(patch("$PATH/read-all").with(auth(actor))).andExpect(status().isOk)
        assertEquals(1, inbox.countUnread(recipient))
        jdbc.update("update concerns set hidden = true where id = ?", concern)
        mvc.perform(get("/api/v1/concerns/$concern").with(auth(recipient))).andExpect(status().isNotFound)
        jdbc.update("update concerns set hidden = false, deleted_at = ? where id = ?", NOW, concern)
        mvc.perform(get("/api/v1/concerns/$concern/comments").with(auth(recipient))).andExpect(status().isNotFound)
        assertEquals(comment, inbox.list(recipient, null, 20).items.single().commentId)
        repeat(2) { mvc.perform(patch("$PATH/$id/read").with(auth(recipient))).andExpect(status().isOk).andExpect(jsonPath("$.data").doesNotExist()) }
    }

    @Test
    fun `잘못된 커서는 400이고 같은 시각 커서는 새 삽입 중에도 기존 항목을 빠짐없이 잇는다`() {
        // given
        val recipient = user()
        val actor = user()
        val concern = concern(recipient)
        repeat(3) { concernComments.create(actor, concern, CreateConcernCommentCommand(null, "댓글 $it")) }
        val first = inbox.list(recipient, null, 2)
        concernComments.create(actor, concern, CreateConcernCommentCommand(null, "새 댓글"))
        // when
        val second = inbox.list(recipient, first.nextCursor, 2)
        // then
        assertTrue(first.hasNext)
        assertFalse(second.hasNext)
        assertNull(second.nextCursor)
        assertEquals(3, (first.items + second.items).map { it.id }.distinct().size)
        listOf("", "bad", "not!base64", "MA", "a".repeat(201)).forEach { cursor ->
            mvc.perform(get(PATH).param("cursor", cursor).with(auth(recipient)))
                .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }
    }

    @Test
    fun `알림 스냅샷은 프로필과 글 수정 및 댓글 삭제 뒤에도 생성 당시 값이다`() {
        // given
        val recipient = user()
        val actor = user()
        val concern = concern(recipient)
        jdbc.update("insert into user_profiles (user_id, nickname, profile_image_url, last_synced_at, created_at, updated_at) values (?, ?, ?, ?, ?, ?)",
            actor, "취준생A", "https://example.com/a.png", NOW, NOW, NOW)
        val comment = concernComments.create(actor, concern, CreateConcernCommentCommand(null, "답변 원문"))
        // when
        jdbc.update("update user_profiles set nickname = ?, profile_image_url = ? where user_id = ?", "바뀐 이름", "https://example.com/b.png", actor)
        jdbc.update("update concerns set title = ? where id = ?", "바뀐 제목", concern)
        concernComments.delete(actor, concern, comment)
        // then
        val item = inbox.list(recipient, null, 20).items.single()
        assertEquals("취준생A", item.actorNickname)
        assertEquals("https://example.com/a.png", item.actorProfileImageUrl)
        assertEquals("제목", item.postTitle)
        assertEquals("답변 원문", item.preview)
    }

    @Test
    fun `알림함 네 API는 인증과 활성 회원을 요구하고 size 범위를 검증한다`() {
        // given
        val user = user()
        val routes = listOf(get(PATH), get("$PATH/unread-count"), patch("$PATH/1/read"), patch("$PATH/read-all"))
        // when, then
        routes.forEach { mvc.perform(it).andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("UNAUTHORIZED")) }
        listOf("0", "101", "-1", "oops").forEach { size ->
            mvc.perform(get(PATH).param("size", size).with(auth(user))).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }
        mvc.perform(get(PATH).param("size", "100").with(auth(user))).andExpect(status().isOk)
        listOf("SUSPENDED", "WITHDRAWN").forEach { state ->
            jdbc.update("update users set status = ? where id = ?", state, user)
            routes.forEach { mvc.perform(it.with(auth(user))).andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("USER_$state")) }
        }
        routes.forEach { mvc.perform(it.with(auth(Long.MAX_VALUE))).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("USER_NOT_FOUND")) }
    }

    @Test
    fun `모두 읽음 API는 기간 안 본인 알림만 읽고 만료된 단건 읽음은 404다`() {
        // given
        val recipient = user()
        val actor = user()
        val concern = concern(recipient)
        repeat(3) { concernComments.create(actor, concern, CreateConcernCommentCommand(null, "댓글 $it")) }
        val expired = inbox.list(recipient, null, 20).items.first().id
        jdbc.update("update user_notifications set created_at = ? where id = ?", NOW.minusDays(30).minusSeconds(1), expired)
        // when, then
        repeat(2) { mvc.perform(patch("$PATH/read-all").with(auth(recipient))).andExpect(status().isOk).andExpect(jsonPath("$.data").doesNotExist()) }
        mvc.perform(get("$PATH/unread-count").with(auth(recipient))).andExpect(jsonPath("$.data.count").value(0))
        assertTrue(inbox.list(recipient, null, 20).items.all { it.readAt == NOW })
        assertEquals(2, inbox.list(recipient, null, 20).items.size)
        mvc.perform(patch("$PATH/$expired/read").with(auth(recipient))).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("USER_NOTIFICATION_NOT_FOUND"))
        assertNull(jdbc.queryForObject("select read_at from user_notifications where id = ?", java.sql.Timestamp::class.java, expired))
    }

    @Test
    fun `댓글 이벤트의 알림 적재는 커밋 전 완료되고 원본 트랜잭션이 롤백되면 함께 사라진다`() {
        // given
        val recipient = user()
        val actor = user()
        val concern = concern(recipient)
        register(recipient, "synchronous-token")

        // when
        tx.executeWithoutResult { transaction ->
            concernComments.create(actor, concern, CreateConcernCommentCommand(null, "동기 이벤트"))
            assertEquals(1, inbox.countUnread(recipient))
            assertEquals(1, jdbc.queryForObject("select count(*) from notifications where recipient_user_id = ?", Long::class.java, recipient))
            transaction.setRollbackOnly()
        }

        // then
        assertEquals(0, inbox.countUnread(recipient))
        assertEquals(0, jdbc.queryForObject("select count(*) from concern_comments where concern_id = ?", Long::class.java, concern))
        assertEquals(0, jdbc.queryForObject("select count(*) from notifications where recipient_user_id = ?", Long::class.java, recipient))
    }

    private fun user(): Long = tx.execute { users.append(UserAppendDto(IDS.incrementAndGet(), NOW)).userId }!!
    private fun concern(author: Long): Long = tx.execute {
        concerns.append(ConcernAppendDto(author, ConcernCategory.entries.first(), "제목", """{"root":{"children":[]}}""")).id
    }!!
    private fun register(userId: Long, token: String) = tx.executeWithoutResult { tokens.register(userId, UserFcmTokenRegisterDto(token, UUID.randomUUID()), NOW) }
    private fun post(author: Long, type: RecruitmentPostType = RecruitmentPostType.entries.first()): Long = tx.execute { posts.append(RecruitmentPostAppendDto(
        authorUserId = author, title = "긴".repeat(255), recruitmentType = type, capacity = 2,
        progressMethod = RecruitmentPostProgressMethod.entries.first(), activityDurationMonths = 1, technologyStacks = emptyList(),
        summary = "요약", content = """{"root":{"type":"root","children":[]}}""", eligibilityAndSelectionProcess = null,
        recruitmentStartDate = NOW.toLocalDate(), recruitmentEndDate = NOW.toLocalDate().plusDays(5),
        positions = listOf(RecruitmentPostPosition.entries.first()), contactMethod = RecruitmentPostContactMethod.EMAIL, contactValue = "test@example.com",
    )).id }!!
    private fun auth(userId: Long) = authentication(UsernamePasswordAuthenticationToken(userId, null, emptyList()))

    @TestConfiguration
    class FixedTime {
        @Bean @Primary fun notificationTestClock(): Clock = CLOCK
    }

    companion object {
        private const val PATH = "/api/v1/users/me/notifications"
        private val IDS = java.util.concurrent.atomic.AtomicLong(90000)
        private val CLOCK = Clock.fixed(Instant.parse("2026-10-21T03:00:00Z"), ZoneId.of("Asia/Seoul"))
        private val NOW = LocalDateTime.now(CLOCK)
    }
}
