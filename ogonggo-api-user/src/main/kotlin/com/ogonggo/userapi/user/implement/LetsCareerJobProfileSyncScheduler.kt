package com.ogonggo.userapi.user.implement

import com.ogonggo.core.user.implement.LetsCareerJobProfileOutboxManager
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.LetsCareerJobProfileOutboxDto
import com.ogonggo.core.user.implement.dto.UserProfileDto
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import java.time.LocalDateTime

/**
 * 오공고에서 고친 학력·희망 조건을 렛츠커리어로 보낸다.
 *
 * 렛츠커리어 응답을 기다리는 동안 DB 커넥션을 잡지 않도록 트랜잭션으로 묶지 않는다.
 * 보낼 때 그 시점의 최신 값과 고친 일시를 읽어 보내므로 여러 번 보내거나 순서가 바뀌어도 결과가 같다.
 * 렛츠커리어는 더 나중에 고친 쪽의 값만 남긴다.
 */
@Component
class LetsCareerJobProfileSyncScheduler(
    private val outboxManager: LetsCareerJobProfileOutboxManager,
    private val userReader: UserReader,
    private val userProfileReader: UserProfileReader,
    private val letsCareerUserClient: LetsCareerUserClient,
    private val schedulerExecutionObserver: SchedulerExecutionObserver,
) {

    /** 실행 주기와 켜짐 여부는 `scheduled_jobs`가 정한다. `UserScheduledJobConfiguration` 참고. */
    @SchedulerLock(name = SCHEDULER_NAME, lockAtLeastFor = "PT20S", lockAtMostFor = "PT5M")
    fun sendPending() {
        val failedCount = schedulerExecutionObserver.observe(SCHEDULER_NAME) {
            outboxManager.readPending().count { outbox -> !send(outbox) }
        }
        if (failedCount > 0) {
            log.warn("렛츠커리어로 보내지 못한 학력·희망 조건이 있습니다. failedCount={}", failedCount)
        }
    }

    private fun send(outbox: LetsCareerJobProfileOutboxDto): Boolean {
        val letsCareerUserId = userReader.read(outbox.userId).letsCareerUserId
        val profile = userProfileReader.read(outbox.userId)
        val updatedAt = profile?.jobInfoUpdatedAt
        if (letsCareerUserId == null || profile == null || updatedAt == null) {
            // 적재할 때와 달리 보낼 값이 없다. 보낼 수 없는 행이 남아 매번 실패하지 않도록 지운다.
            outboxManager.markSent(outbox)
            return true
        }

        return try {
            letsCareerUserClient.replaceJobProfile(letsCareerUserId, profile.toReplaceCommand(updatedAt))
            outboxManager.markSent(outbox)
            true
        } catch (exception: RestClientException) {
            outboxManager.markFailed(outbox)
            log.warn(
                "렛츠커리어 학력·희망 조건 동기화에 실패했습니다. userId={}, attemptCount={}",
                outbox.userId,
                outbox.attemptCount + 1,
                exception,
            )
            false
        }
    }

    companion object {
        const val SCHEDULER_NAME = "letsCareerJobProfileSync"
        private val log = LoggerFactory.getLogger(LetsCareerJobProfileSyncScheduler::class.java)
    }
}

private fun UserProfileDto.toReplaceCommand(updatedAt: LocalDateTime) = LetsCareerJobProfileReplaceCommand(
    university = university,
    major = major,
    grade = grade,
    wishField = wishField,
    wishJob = wishJob,
    wishIndustry = wishIndustry,
    wishEmploymentType = wishEmploymentType,
    wishCompany = wishCompany,
    updatedAt = updatedAt,
)
