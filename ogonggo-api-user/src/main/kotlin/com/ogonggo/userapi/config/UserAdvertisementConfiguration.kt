package com.ogonggo.userapi.config

import com.ogonggo.userapi.advertisement.implement.ADVERTISEMENT_SLACK_REST_CLIENT
import com.ogonggo.userapi.advertisement.implement.AdvertisementSlackProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdvertisementSlackProperties::class)
class UserAdvertisementConfiguration {

    /**
     * 광고 문의 알림 전용 슬랙 클라이언트다.
     *
     * 요청 처리 중에 동기로 호출하므로 타임아웃을 짧게 둔다.
     * 슬랙이 느려질 때 문의 폼 응답이 그만큼 늘어지면 안 된다.
     * 웹훅 주소가 곧 전체 URL이라 baseUrl은 두지 않고 호출 시점에 넘긴다.
     */
    @Bean(name = [ADVERTISEMENT_SLACK_REST_CLIENT])
    fun advertisementSlackRestClient(properties: AdvertisementSlackProperties): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(properties.readTimeout)
        }

        return RestClient.builder()
            .requestFactory(requestFactory)
            .build()
    }
}
