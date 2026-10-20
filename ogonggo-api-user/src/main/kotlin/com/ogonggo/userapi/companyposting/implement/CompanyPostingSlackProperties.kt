package com.ogonggo.userapi.companyposting.implement

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 기업회원 등록 알림이 쓰는 슬랙 인커밍 웹훅 설정이다.
 *
 * registeredUrl의 기본값을 빈 문자열로 둬서 값이 없어도 애플리케이션은 기동한다.
 * 광고 문의와 달리 공고는 DB에 남아 있어 알림을 건너뛰어도 잃는 것이 없다.
 */
@ConfigurationProperties(prefix = "ogonggo.company-posting.slack")
data class CompanyPostingSlackProperties(
    /** 기업회원이 공고·부트캠프를 등록했을 때 알림을 받을 채널의 웹훅 */
    val registeredUrl: String = "",
    val connectTimeout: Duration = Duration.ofSeconds(2),
    val readTimeout: Duration = Duration.ofSeconds(5),
)
