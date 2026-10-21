package com.ogonggo.userapi.mail

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 오공고가 보내는 메일의 공통 설정이다. SMTP 접속 정보는 Spring Boot가 spring.mail.*로 받는다.
 *
 * from은 Spring Boot의 표준 키가 아니라 여기서 따로 받는다. 광고 문의 확인 메일과 이메일 알림이 함께 쓴다.
 * 기본값을 빈 문자열로 둬서 값이 없어도 애플리케이션은 기동한다.
 * 발신 주소가 없는 메일은 SES가 거절하므로, 빈 주소를 만났을 때 건너뛸지 실패로 볼지는 메일마다 정한다.
 */
@ConfigurationProperties(prefix = "ogonggo.mail")
data class OgonggoMailProperties(
    val from: String = "",
)
