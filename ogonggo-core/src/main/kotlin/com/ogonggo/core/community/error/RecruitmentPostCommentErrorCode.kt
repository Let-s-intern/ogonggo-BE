package com.ogonggo.core.community.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class RecruitmentPostCommentErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    RECRUITMENT_POST_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    RECRUITMENT_POST_COMMENT_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "댓글을 삭제할 권한이 없습니다."),
    RECRUITMENT_POST_COMMENT_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 댓글을 찾을 수 없습니다."),
    RECRUITMENT_POST_COMMENT_PARENT_TARGET_MISMATCH(HttpStatus.BAD_REQUEST, "부모 댓글이 같은 모집글에 속하지 않습니다."),
    RECRUITMENT_POST_COMMENT_NESTING_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "대댓글에는 다시 답글을 작성할 수 없습니다."),
}
