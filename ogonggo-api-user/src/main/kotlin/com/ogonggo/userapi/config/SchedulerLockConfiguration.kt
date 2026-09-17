package com.ogonggo.userapi.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.dao.DataAccessException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT2H")
class SchedulerLockConfiguration {

    @Bean
    fun schedulerLockProvider(dataSource: DataSource): LockProvider {
        val jdbcTemplate = JdbcTemplate(dataSource)
        validateShedLockTable(jdbcTemplate)

        return JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(jdbcTemplate)
                .usingDbTime()
                .build(),
        )
    }

    private fun validateShedLockTable(jdbcTemplate: JdbcTemplate) {
        try {
            jdbcTemplate.queryForObject("select count(*) from shedlock", Long::class.java)
        } catch (exception: DataAccessException) {
            throw IllegalStateException(
                "ShedLock 테이블에 접근할 수 없습니다. 운영 DDL을 먼저 적용해야 합니다.",
                exception,
            )
        }
    }
}
