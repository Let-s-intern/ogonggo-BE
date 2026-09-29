package com.ogonggo.userapi.user.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

data class LetsCareerJobProfileSyncResponse(
    @Schema(description = "반영했으면 true. 오공고에서 더 나중에 고쳤거나, 이미 받은 수정이거나, 오공고 계정이 없으면 false다.")
    val applied: Boolean,
)
