package com.ogonggo.userapi.config

import net.javacrumbs.shedlock.core.LockConfiguration
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import java.time.Duration
import java.time.Instant

class SchedulerLockConfigurationTest {

    @Test
    fun `잠금 테이블이 없어도 LockProvider를 초기화한다`() {
        // given
        val dataSource = DriverManagerDataSource(
            "jdbc:h2:mem:scheduler-lock-missing;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "sa",
            "",
        )

        // when
        val provider = SchedulerLockConfiguration().schedulerLockProvider(dataSource)

        // then
        assertNotNull(provider)
    }

    @Test
    fun `같은 DB를 사용하는 두 인스턴스는 동일한 스케줄러 잠금을 동시에 획득하지 않는다`() {
        // given
        val dataSource = DriverManagerDataSource(
            "jdbc:h2:mem:scheduler-lock-shared;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "sa",
            "",
        )
        ResourceDatabasePopulator(ClassPathResource("schema.sql")).execute(dataSource)
        val configuration = SchedulerLockConfiguration()
        val firstProvider = configuration.schedulerLockProvider(dataSource)
        val secondProvider = configuration.schedulerLockProvider(dataSource)
        val lockConfiguration = LockConfiguration(
            Instant.parse("2099-01-01T00:00:00Z"),
            "sharedScheduler",
            Duration.ofMinutes(1),
            Duration.ZERO,
        )

        // when
        val firstLock = firstProvider.lock(lockConfiguration)
        val competingLock = secondProvider.lock(lockConfiguration)

        // then
        assertTrue(firstLock.isPresent)
        assertTrue(competingLock.isEmpty)

        firstLock.orElseThrow().unlock()
        val lockAfterRelease = secondProvider.lock(lockConfiguration)
        assertTrue(lockAfterRelease.isPresent)
        lockAfterRelease.orElseThrow().unlock()
    }
}
