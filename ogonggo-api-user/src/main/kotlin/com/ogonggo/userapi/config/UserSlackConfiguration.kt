package com.ogonggo.userapi.config

import com.ogonggo.userapi.slack.SLACK_REST_CLIENT
import com.ogonggo.userapi.slack.SlackProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SlackProperties::class)
class UserSlackConfiguration {

    /**
     * `ogonggo.slack` 아래 웹훅들이 함께 쓰는 슬랙 클라이언트다.
     * 웹훅 주소가 곧 전체 URL이라 baseUrl은 두지 않고 호출 시점에 넘긴다.
     */
    @Bean(name = [SLACK_REST_CLIENT])
    fun slackRestClient(properties: SlackProperties): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(properties.readTimeout)
        }

        return RestClient.builder()
            .requestFactory(requestFactory)
            .build()
    }
}
