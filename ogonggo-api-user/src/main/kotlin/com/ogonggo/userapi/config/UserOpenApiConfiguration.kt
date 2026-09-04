package com.ogonggo.userapi.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
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
        // 문서를 열고 있는 오리진을 그대로 쓴다. 절대 URL을 적으면 서버가 인식한 주소(HTTP ALB)가
        // 새어 나가, HTTPS로 연 Swagger UI에서 Try it out이 mixed content로 차단된다.
        .servers(listOf(Server().url("/")))
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
