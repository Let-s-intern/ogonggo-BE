package com.ogonggo.userapi.notification.channel.alimtalk

/**
 * NHN 등록 템플릿에 전달할 요청 값이다.
 * 템플릿 본문과 버튼은 NHN에 등록된 내용이므로 애플리케이션에서 재구성하지 않는다.
 */
data class AlimTalkMessage(
    val recipientNo: String,
    val templateCode: String,
    val templateParameters: Map<String, String>,
    val idempotencyKey: String? = null,
)
