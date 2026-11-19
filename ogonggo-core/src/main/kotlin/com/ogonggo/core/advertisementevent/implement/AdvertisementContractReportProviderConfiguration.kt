package com.ogonggo.core.advertisementevent.implement

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Clock

/** Source selection is shared so customer and administrator report exports cannot silently diverge. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PosthogEndpointsProperties::class)
class AdvertisementContractReportProviderConfiguration {

    @Bean("posthogAdvertisementReportRestClient")
    fun posthogAdvertisementReportRestClient(properties: PosthogEndpointsProperties): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(properties.readTimeout)
        }
        return RestClient.builder().requestFactory(requestFactory).build()
    }

    @Bean
    fun posthogEndpointsClient(
        @org.springframework.beans.factory.annotation.Qualifier("posthogAdvertisementReportRestClient") restClient: RestClient,
        properties: PosthogEndpointsProperties,
        clock: Clock,
    ) = PosthogEndpointsClient(restClient, properties, clock)

    @Bean
    fun posthogAdvertisementReportEndpointMapper() = PosthogAdvertisementReportEndpointMapper()

    @Bean
    @ConditionalOnProperty(name = ["ogonggo.advertisement-report.source"], havingValue = "MYSQL", matchIfMissing = true)
    fun mySqlAdvertisementContractReportProvider(reader: AdvertisementEventReportReader): AdvertisementContractReportProvider =
        MySqlAdvertisementContractReportProvider(reader)

    @Bean
    @ConditionalOnProperty(name = ["ogonggo.advertisement-report.source"], havingValue = "POSTHOG")
    fun posthogAdvertisementContractReportProvider(
        endpointsClient: PosthogEndpointsClient,
        mapper: PosthogAdvertisementReportEndpointMapper,
    ): AdvertisementContractReportProvider = PosthogAdvertisementContractReportProvider(endpointsClient, mapper)
}
