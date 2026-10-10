package com.ogonggo.core.job.implement

import java.time.LocalDateTime

/** 리마인드 알림의 발송 건 식별자를 만들고 공고별 접두어로 취소 범위를 지정한다. */
object JobBookmarkReminderNotificationKey {
    /** 이 접두어는 해당 공고의 이전·현재 마감 알림을 찾는 취소 범위로 사용한다. */
    fun jobPrefix(jobId: Long): String =
        "clip-remind:job:$jobId:"

    /** 같은 공고·D-1 시각·수신자의 재평가는 같은 행으로 수렴한다. */
    fun forRecipient(jobId: Long, userId: Long, channel: String, reminderAt: LocalDateTime): String =
        "${recipientPrefix(jobId, userId, channel)}$reminderAt"

    fun recipientPrefix(jobId: Long, userId: Long, channel: String): String =
        "${jobPrefix(jobId)}user:$userId:$channel:at:"
}
