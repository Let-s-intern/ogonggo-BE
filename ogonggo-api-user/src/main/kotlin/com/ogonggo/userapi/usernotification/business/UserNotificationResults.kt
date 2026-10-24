package com.ogonggo.userapi.usernotification.business

import com.ogonggo.core.usernotification.implement.dto.UserNotificationDto

data class UserNotificationPageResult(
    val items: List<UserNotificationDto>,
    val nextCursor: String?,
    val hasNext: Boolean,
)
