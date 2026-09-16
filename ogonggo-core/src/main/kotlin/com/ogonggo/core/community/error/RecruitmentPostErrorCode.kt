package com.ogonggo.core.community.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class RecruitmentPostErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    RECRUITMENT_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "모집글을 찾을 수 없습니다."),
    RECRUITMENT_POST_NOT_READY(HttpStatus.BAD_REQUEST, "모집글 게시 조건을 충족하지 않았습니다."),
    RECRUITMENT_POST_BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 북마크한 모집글입니다."),
    RECRUITMENT_POST_CLOSED(HttpStatus.CONFLICT, "마감된 모집글에는 지원할 수 없습니다."),
}
