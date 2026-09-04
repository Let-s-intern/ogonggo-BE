package com.ogonggo.adminapi.config

import com.ogonggo.adminapi.internal.implement.InternalApiKeyAuthenticationFilter
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
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
        // 문서를 열고 있는 오리진을 그대로 쓴다. 절대 URL을 적으면 서버가 인식한 주소(HTTP ALB)가
        // 새어 나가, HTTPS로 연 Swagger UI에서 Try it out이 mixed content로 차단된다.
        .servers(listOf(Server().url("/")))
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
