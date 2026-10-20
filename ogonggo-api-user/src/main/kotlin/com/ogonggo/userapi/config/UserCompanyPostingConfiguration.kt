package com.ogonggo.userapi.config

import com.ogonggo.userapi.companyposting.implement.COMPANY_POSTING_SLACK_REST_CLIENT
import com.ogonggo.userapi.companyposting.implement.CompanyPostingSlackProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CompanyPostingSlackProperties::class)
class UserCompanyPostingConfiguration {

    /**
     * 기업회원 등록 알림 전용 슬랙 클라이언트다.
     * 웹훅 주소가 곧 전체 URL이라 baseUrl은 두지 않고 호출 시점에 넘긴다.
     */
    @Bean(name = [COMPANY_POSTING_SLACK_REST_CLIENT])
    fun companyPostingSlackRestClient(properties: CompanyPostingSlackProperties): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(properties.readTimeout)
        }

        return RestClient.builder()
            .requestFactory(requestFactory)
            .build()
    }
}
