package com.ogonggo.core.letscareercontent.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class LetsCareerContentErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    LETS_CAREER_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "렛츠커리어 콘텐츠를 찾을 수 없습니다."),
    LETS_CAREER_CONTENT_TAGS_OUTDATED(HttpStatus.CONFLICT, "태그를 붙인 뒤로 렛츠커리어 콘텐츠 내용이 바뀌었습니다."),
}
