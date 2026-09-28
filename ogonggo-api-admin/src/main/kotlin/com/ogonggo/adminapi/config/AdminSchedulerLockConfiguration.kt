package com.ogonggo.adminapi.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/**
 * 관리자 API도 태스크가 여럿 뜰 수 있어 스케줄 작업을 한 곳에서만 돌린다.
 * 사용자 API와 같은 `shedlock` 테이블을 쓰며, 잠금 이름으로 서로 구분한다.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT2H")
class AdminSchedulerLockConfiguration {

    @Bean
    fun schedulerLockProvider(dataSource: DataSource): LockProvider {
        return JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(JdbcTemplate(dataSource))
                .usingDbTime()
                .build(),
        )
    }
}
