package com.ogonggo.userapi.health

import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.actuate.health.Status
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * ALB 헬스 체크와 배포 워크플로의 기동 검증이 보는 경로다.
 *
 * 프로세스가 떴는지가 아니라 요청을 처리하는 데 꼭 필요한 DB와 Redis에 실제로 붙는지 본다.
 * 하나라도 붙지 못하면 503을 돌려 ALB가 이 태스크로 트래픽을 보내지 않게 한다.
 * 운영 설정이 틀린 새 버전은 이 검사를 통과하지 못해 배포가 실패하고 기존 버전이 계속 서비스한다.
 *
 * 메일·디스크처럼 잠깐 느려도 서비스가 돌아가는 의존성은 넣지 않는다.
 * 넣으면 SMTP 지연 하나로 모든 태스크가 트래픽에서 빠진다.
 *
 * 확인할 항목은 운영 설정 파일이 아니라 코드에 둔다. application.yml 은 배포 때 시크릿으로 통째 덮어써진다.
 * 인증 없이 열린 경로라 응답에는 항목별 상태만 담고 오류 내용이나 접속 주소는 담지 않는다.
 */
@RestController
class UserHealthController(
    private val healthEndpoint: HealthEndpoint,
) {

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        val components = REQUIRED_COMPONENTS.associateWith { name ->
            // 항목이 등록되지 않았으면 확인할 수 없으므로 붙지 못한 것으로 본다.
            healthEndpoint.healthForPath(name)?.status ?: Status.DOWN
        }
        val status = if (components.values.all { it == Status.UP }) Status.UP else Status.DOWN
        val body = mapOf(
            "status" to status.code,
            "application" to APPLICATION_NAME,
            "components" to components.mapValues { (_, componentStatus) -> componentStatus.code },
        )
        val httpStatus = if (status == Status.UP) HttpStatus.OK else HttpStatus.SERVICE_UNAVAILABLE
        return ResponseEntity.status(httpStatus).body(body)
    }

    companion object {
        private const val APPLICATION_NAME = "ogonggo-api-user"

        /** 스프링 부트가 등록하는 헬스 항목 이름이다. DataSource 는 db, Redis 는 redis 로 등록된다. */
        private val REQUIRED_COMPONENTS = listOf("db", "redis")
    }
}
