package com.ogonggo.userapi.notification.channel.alimtalk

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/** 기존 NHN 설정 키를 코드와의 호환을 위해 유지한다. 실제 운영 비밀값은 외부 설정에서만 공급한다. */
@Component
data class NhnAlimTalkProperties(
    @param:Value("\${nhn.appKey:}") val appKey: String = "",
    @param:Value("\${nhn.secretKey:}") val secretKey: String = "",
    @param:Value("\${nhn.sendKey:}") val sendKey: String = "",
    /** 기존 설정 호환용으로만 바인딩한다. 실제 알림별 템플릿 코드는 각 메시지에서 전달한다. */
    @param:Value("\${nhn.templateCode:}") val templateCode: String = "",
)
