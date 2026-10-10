package com.ogonggo.userapi.user.business

import com.ogonggo.core.user.implement.LetsCareerJobProfileOutboxManager
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.LetsCareerJobProfileOutboxDto
import com.ogonggo.core.user.implement.dto.UserProfileDto
import com.ogonggo.userapi.user.implement.LetsCareerJobProfileReplaceCommand
import com.ogonggo.userapi.user.implement.LetsCareerUserClient
import java.time.Clock
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.client.RestClientException

/**
 * 오공고에서 고친 학력·희망 조건을 렛츠커리어로 보낸다.
 *
 * 렛츠커리어 응답을 기다리는 동안 DB 커넥션을 잡지 않도록 전체를 트랜잭션으로 묶지 않고,
 * 보낸 일시와 실패 횟수를 기록할 때만 짧게 연다.
 * 보낼 때 그 시점의 최신 값과 고친 일시를 읽어 보내므로 여러 번 보내거나 순서가 바뀌어도 결과가 같다.
 * 렛츠커리어는 더 나중에 고친 쪽의 값만 남긴다.
 */
@Service
class LetsCareerJobProfileSendService(
    private val outboxManager: LetsCareerJobProfileOutboxManager,
    private val userReader: UserReader,
    private val userProfileReader: UserProfileReader,
    private val letsCareerUserClient: LetsCareerUserClient,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {

    /** 보낼 변경을 보내고 보내지 못한 건수를 돌려준다. */
    fun sendPending(): Int = outboxManager.readPending().count { outbox -> !send(outbox) }

    private fun send(outbox: LetsCareerJobProfileOutboxDto): Boolean {
        val letsCareerUserId = userReader.read(outbox.userId).letsCareerUserId
        val profile = userProfileReader.read(outbox.userId)
        val updatedAt = profile?.jobInfoUpdatedAt
        if (letsCareerUserId == null || profile == null || updatedAt == null) {
            // 적재할 때와 달리 보낼 값이 없다. 보낼 수 없는 행이 남아 매번 실패하지 않도록 보낸 것으로 표시한다.
            markSent(outbox)
            return true
        }

        return try {
            letsCareerUserClient.replaceJobProfile(letsCareerUserId, profile.toReplaceCommand(updatedAt))
            markSent(outbox)
            true
        } catch (exception: RestClientException) {
            transactionTemplate.executeWithoutResult { outboxManager.markFailed(outbox) }
            log.warn(
                "렛츠커리어 학력·희망 조건 동기화에 실패했습니다. userId={}, attemptCount={}",
                outbox.userId,
                outbox.attemptCount + 1,
                exception,
            )
            false
        }
    }

    private fun markSent(outbox: LetsCareerJobProfileOutboxDto) {
        transactionTemplate.executeWithoutResult { outboxManager.markSent(outbox, LocalDateTime.now(clock)) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(LetsCareerJobProfileSendService::class.java)
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
