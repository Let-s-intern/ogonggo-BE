package com.ogonggo.core.fcmtopicsynctask.implement.dto

data class FcmTopicSyncTaskDto(val id: Long, val revision: Long, val topic: String, val token: String,
    val subscribed: Boolean, val attemptCount: Int)
