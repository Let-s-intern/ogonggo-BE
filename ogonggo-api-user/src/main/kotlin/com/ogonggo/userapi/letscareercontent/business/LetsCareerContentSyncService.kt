package com.ogonggo.userapi.letscareercontent.business

import com.ogonggo.core.letscareercontent.implement.LetsCareerContentManager
import com.ogonggo.core.letscareercontent.implement.dto.LetsCareerContentSyncResultDto
import com.ogonggo.userapi.letscareercontent.implement.LetsCareerContentClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDateTime

/**
 * 렛츠커리어 콘텐츠 사본을 렛츠커리어 목록으로 덮어쓴다.
 * 렛츠커리어 응답을 기다리는 동안 DB 커넥션을 잡지 않도록 받은 뒤에 짧게 트랜잭션을 연다.
 */
@Service
class LetsCareerContentSyncService(
    private val letsCareerContentClient: LetsCareerContentClient,
    private val letsCareerContentManager: LetsCareerContentManager,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {

    /**
     * 받은 목록이 비어 있으면 덮어쓰지 않는다. 렛츠커리어 쪽 일시 오류로 빈 목록이 오면 사본이 모두 지워지고
     * 다시 태그를 붙여야 하기 때문이다. 노출 콘텐츠가 정말 하나도 없는 경우는 없다고 본다.
     *
     * @return 덮어쓴 결과, 덮어쓰지 않았으면 `null`
     */
    fun sync(): LetsCareerContentSyncResultDto? {
        val contents = letsCareerContentClient.readCatalog()
        if (contents.isEmpty()) {
            log.warn("렛츠커리어 콘텐츠 목록이 비어 있어 사본을 덮어쓰지 않았습니다.")
            return null
        }
        return transactionTemplate.execute {
            letsCareerContentManager.replaceAll(contents, LocalDateTime.now(clock))
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(LetsCareerContentSyncService::class.java)
    }
}
