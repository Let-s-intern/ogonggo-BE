package com.ogonggo.core.common

import com.ogonggo.core.time.TimeConfiguration
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@Import(TimeConfiguration::class)
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@EntityScan(basePackages = ["com.ogonggo.core"])
@EnableJpaRepositories(basePackages = ["com.ogonggo.core"])
class CoreJpaConfiguration {

    /** 선택 필터가 붙는 목록 조회를 동적 쿼리로 조립하기 위해 사용한다. */
    @Bean
    fun jpaQueryFactory(entityManager: EntityManager): JPAQueryFactory = JPAQueryFactory(entityManager)
}
