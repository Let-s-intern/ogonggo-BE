package com.ogonggo.adminapi.ec2instance.presentation.response

import com.ogonggo.adminapi.ec2instance.business.AdminEc2InstanceResult
import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2InstanceState
import io.swagger.v3.oas.annotations.media.Schema

data class AdminEc2InstanceResponse(
    @Schema(description = "켜고 끌 때 경로에 넣는 이름", example = "redash")
    val name: String,
    @Schema(description = "AWS EC2 인스턴스 ID", example = "i-0b277b7cfbfe0d9c2")
    val instanceId: String,
    @Schema(description = "AWS 인스턴스 상태. 켜고 끈 직후에는 PENDING·STOPPING이고, AWS가 돌려주지 않으면 UNKNOWN입니다.")
    val state: Ec2InstanceState,
) {
    companion object {
        internal fun from(result: AdminEc2InstanceResult): AdminEc2InstanceResponse =
            AdminEc2InstanceResponse(result.name, result.instanceId, result.state)
    }
}
