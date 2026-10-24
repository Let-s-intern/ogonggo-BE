package com.ogonggo.core.notification.domain

import com.ogonggo.core.enumeration.EnumField

/** DIRECT는 채널별 개별 주소, TOPIC은 FCM 구독 그룹을 의미한다. */
enum class NotificationRecipientType(override val code: Int, override val desc: String) : EnumField {
    DIRECT(1, "개별 주소"),
    TOPIC(2, "FCM 토픽"),
}
