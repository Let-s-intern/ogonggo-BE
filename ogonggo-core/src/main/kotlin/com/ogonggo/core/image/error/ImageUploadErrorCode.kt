package com.ogonggo.core.image.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class ImageUploadErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    IMAGE_FILE_REQUIRED(HttpStatus.BAD_REQUEST, "이미지 파일은 필수입니다."),
    IMAGE_FILE_TYPE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다."),
    IMAGE_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "이미지 파일은 10 MiB 이하여야 합니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다."),
}
