package com.ogonggo.core.fcmtopicsynctask.persistence

import com.ogonggo.core.fcmtopicsynctask.domain.FcmTopicSyncTask
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface FcmTopicSyncTaskJpaRepository : JpaRepository<FcmTopicSyncTask, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from FcmTopicSyncTask t where t.topic = :topic and t.tokenHash = :hash")
    fun findForUpdate(@Param("topic") topic: String, @Param("hash") hash: String): FcmTopicSyncTask?
}
