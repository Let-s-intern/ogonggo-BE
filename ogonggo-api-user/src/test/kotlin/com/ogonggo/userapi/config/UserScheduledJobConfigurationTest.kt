package com.ogonggo.userapi.config

import com.ogonggo.core.schedule.implement.dto.ScheduledJobDefinition
import com.ogonggo.userapi.notification.delivery.implement.NotificationCleanupScheduler
import com.ogonggo.userapi.notification.delivery.implement.NotificationDispatcher
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class UserScheduledJobConfigurationTest {

    @Test
    fun `발송 대기열은 매초 실행되고 다중 인스턴스는 ShedLock으로 직렬화한다`() {
        // given
        val dispatcher = Mockito.mock(NotificationDispatcher::class.java)

        // when
        val definition = UserScheduledJobConfiguration().notificationDeliveryJob(dispatcher)
        val lock = NotificationDispatcher::class.java.getMethod("dispatch").getAnnotation(SchedulerLock::class.java)

        // then
        assertEquals("notificationDelivery", definition.name)
        assertEquals("* * * * * *", definition.defaultCron)
        assertNotNull(lock)
        assertEquals(definition.name, lock.name)
    }

    @Test
    fun `템플릿 승인 전에는 스크랩 리마인드 스케줄 정의를 등록하지 않는다`() {
        // given
        val jobDefinitions = UserScheduledJobConfiguration::class.java.declaredMethods
            .filter { method -> method.returnType == ScheduledJobDefinition::class.java }
            .map { method -> method.name }

        // then
        assertFalse("jobBookmarkAlimTalkReminderJob" in jobDefinitions)
    }

    @Test
    fun `알림 정리는 매일 등록하고 ShedLock을 사용한다`() {
        // given
        val scheduler = Mockito.mock(NotificationCleanupScheduler::class.java)

        // when
        val definition = UserScheduledJobConfiguration().notificationCleanupJob(scheduler)
        val lock = NotificationCleanupScheduler::class.java.getMethod("cleanup").getAnnotation(SchedulerLock::class.java)

        // then
        assertEquals("notificationCleanup", definition.name)
        assertEquals("0 30 3 * * *", definition.defaultCron)
        assertNotNull(lock)
        assertEquals(definition.name, lock.name)
    }
}
