package com.ogonggo.adminapi.work24.business

import com.fasterxml.jackson.databind.JsonNode
import com.ogonggo.core.work24.domain.Work24Api
import com.ogonggo.core.work24.implement.Work24Client
import org.springframework.stereotype.Service

/**
 * 운영자가 고용24 응답을 바로 확인하는 유스케이스다. 저장하지 않는다.
 * 매일 받아 저장하는 흐름은 [AdminWork24CollectionService]가 맡는다.
 */
@Service
class AdminWork24Service(
    private val work24Client: Work24Client,
) {

    fun fetch(api: Work24Api, parameters: Map<String, String>): JsonNode =
        work24Client.fetch(api, parameters)
}
