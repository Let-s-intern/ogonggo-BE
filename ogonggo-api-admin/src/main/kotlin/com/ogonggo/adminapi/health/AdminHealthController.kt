package com.ogonggo.adminapi.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminHealthController {

    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf(
        "status" to "UP",
        "application" to "ogonggo-api-admin",
    )
}
