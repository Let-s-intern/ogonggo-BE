package com.ogonggo.userapi.enumeration.presentation.response

import com.ogonggo.core.enumeration.EnumOption
import io.swagger.v3.oas.annotations.media.Schema

data class UserEnumOptionResponse(
    @Schema(description = "요청과 응답에 쓰는 값입니다.", example = "FULL_TIME")
    val name: String,
    @Schema(description = "화면에 보일 한국어 라벨입니다.", example = "정규직")
    val desc: String,
    @Schema(description = "상위 값의 name입니다. SubRegion처럼 다른 enum 값에 속할 때만 있고, 그 밖에는 null입니다.", example = "SEOUL")
    val parent: String?,
) {
    companion object {
        internal fun from(option: EnumOption): UserEnumOptionResponse =
            UserEnumOptionResponse(name = option.name, desc = option.desc, parent = option.parent)
    }
}
