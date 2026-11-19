package com.ogonggo.core.advertisementevent.implement

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "ogonggo.posthog.endpoints")
class PosthogEndpointsProperties {
    var apiHost: String = "https://us.posthog.com"
    var projectId: String = ""
    var endpointName: String = "advertisement-contract-report"
    var apiKey: String = ""
    var reportVersion: Int = 1
    var connectTimeout: Duration = Duration.ofSeconds(2)
    var readTimeout: Duration = Duration.ofSeconds(8)
    var cacheTtl: Duration = Duration.ofSeconds(60)
    var pageSize: Int = 1_000
    var maxRows: Int = 20_000

    fun isConfigured(): Boolean =
        apiHost.isNotBlank() && projectId.isNotBlank() && endpointName.isNotBlank() && apiKey.isNotBlank() &&
            reportVersion > 0 && pageSize in 1..1_000 && maxRows >= pageSize

    override fun toString(): String =
        "PosthogEndpointsProperties(apiHost=$apiHost, projectId=$projectId, endpointName=$endpointName, reportVersion=$reportVersion, pageSize=$pageSize, maxRows=$maxRows, apiKey=<redacted>)"
}
