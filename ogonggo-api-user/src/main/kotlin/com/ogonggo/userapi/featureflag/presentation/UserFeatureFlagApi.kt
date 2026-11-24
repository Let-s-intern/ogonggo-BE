package com.ogonggo.userapi.featureflag.presentation

import com.ogonggo.userapi.featureflag.presentation.response.UserFeatureFlagListResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "기능 플래그")
interface UserFeatureFlagApi {

    @Operation(
        operationId = "listPublicFeatureFlags",
        summary = "기능 플래그 목록 조회",
        description = """
            화면이나 기능을 켜고 끄는 플래그의 key와 켜짐 여부를 모두 반환합니다. 로그인 없이 조회할 수 있고 설명과 변경자는 내리지 않습니다.

            서버가 값을 메모리에 30초 두므로, 관리자가 바꾼 값이 모든 서버에 퍼지는 데 최대 30초 걸립니다.
            목록에 없는 key는 클라이언트가 해당 플래그의 기본 동작으로 처리합니다.
        """,
    )
    fun getFeatureFlags(): ResponseEntity<SuccessResponse<UserFeatureFlagListResponse>>
}
