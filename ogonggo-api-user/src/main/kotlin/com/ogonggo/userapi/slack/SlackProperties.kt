package com.ogonggo.userapi.slack

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 슬랙 인커밍 웹훅 설정이다. 알림이 늘어나면 웹훅 주소를 여기에 이름별로 하나씩 붙인다.
 *
 * 웹훅 주소의 기본값은 빈 문자열이다. 값이 없어도 애플리케이션은 기동하고,
 * 빈 주소를 만났을 때 건너뛸지 실패로 볼지는 알림마다 정한다.
 * 광고 문의 웹훅은 운영 설정이 이미 `ogonggo.advertisement.slack` 아래에 있어 아직 옮기지 않았다.
 */
@ConfigurationProperties(prefix = "ogonggo.slack")
data class SlackProperties(
    /** 기업회원이 채용공고·부트캠프를 등록했을 때 알림을 받을 채널의 웹훅 */
    val companyPosting: String = "",
    val connectTimeout: Duration = Duration.ofSeconds(2),
    val readTimeout: Duration = Duration.ofSeconds(5),
)

internal const val SLACK_REST_CLIENT = "slackRestClient"
