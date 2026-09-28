package com.ogonggo.core.schedule.implement.dto

/** 코드가 정한 작업이다. DB에 행이 없을 때 [defaultCron]으로 만든다. */
data class ScheduledJobDefinition(
    val name: String,
    val defaultCron: String,
    val description: String,
    val action: () -> Unit,
)

/** DB에 저장된 작업 설정이다. */
data class ScheduledJobSettingDto(
    val name: String,
    val cron: String,
    val enabled: Boolean,
)
