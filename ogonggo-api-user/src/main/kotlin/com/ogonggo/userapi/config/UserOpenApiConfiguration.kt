package com.ogonggo.userapi.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val USER_BEARER_AUTH_SCHEME = "BearerAuth"

@Configuration
class UserOpenApiConfiguration {

    @Bean
    fun userOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Ogonggo User API")
                .version("v1"),
        )
        .components(
            Components().addSecuritySchemes(
                USER_BEARER_AUTH_SCHEME,
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"),
            ),
        )
}
