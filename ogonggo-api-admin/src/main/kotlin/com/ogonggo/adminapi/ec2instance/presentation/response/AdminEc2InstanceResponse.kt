package com.ogonggo.adminapi.ec2instance.presentation.response

import com.ogonggo.adminapi.ec2instance.business.AdminEc2InstanceResult
import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2Instance
import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2InstanceState
import io.swagger.v3.oas.annotations.media.Schema

data class AdminEc2InstanceResponse(
    @Schema(description = "인스턴스. 켜고 끌 때 경로에 그대로 넣습니다. 이름은 /enums의 Ec2Instance에 있습니다.")
    val instance: Ec2Instance,
    @Schema(description = "AWS 인스턴스 상태. 켜고 끈 직후에는 PENDING·STOPPING이고, AWS가 돌려주지 않으면 UNKNOWN입니다.")
    val state: Ec2InstanceState,
) {
    companion object {
        internal fun from(result: AdminEc2InstanceResult): AdminEc2InstanceResponse =
            AdminEc2InstanceResponse(result.instance, result.state)
    }
}
