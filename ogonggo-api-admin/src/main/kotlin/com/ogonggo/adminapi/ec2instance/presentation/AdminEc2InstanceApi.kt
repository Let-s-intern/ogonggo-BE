package com.ogonggo.adminapi.ec2instance.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2Instance
import com.ogonggo.adminapi.ec2instance.presentation.response.AdminEc2InstanceResponse
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "관리자 EC2 인스턴스")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminEc2InstanceApi {

    @Operation(
        operationId = "listEc2Instances",
        summary = "EC2 인스턴스 상태 목록",
        description = "관리자 콘솔에서 켜고 끌 수 있는 인스턴스(REDASH, CRAWLER)와 지금 AWS 상태를 돌려줍니다. 인스턴스 ID는 내보내지 않습니다. 서버에 ID가 설정되지 않은 인스턴스는 UNKNOWN입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "502",
                description = "EC2_UNAVAILABLE: AWS EC2 호출에 실패했습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun listEc2Instances(): ResponseEntity<SuccessResponse<List<AdminEc2InstanceResponse>>>

    @Operation(
        operationId = "startEc2Instance",
        summary = "EC2 인스턴스 켜기",
        description = """
            AWS에 켜기를 요청하고 바로 돌아옵니다. 켜지기까지 수십 초가 걸리므로 응답 상태는 보통 PENDING이고,
            목록을 다시 불러 RUNNING이 되었는지 확인합니다. 이미 켜져 있으면 RUNNING으로 그대로 성공합니다.
            매일 정해진 끄는 시각(크롤러 14:00, 리대시 22:00)이 되면 다시 꺼집니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "켜기 요청 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: REDASH·CRAWLER가 아닌 인스턴스입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "EC2_INSTANCE_STATE_CONFLICT: 끄는 중이라 지금은 켤 수 없습니다. 꺼진 뒤 다시 시도합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "503",
                description = "EC2_INSTANCE_NOT_CONFIGURED: 서버 설정에 그 인스턴스의 ID가 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "502",
                description = "EC2_UNAVAILABLE: AWS EC2 호출에 실패했습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun startEc2Instance(
        @Parameter(description = "인스턴스. 대소문자를 가리지 않습니다.", schema = Schema(implementation = Ec2Instance::class))
        instance: String,
    ): ResponseEntity<SuccessResponse<AdminEc2InstanceResponse>>

    @Operation(
        operationId = "stopEc2Instance",
        summary = "EC2 인스턴스 끄기",
        description = """
            AWS에 끄기를 요청하고 바로 돌아옵니다. 응답 상태는 보통 STOPPING이고, 목록을 다시 불러 STOPPED가 되었는지 확인합니다.
            이미 꺼져 있으면 STOPPED로 그대로 성공합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "끄기 요청 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: REDASH·CRAWLER가 아닌 인스턴스입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "EC2_INSTANCE_STATE_CONFLICT: 켜는 중이라 지금은 끌 수 없습니다. 켜진 뒤 다시 시도합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "503",
                description = "EC2_INSTANCE_NOT_CONFIGURED: 서버 설정에 그 인스턴스의 ID가 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "502",
                description = "EC2_UNAVAILABLE: AWS EC2 호출에 실패했습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun stopEc2Instance(
        @Parameter(description = "인스턴스. 대소문자를 가리지 않습니다.", schema = Schema(implementation = Ec2Instance::class))
        instance: String,
    ): ResponseEntity<SuccessResponse<AdminEc2InstanceResponse>>
}
