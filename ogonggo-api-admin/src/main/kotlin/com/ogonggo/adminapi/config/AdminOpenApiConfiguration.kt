package com.ogonggo.adminapi.config

import com.ogonggo.adminapi.internal.implement.InternalApiKeyAuthenticationFilter
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val ADMIN_BEARER_AUTH_SCHEME = "BearerAuth"
const val ADMIN_INTERNAL_API_KEY_SCHEME = "InternalApiKey"

@Configuration
class AdminOpenApiConfiguration {

    @Bean
    fun adminOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Ogonggo Admin API")
                .version("v1"),
        )
        .components(
            Components()
                .addSecuritySchemes(
                    ADMIN_BEARER_AUTH_SCHEME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                )
                .addSecuritySchemes(
                    ADMIN_INTERNAL_API_KEY_SCHEME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .`in`(SecurityScheme.In.HEADER)
                        .name(InternalApiKeyAuthenticationFilter.INTERNAL_API_KEY_HEADER),
                ),
        )
}
