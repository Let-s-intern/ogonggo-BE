package com.ogonggo.userapi.notification.intake.implement.signup

import com.ogonggo.core.user.domain.LetsCareerAuthProvider
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.auth.business.CompanyAuthService
import com.ogonggo.userapi.auth.business.CompanySignUpCommand
import com.ogonggo.userapi.auth.business.UserAuthService
import com.ogonggo.userapi.auth.business.UserSignedUpEvent
import com.ogonggo.userapi.auth.implement.LetsCareerAuthClient
import com.ogonggo.userapi.auth.implement.LetsCareerUser
import com.ogonggo.userapi.auth.implement.RefreshTokenStore
import com.ogonggo.userapi.config.UserAsyncConfiguration
import com.ogonggo.userapi.config.UserAlimTalkConfiguration
import com.ogonggo.userapi.notification.delivery.implement.NotificationDispatcher
import com.ogonggo.userapi.user.implement.LetsCareerUserClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.client.RestClient
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:ogonggo-user-signup-alimtalk;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ogonggo.auth.jwt.secret=b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA==",
        "ogonggo.letscareer.base-url=http://localhost:8090",
        "ogonggo.letscareer.internal-api-key=test-internal-api-key",
        "spring.mail.host=localhost",
        "nhn.appKey=test-app-key",
        "nhn.secretKey=test-secret-key",
        "nhn.sendKey=test-send-key",
    ],
)
class UserSignUpAlimTalkEventIntegrationTest @Autowired constructor(
    private val service: UserAuthService,
    private val companyService: CompanyAuthService,
    private val userReader: UserReader,
    private val eventPublisher: ApplicationEventPublisher,
    private val transactionTemplate: TransactionTemplate,
    private val boundary: NhnHttpBoundary,
    private val jdbc: JdbcTemplate,
    @Qualifier(UserAsyncConfiguration.NOTIFICATION_ENQUEUE_TASK_EXECUTOR)
    private val notificationEnqueueTaskExecutor: TaskExecutor,
) {

    @MockBean
    private lateinit var letsCareerAuthClient: LetsCareerAuthClient

    @MockBean
    private lateinit var letsCareerUserClient: LetsCareerUserClient

    @MockBean
    private lateinit var refreshTokenStore: RefreshTokenStore

    /** 이 테스트는 행 적재와 AFTER_COMMIT 경계까지만 확인한다. provider 처리는 실행하지 않는다. */
    @MockBean
    private lateinit var notificationDispatcher: NotificationDispatcher

    @BeforeEach
    fun prepareHttpBoundary() {
        boundary.reset()
        jdbc.update("delete from notifications")
    }

    @Test
    fun `실제 신규 가입은 커밋 후 notification 행을 적재하고 재로그인에는 중복 행을 만들지 않는다`() {
        // given
        stubLetsCareerUser(4821L)

        // when
        val firstLogin = service.signInWithLetsCareer("lc-access")
        awaitNotificationEnqueue()

        // then
        assertTrue(firstLogin.isNewUser)
        val account = checkNotNull(userReader.readByLetsCareerUserId(4821L))
        assertEquals(1, notificationCount(account.userId))
        val notification = jdbc.queryForMap(
            "select channel, template_code, status, recipient_address, deduplication_key " +
                "from notifications where recipient_user_id = ?",
            account.userId,
        )
        assertEquals("KAKAO", notification["channel"])
        assertEquals("sign_up_confirm", notification["template_code"])
        assertEquals("PENDING", notification["status"])
        assertEquals("01012345678", notification["recipient_address"])
        assertEquals("sign_up_confirm:user:${account.userId}:KAKAO", notification["deduplication_key"])
        boundary.server.verify()

        // when
        val secondLogin = service.signInWithLetsCareer("lc-access")

        // then
        assertFalse(secondLogin.isNewUser)
        assertEquals(1, notificationCount(account.userId))
    }

    @Test
    fun `기업회원 가입은 일반회원 가입 알림을 요청하지 않는다`() {
        // given
        val command = CompanySignUpCommand(
            email = "company-signup@example.com",
            password = "test-password",
            organizationName = "테스트 기업",
            managerName = "테스트 담당자",
        )

        // when
        val tokens = companyService.signUp(command)
        awaitNotificationEnqueue()

        // then
        assertTrue(tokens.accessToken.isNotBlank())
        assertNotNull(userReader.readCredentialByEmail(command.email))
        assertEquals(0, jdbc.queryForObject("select count(*) from notifications", Int::class.java))
        boundary.server.verify()
    }

    @Test
    fun `번호가 잘못돼도 계정 가입은 성공하고 HTTP 요청만 생략한다`() {
        // given
        stubLetsCareerUser(4822L, "010")

        // when
        val result = service.signInWithLetsCareer("lc-access")
        awaitNotificationEnqueue()

        // then
        assertTrue(result.isNewUser)
        val account = checkNotNull(userReader.readByLetsCareerUserId(4822L))
        assertEquals(0, notificationCount(account.userId))
        boundary.server.verify()
    }

    @Test
    fun `가입 트랜잭션이 진행 중이면 notification 행을 만들지 않고 커밋 후 적재한다`() {
        // when / then
        transactionTemplate.executeWithoutResult {
            eventPublisher.publishEvent(SIGN_UP_EVENT)
            assertEquals(0, notificationCount(SIGN_UP_EVENT.userId))
        }
        awaitNotificationEnqueue()
        assertEquals(1, notificationCount(SIGN_UP_EVENT.userId))
        boundary.server.verify()
    }

    @Test
    fun `가입 이벤트는 트랜잭션 롤백 시 처리되지 않는다`() {
        // when
        transactionTemplate.executeWithoutResult { status ->
            eventPublisher.publishEvent(SIGN_UP_EVENT)
            status.setRollbackOnly()
        }
        awaitNotificationEnqueue()

        // then
        assertEquals(0, notificationCount(SIGN_UP_EVENT.userId))
        boundary.server.verify()
    }

    @Test
    fun `커밋 직전 검증에서 실패해도 HTTP 요청이 나가지 않는다`() {
        // when
        assertThrows(IllegalStateException::class.java) {
            transactionTemplate.executeWithoutResult {
                eventPublisher.publishEvent(SIGN_UP_EVENT)
                // 이벤트 동기화 등록 뒤 실패시켜 BEFORE_COMMIT 발송도 검출한다.
                TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                    override fun beforeCommit(readOnly: Boolean) {
                        throw IllegalStateException("커밋 직전 실패")
                    }
                })
            }
        }
        awaitNotificationEnqueue()

        // then
        assertEquals(0, notificationCount(SIGN_UP_EVENT.userId))
        boundary.server.verify()
    }

    @Test
    fun `트랜잭션 밖에서 발행된 이벤트는 발송하지 않는다`() {
        // when
        eventPublisher.publishEvent(SIGN_UP_EVENT)
        awaitNotificationEnqueue()

        // then
        assertEquals(0, notificationCount(SIGN_UP_EVENT.userId))
        boundary.server.verify()
    }

    private fun notificationCount(userId: Long): Int = checkNotNull(
        jdbc.queryForObject(
            "select count(*) from notifications where recipient_user_id = ?",
            Int::class.java,
            userId,
        ),
    )

    private fun awaitNotificationEnqueue() {
        val completed = CountDownLatch(1)
        notificationEnqueueTaskExecutor.execute { completed.countDown() }
        assertTrue(completed.await(5, TimeUnit.SECONDS), "알림 적재 실행기 작업 제한 시간 초과")
    }

    private fun stubLetsCareerUser(userId: Long, phoneNum: String = "010-1234-5678") {
        Mockito.`when`(letsCareerAuthClient.verify("lc-access")).thenReturn(
            LetsCareerUser(
                userId = userId, email = SIGN_UP_EVENT.email, name = SIGN_UP_EVENT.name,
                phoneNum = phoneNum, authProvider = SIGN_UP_EVENT.authProvider,
                nickname = null, profileImageUrl = null, isAdmin = false, updatedAt = null,
            ),
        )
    }

    @TestConfiguration
    internal class HttpBoundaryConfiguration {
        @Bean
        fun nhnHttpBoundary() = NhnHttpBoundary()

        /** 알림 행 적재 테스트에서는 NHN HTTP 요청이 발생하지 않아야 한다. */
        @Bean
        @Primary
        @Qualifier(UserAlimTalkConfiguration.NHN_ALIMTALK_REST_CLIENT)
        fun testNhnRestClient(boundary: NhnHttpBoundary): RestClient = boundary.restClient
    }

    private companion object {
        val SIGN_UP_EVENT = UserSignedUpEvent(
            userId = 17L,
            name = "김렛츠",
            email = "lets@career.co.kr",
            phoneNum = "010-1234-5678",
            authProvider = LetsCareerAuthProvider.SERVICE,
            joinedAt = LocalDateTime.of(2026, 8, 27, 10, 0),
        )
    }
}

class NhnHttpBoundary {
    private val builder = RestClient.builder()
    val server: MockRestServiceServer = MockRestServiceServer.bindTo(builder).build()
    val restClient: RestClient = builder.build()
    fun reset() = server.reset()
}
