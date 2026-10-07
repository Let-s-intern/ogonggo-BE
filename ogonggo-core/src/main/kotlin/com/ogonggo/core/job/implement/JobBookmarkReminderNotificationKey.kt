package com.ogonggo.core.job.implement

import java.util.UUID

/** 리마인드 알림의 발송 건 식별자를 만들고 공고별 접두어로 취소 범위를 지정한다. */
object JobBookmarkReminderNotificationKey {
    /** 이 접두어는 해당 공고의 이전·현재 마감 알림을 찾는 취소 범위로 사용한다. */
    fun jobPrefix(jobId: Long): String =
        "clip-remind:job:$jobId:"

    /**
     * 마감 변경으로 다시 발송해야 하는 건은 이전 건과 다른 UUID를 사용한다.
     * 페이지 적재와 커서 갱신이 한 트랜잭션이므로 롤백 시 생성된 키만 남는 문제는 없다.
     */
    fun forRecipient(jobId: Long, userId: Long, channel: String): String =
        "${jobPrefix(jobId)}${UUID.randomUUID()}:user:$userId:$channel"
}
