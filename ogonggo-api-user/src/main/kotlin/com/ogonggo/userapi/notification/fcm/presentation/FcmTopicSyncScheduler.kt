package com.ogonggo.userapi.notification.fcm.presentation

import com.ogonggo.userapi.notification.fcm.business.FcmTopicSyncService
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.stereotype.Component

@Component
class FcmTopicSyncScheduler(private val service: FcmTopicSyncService, private val observer: SchedulerExecutionObserver) {
    @SchedulerLock(name = SCHEDULER_NAME, lockAtMostFor = "PT5M")
    fun sync() = observer.observe(SCHEDULER_NAME) { service.sync() }

    companion object { const val SCHEDULER_NAME = "fcmTopicSync" }
}
