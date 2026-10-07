package com.ogonggo.userapi.notification.intake.business

import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.job.implement.JobBookmarkReminderReader
import com.ogonggo.core.job.implement.JobBookmarkReminderScheduleManager
import com.ogonggo.core.job.implement.dto.JobBookmarkReminderCandidateDto
import com.ogonggo.core.job.implement.dto.JobBookmarkReminderScheduleDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.time.LocalDateTime
import javax.sql.DataSource

@SpringJUnitConfig(JobBookmarkReminderEnqueueServiceTransactionTest.TestConfiguration::class)
class JobBookmarkReminderEnqueueServiceTransactionTest {

    @Autowired
    private lateinit var enqueueService: JobBookmarkReminderEnqueueService

    @Autowired
    private lateinit var reminderReader: JobBookmarkReminderReader

    @Autowired
    private lateinit var notificationAppender: NotificationAppender

    @Autowired
    private lateinit var scheduleManager: JobBookmarkReminderScheduleManager

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun prepare() {
        jdbc.update("delete from reminder_page_results")
        Mockito.reset(reminderReader, notificationAppender, scheduleManager)
    }

    @Test
    fun `한 일정 페이지 실패는 앞서 커밋한 페이지를 되돌리지 않는다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 4, 9, 0)
        val firstSchedule = schedule(1)
        val secondSchedule = schedule(2)
        Mockito.`when`(scheduleManager.readDueSchedules(now, 10))
            .thenReturn(listOf(firstSchedule, secondSchedule))
        Mockito.`when`(reminderReader.readEligibleCandidates(firstSchedule, now, 500))
            .thenReturn(listOf(candidate()))
        Mockito.`when`(reminderReader.readEligibleCandidates(secondSchedule, now, 500))
            .thenThrow(IllegalStateException("second page failed"))
        Mockito.`when`(notificationAppender.appendAll(Mockito.anyCollection<NotificationAppendDto>())).thenAnswer {
            jdbc.update("insert into reminder_page_results (page_no) values (1)")
            1
        }

        // when
        assertThrows(IllegalStateException::class.java) { enqueueService.enqueueDue(now) }

        // then
        assertEquals(1, jdbc.queryForObject("select count(*) from reminder_page_results", Int::class.java))
    }

    private fun schedule(id: Long) = JobBookmarkReminderScheduleDto(
        id = id,
        jobId = id,
        recruitmentEndAt = LocalDateTime.of(2026, 10, 5, 9, 0),
        reminderAt = LocalDateTime.of(2026, 10, 4, 9, 0),
        lastBookmarkId = null,
    )

    private fun candidate() = JobBookmarkReminderCandidateDto(
        bookmarkId = 18,
        jobId = 1,
        userId = 7,
        recruitmentEndAt = LocalDateTime.of(2026, 10, 5, 9, 0),
        recipientNo = "01012345678",
        recipientName = "테스트 사용자",
        postingTitle = "테스트 공고",
    )

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import(JobBookmarkReminderEnqueueService::class)
    class TestConfiguration {
        @Bean
        fun dataSource(): DataSource = DriverManagerDataSource(
            "jdbc:h2:mem:reminder-enqueuer-transactions;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "sa",
            "",
        )

        @Bean
        fun transactionManager(dataSource: DataSource): PlatformTransactionManager =
            DataSourceTransactionManager(dataSource)

        @Bean
        fun jdbcTemplate(dataSource: DataSource) = JdbcTemplate(dataSource).apply {
            execute("create table if not exists reminder_page_results (page_no integer not null)")
        }

        @Bean
        fun reminderReader() = Mockito.mock(JobBookmarkReminderReader::class.java)

        @Bean
        fun notificationAppender() = Mockito.mock(NotificationAppender::class.java)

        @Bean
        fun objectMapper() = com.fasterxml.jackson.databind.ObjectMapper()

        @Bean
        fun scheduleManager() = Mockito.mock(JobBookmarkReminderScheduleManager::class.java)
    }
}
