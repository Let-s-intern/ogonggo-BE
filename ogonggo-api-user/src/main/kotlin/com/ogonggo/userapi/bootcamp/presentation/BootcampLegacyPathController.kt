package com.ogonggo.userapi.bootcamp.presentation

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 부트캠프 외부 링크 이동 기록을 채용공고와 같은 `source-url-clicks`로 맞추기 전의 경로다.
 * 서버를 먼저 배포하므로 프런트가 새 경로로 옮길 때까지 같은 동작으로 받고, 명세에는 새 경로만 보인다.
 * 프런트 배포 후 서버 2차 배포에서 이 파일을 지운다.
 */
@Hidden
@RestController
class BootcampLegacyPathController(
    private val userBootcampController: UserBootcampController,
) {

    @PostMapping("/api/v1/bootcamps/{bootcampId}/application-url-clicks")
    fun recordApplicationUrlClick(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") bootcampId: Long,
    ) = userBootcampController.recordSourceUrlClick(userId, bootcampId)
}
