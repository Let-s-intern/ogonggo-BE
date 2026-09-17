package com.ogonggo.userapi.scheduling

import com.ogonggo.core.community.implement.RecruitmentPostManager
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.userapi.community.implement.RecruitmentPostAutoCloseScheduler
import com.ogonggo.userapi.config.SchedulerLockConfiguration
import com.ogonggo.userapi.image.implement.ImageAssetCleanupScheduler
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.aop.support.AopUtils
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SchedulerLockIntegrationTest {

    @Test
    fun `실제 스케줄러는 중복 실행을 막고 서로 다른 잠금을 사용한다`() {
        // given
        val dataSource = DriverManagerDataSource(
            "jdbc:h2:mem:scheduler-lock-integration;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "sa",
            "",
        )
        ResourceDatabasePopulator(ClassPathResource("schema.sql")).execute(dataSource)

        val postManager = Mockito.mock(RecruitmentPostManager::class.java)
        val imageAssetManager = Mockito.mock(ImageAssetManager::class.java)
        val context = AnnotationConfigApplicationContext()
        context.beanFactory.registerSingleton("dataSource", dataSource)
        context.beanFactory.registerSingleton("recruitmentPostManager", postManager)
        context.beanFactory.registerSingleton("imageAssetManager", imageAssetManager)
        context.beanFactory.registerSingleton("clock", CLOCK)
        context.beanFactory.registerSingleton("meterRegistry", SimpleMeterRegistry())
        context.register(
            SchedulerLockConfiguration::class.java,
            SchedulerExecutionObserver::class.java,
            RecruitmentPostAutoCloseScheduler::class.java,
            ImageAssetCleanupScheduler::class.java,
        )

        try {
            context.refresh()
            val postScheduler = context.getBean(RecruitmentPostAutoCloseScheduler::class.java)
            val imageScheduler = context.getBean(ImageAssetCleanupScheduler::class.java)
            assertTrue(AopUtils.isAopProxy(postScheduler))
            assertTrue(AopUtils.isAopProxy(imageScheduler))

            // when
            postScheduler.closeExpiredRecruitmentPosts()
            postScheduler.closeExpiredRecruitmentPosts()
            imageScheduler.cleanup()
            imageScheduler.cleanup()

            // then
            Mockito.verify(postManager, Mockito.times(1)).closeExpired(TODAY, NOW)
            Mockito.verify(imageAssetManager, Mockito.times(1)).cleanup(NOW, Duration.ofHours(24))
        } finally {
            context.close()
        }
    }

    private companion object {
        val CLOCK: Clock = Clock.fixed(
            Instant.parse("2026-09-17T00:00:00Z"),
            ZoneId.of("Asia/Seoul"),
        )
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 17, 9, 0)
        val TODAY: LocalDate = LocalDate.of(2026, 9, 17)
    }
}
