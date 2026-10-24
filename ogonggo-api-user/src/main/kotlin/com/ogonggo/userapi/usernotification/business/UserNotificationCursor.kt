package com.ogonggo.userapi.usernotification.business

import com.ogonggo.core.usernotification.implement.dto.UserNotificationCursorDto
import com.ogonggo.core.usernotification.implement.dto.UserNotificationDto
import com.ogonggo.userapi.error.InvalidRequestParameterException
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
import java.util.Base64

internal object UserNotificationCursor {
    fun encode(item: UserNotificationDto): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("${item.createdAt}|${item.id ?: item.broadcastId}".toByteArray(UTF_8))

    fun decode(value: String?): UserNotificationCursorDto? {
        if (value == null) return null
        try {
            require(value.length in 1..200 && value.matches(Regex("[A-Za-z0-9_-]+")))
            val parts = String(Base64.getUrlDecoder().decode(value), UTF_8).split('|')
            require(parts.size == 2)
            val createdAt = LocalDateTime.parse(parts[0])
            require(createdAt.year in 1..9999)
            val id = parts[1].toLongOrNull()
            if (id != null) {
                require(id > 0)
                return UserNotificationCursorDto(createdAt, id)
            }
            val uuid = java.util.UUID.fromString(parts[1])
            require(uuid.toString() == parts[1])
            return UserNotificationCursorDto(createdAt, null, uuid.toString())
        } catch (_: IllegalArgumentException) {
            throw InvalidRequestParameterException("cursor", "올바른 알림 커서가 아닙니다.")
        } catch (_: java.time.format.DateTimeParseException) {
            throw InvalidRequestParameterException("cursor", "올바른 알림 커서가 아닙니다.")
        }
    }
}
