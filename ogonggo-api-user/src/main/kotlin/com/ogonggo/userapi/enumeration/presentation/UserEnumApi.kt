package com.ogonggo.userapi.enumeration.presentation

import com.ogonggo.userapi.enumeration.presentation.response.UserEnumOptionResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "선택지")
interface UserEnumApi {

    @Operation(
        operationId = "listPublicEnums",
        summary = "enum 선택지 조회",
        description = """
            로그인 없이 조회할 수 있습니다. 사용자 API의 요청·응답에 나오는 enum을 enum 이름별로 묶어
            선언 순서대로 반환합니다. 예: `data.EmploymentType[0]`은 `{"name": "FULL_TIME", "desc": "정규직"}`입니다.
            요청에는 `name`을 보내고 `desc`는 화면 라벨로만 씁니다.
            값은 enum의 전체 값이며, 목록 필터처럼 일부 값만 받는 곳의 범위는 해당 API 명세를 따릅니다.
        """,
    )
    fun getEnums(): ResponseEntity<SuccessResponse<Map<String, List<UserEnumOptionResponse>>>>
}
