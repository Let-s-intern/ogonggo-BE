package com.ogonggo.userapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.http.client.SimpleClientHttpRequestFactory
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class UserAlimTalkConfiguration {

    @Bean(name = [NHN_ALIMTALK_REST_CLIENT])
    fun nhnAlimTalkRestClient(): RestClient = RestClient.builder()
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(3))
            setReadTimeout(Duration.ofSeconds(5))
        })
        .build()

    companion object {
        const val NHN_ALIMTALK_REST_CLIENT = "nhnAlimTalkRestClient"
    }
}
