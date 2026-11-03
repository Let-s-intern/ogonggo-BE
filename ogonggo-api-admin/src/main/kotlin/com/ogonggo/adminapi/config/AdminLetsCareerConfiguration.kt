package com.ogonggo.adminapi.config

import com.ogonggo.adminapi.notificationbatch.implement.AdminLetsCareerProperties
import com.ogonggo.adminapi.notificationbatch.implement.LetsCareerMarketingAgreementClient.Companion.LETSCAREER_REST_CLIENT
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminLetsCareerProperties::class)
class AdminLetsCareerConfiguration {

    /** 렛츠커리어 내부 API 전용 클라이언트다. 관리자 발송 요청 안에서 부르므로 타임아웃을 짧게 둔다. */
    @Bean(name = [LETSCAREER_REST_CLIENT])
    fun adminLetsCareerRestClient(properties: AdminLetsCareerProperties): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(properties.readTimeout)
        }
        return RestClient.builder()
            .baseUrl(properties.baseUrl.ifBlank { "http://letscareer.invalid" })
            .requestFactory(requestFactory)
            .build()
    }
}
