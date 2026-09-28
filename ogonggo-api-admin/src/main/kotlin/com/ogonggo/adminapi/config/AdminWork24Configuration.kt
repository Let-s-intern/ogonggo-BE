package com.ogonggo.adminapi.config

import com.ogonggo.adminapi.ingestion.work24.implement.WORK24_REST_CLIENT
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Properties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Work24Properties::class)
class AdminWork24Configuration {

    /**
     * 고용24 전용 클라이언트다.
     * 다른 외부 연동과 타임아웃이 달라 공용 RestClient를 쓰지 않는다.
     */
    @Bean(name = [WORK24_REST_CLIENT])
    fun work24RestClient(properties: Work24Properties): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(properties.readTimeout)
        }

        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }
}

