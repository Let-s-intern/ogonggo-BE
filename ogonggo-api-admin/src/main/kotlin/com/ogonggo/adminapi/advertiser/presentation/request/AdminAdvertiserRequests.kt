package com.ogonggo.adminapi.advertiser.presentation.request

import com.ogonggo.adminapi.advertiser.business.AdminAdvertiserCreateCommand
import com.ogonggo.adminapi.advertiser.business.AdminAdvertiserUpdateCommand
import io.swagger.v3.oas.annotations.media.Schema

/** 칸 검증은 화면 문구(`필드: 설명`)로 알리려고 서비스가 한다. */
data class CreateAdminAdvertiserRequest(
    @Schema(description = "기업 이름. 앞뒤 공백을 빼고 1~50자입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String? = null,
    val contactName: String? = null,
    @Schema(description = "담당자 메일. 메일 형식만 봅니다.")
    val contactEmail: String? = null,
    val contactPhone: String? = null,
) {
    fun toCommand(): AdminAdvertiserCreateCommand = AdminAdvertiserCreateCommand(
        name = name,
        contactName = contactName,
        contactEmail = contactEmail,
        contactPhone = contactPhone,
    )
}

/** 보낸 칸만 바꿉니다. 빈 문자열은 그 칸을 지웁니다. 이름은 지울 수 없습니다. */
data class UpdateAdminAdvertiserRequest(
    val name: String? = null,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
) {
    fun toCommand(): AdminAdvertiserUpdateCommand = AdminAdvertiserUpdateCommand(
        name = name,
        contactName = contactName,
        contactEmail = contactEmail,
        contactPhone = contactPhone,
    )
}
