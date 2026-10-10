package com.ogonggo.userapi.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.core.LockConfiguration as ShedLockConfiguration
import net.javacrumbs.shedlock.core.SimpleLock
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT2H")
class SchedulerLockConfiguration {

    @Bean
    fun schedulerLockProvider(dataSource: DataSource): LockProvider {
        val provider = JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(JdbcTemplate(dataSource))
                .usingDbTime()
                .build(),
        )
        return LoggingLockProvider(provider)
    }
}

internal class LoggingLockProvider(
    private val delegate: LockProvider,
) : LockProvider {

    override fun lock(lockConfiguration: ShedLockConfiguration): java.util.Optional<SimpleLock> {
        val lock = delegate.lock(lockConfiguration)
        if (lock.isEmpty) {
            log.info("스케줄러 잠금을 획득하지 못해 이번 실행을 건너뜁니다. scheduler={}", lockConfiguration.name)
        }
        return lock
    }

    private companion object {
        val log = LoggerFactory.getLogger(LoggingLockProvider::class.java)
    }
}
