package com.ogonggo.core.time

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

@Configuration(proxyBeanMethods = false)
class TimeConfiguration {

    @Bean
    fun serviceClock(): Clock = Clock.system(ZoneId.of(SEOUL_TIME_ZONE))

    @Bean
    fun auditingDateTimeProvider(clock: Clock): DateTimeProvider =
        DateTimeProvider { Optional.of(LocalDateTime.now(clock)) }

    companion object {
        const val SEOUL_TIME_ZONE = "Asia/Seoul"
    }
}
