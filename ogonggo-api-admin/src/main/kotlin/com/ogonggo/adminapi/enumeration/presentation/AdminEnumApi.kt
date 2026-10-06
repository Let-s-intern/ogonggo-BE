package com.ogonggo.adminapi.enumeration.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.enumeration.presentation.response.AdminEnumOptionResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "관리자 선택지")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminEnumApi {

    @Operation(
        operationId = "listAdminEnums",
        summary = "enum 선택지 조회",
        description = """
            관리자 API의 요청·응답에 나오는 enum을 enum 이름별로 묶어 선언 순서대로 반환합니다.
            예: `data.JobEmploymentType[0]`은 `{"name": "FULL_TIME", "desc": "정규직", "parent": null}`입니다.
            요청에는 `name`을 보내고 `desc`는 화면 라벨로만 씁니다.
            `parent`는 JobRole처럼 다른 enum 값에 속하는 값의 상위 값 name이며, 그 밖에는 null입니다.
            값은 enum의 전체 값이며, 목록 필터처럼 일부 값만 받는 곳의 범위는 해당 API 명세를 따릅니다.
        """,
    )
    fun getEnums(): ResponseEntity<SuccessResponse<Map<String, List<AdminEnumOptionResponse>>>>
}
