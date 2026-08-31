package com.ogonggo.core.common

import com.ogonggo.core.time.TimeConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@Import(TimeConfiguration::class)
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@EntityScan(basePackages = ["com.ogonggo.core"])
@EnableJpaRepositories(basePackages = ["com.ogonggo.core"])
class CoreJpaConfiguration
