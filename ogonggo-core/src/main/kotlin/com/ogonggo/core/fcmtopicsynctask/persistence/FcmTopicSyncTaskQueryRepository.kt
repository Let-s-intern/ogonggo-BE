package com.ogonggo.core.fcmtopicsynctask.persistence

import com.ogonggo.core.fcmtopicsynctask.domain.QFcmTopicSyncTask.fcmTopicSyncTask as task
import com.ogonggo.core.fcmtopicsynctask.implement.dto.FcmTopicSyncTaskDto
import com.ogonggo.core.user.domain.QUser.user
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.userfcmtoken.domain.QUserFcmToken.userFcmToken as token
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
internal class FcmTopicSyncTaskQueryRepository(private val queries: JPAQueryFactory) {
    /** 없는 작업부터 채우므로 별도 커서 없이 재시작해도 최초 구독을 이어간다. 회원 상태 변화도 재평가한다. */
    fun reconciliation(topic: String, limit: Int): List<Pair<Long, String>> {
        val active = user.status.eq(UserStatus.ACTIVE)
        return queries.select(token.userId, token.token).from(token).join(user).on(user.id.eq(token.userId))
            .leftJoin(task).on(task.topic.eq(topic), task.tokenHash.eq(token.tokenHash))
            .where(token.deletedAt.isNull, token.token.isNotNull,
                task.id.isNull.and(active).or(task.id.isNotNull.and(task.desiredSubscribed.ne(
                    CaseBuilder().`when`(active).then(true).otherwise(false)))))
            .orderBy(token.id.asc()).limit(limit.toLong()).fetch()
            .map { checkNotNull(it.get(token.userId)) to checkNotNull(it.get(token.token)) }
    }

    fun pending(topic: String, now: LocalDateTime, limit: Int) = queries.selectFrom(task)
        .where(task.topic.eq(topic), task.pending.isTrue, task.nextAttemptAt.loe(now))
        .orderBy(task.nextAttemptAt.asc(), task.id.asc()).limit(limit.toLong()).fetch().map {
            FcmTopicSyncTaskDto(checkNotNull(it.id), it.revision, it.topic, checkNotNull(it.token), it.desiredSubscribed, it.attemptCount)
        }

    fun finish(dto: FcmTopicSyncTaskDto, now: LocalDateTime): Boolean = queries.update(task)
        .set(task.pending, false).setNull(task.token).setNull(task.lastErrorCode).set(task.updatedAt, now)
        .where(task.id.eq(dto.id), task.revision.eq(dto.revision), task.pending.isTrue).execute() == 1L

    fun retry(dto: FcmTopicSyncTaskDto, code: String, now: LocalDateTime): Boolean = queries.update(task)
        .set(task.attemptCount, dto.attemptCount + 1).set(task.lastErrorCode, code.take(100))
        .set(task.nextAttemptAt, now.plusSeconds(minOf(3600L, 5L shl minOf(dto.attemptCount, 10))))
        .set(task.updatedAt, now)
        .where(task.id.eq(dto.id), task.revision.eq(dto.revision), task.pending.isTrue).execute() == 1L

    fun countPending(topic: String): Long = queries.select(task.count()).from(task)
        .where(task.topic.eq(topic), task.pending.isTrue).fetchOne() ?: 0
}
