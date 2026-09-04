package com.ogonggo.userapi.advertisement.implement

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 광고 도메인이 쓰는 슬랙 인커밍 웹훅 설정이다.
 *
 * URL 키는 `webhook-url`처럼 뭉뚱그리지 않고 어떤 알림인지로 이름을 붙인다.
 * 광고 도메인에 알림이 하나 더 생기면 `inquiry-url` 옆에 다른 이름으로 붙고,
 * 다른 도메인의 웹훅은 각자의 `ogonggo.<도메인>.slack` 아래에 둔다.
 *
 * inquiryUrl의 기본값을 빈 문자열로 둬서 값이 없어도 애플리케이션은 기동한다.
 * 웹훅 하나 때문에 사용자 API 전체가 뜨지 못하면 피해가 훨씬 크다.
 * 값이 없을 때 문의를 조용히 버리지 않도록, 발송 시점에 실패로 처리한다.
 */
@ConfigurationProperties(prefix = "ogonggo.advertisement.slack")
data class AdvertisementSlackProperties(
    /** 광고 문의 접수 알림을 받을 채널의 웹훅 */
    val inquiryUrl: String = "",
    val connectTimeout: Duration = Duration.ofSeconds(2),
    val readTimeout: Duration = Duration.ofSeconds(5),
)

/**
 * 접수 확인 메일 설정이다. SMTP 접속 정보는 Spring Boot가 spring.mail.*로 받는다.
 *
 * from은 Spring Boot의 표준 키가 아니라 여기서 따로 받는다.
 * 비어 있으면 발송을 건너뛴다. 발신 주소가 없는 메일은 SES가 거절한다.
 */
@ConfigurationProperties(prefix = "ogonggo.advertisement.mail")
data class AdvertisementMailProperties(
    val from: String = "",
)
