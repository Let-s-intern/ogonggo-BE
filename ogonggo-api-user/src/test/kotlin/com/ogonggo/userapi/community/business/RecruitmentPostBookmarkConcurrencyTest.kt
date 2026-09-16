package com.ogonggo.userapi.community.business

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.implement.RecruitmentPostAppendCommand
import com.ogonggo.core.community.implement.RecruitmentPostAppender
import com.ogonggo.core.error.ConflictException
import com.ogonggo.userapi.UserApiApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("concurrency")
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:recruitment-post-bookmark-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ogonggo.auth.jwt.secret=b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA==",
        "ogonggo.letscareer.base-url=http://localhost:8090",
        "ogonggo.letscareer.internal-api-key=test-internal-api-key",
        "spring.mail.host=localhost",
    ],
)
@ContextConfiguration(classes = [UserApiApplication::class, CoreJpaConfiguration::class])
class RecruitmentPostBookmarkConcurrencyTest @Autowired constructor(
    private val bookmarkService: RecruitmentPostBookmarkService,
    private val postAppender: RecruitmentPostAppender,
    private val jdbcTemplate: JdbcTemplate,
) {

    @Test
    fun `같은 사용자가 같은 모집글을 동시에 북마크하면 하나만 등록되고 나머지는 충돌한다`() {
        appendUser()
        val postId = appendPost()
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val results = (1..2).map {
                executor.submit<Result<Unit>> {
                    ready.countDown()
                    check(start.await(5, TimeUnit.SECONDS)) { "동시 등록 시작 신호를 받지 못했습니다." }
                    runCatching { bookmarkService.addBookmark(USER_ID, postId) }
                }
            }

            check(ready.await(5, TimeUnit.SECONDS)) { "동시 등록 요청이 준비되지 않았습니다." }
            start.countDown()

            val completed = results.map { it.get(10, TimeUnit.SECONDS) }
            assertEquals(1, completed.count(Result<Unit>::isSuccess))
            assertInstanceOf(ConflictException::class.java, completed.single(Result<Unit>::isFailure).exceptionOrNull())
            assertEquals(
                listOf(postId),
                bookmarkService.getBookmarks(USER_ID, cursor = null, size = 10).items.map { it.id },
            )
        } finally {
            executor.shutdownNow()
        }
    }

    private fun appendPost(): Long = checkNotNull(
        postAppender.append(
            RecruitmentPostAppendCommand(
                authorUserId = 1L,
                title = "동시성 검증용 모집글",
                recruitmentType = RecruitmentType.SIDE_PROJECT,
                capacity = 4,
                progressMethod = ProgressMethod.ONLINE,
                activityDurationMonths = 3,
                technologyStacks = listOf("Kotlin"),
                summary = "동시 북마크 등록을 검증합니다.",
                content = "{\"root\":{\"children\":[]}}",
                eligibilityAndSelectionProcess = null,
                recruitmentStartDate = LocalDate.of(2026, 9, 1),
                recruitmentEndDate = LocalDate.of(2026, 9, 30),
                positions = listOf(RecruitmentPosition.BACKEND),
                contactMethod = ContactMethod.EMAIL,
                contactValue = "team@example.com",
            ),
        ).id,
    )

    private fun appendUser() {
        val joinedAt = "2026-09-01 00:00:00"
        jdbcTemplate.update(
            """
            insert into users (id, letscareer_user_id, status, role, joined_at, created_at, updated_at)
            values (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            USER_ID,
            4821L,
            "ACTIVE",
            "USER",
            joinedAt,
            joinedAt,
            joinedAt,
        )
    }

    companion object {
        private const val USER_ID = 17L
    }
}
