package com.ogonggo.core.featureflag.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class FeatureFlagErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    FEATURE_FLAG_NOT_FOUND(HttpStatus.NOT_FOUND, "기능 플래그를 찾을 수 없습니다."),
}
