package com.ogonggo.userapi.community.presentation

import com.ogonggo.core.community.implement.RecruitmentPostCommentCursor
import com.ogonggo.userapi.error.InvalidRequestParameterException
import java.time.LocalDateTime

object RecruitmentPostCommentCursorCodec {

    fun encode(cursor: RecruitmentPostCommentCursor): String {
        return "${cursor.createdAt}_${cursor.id}"
    }

    fun decode(value: String?): RecruitmentPostCommentCursor? {
        if (value == null) return null

        return runCatching {
            val idSeparator = value.lastIndexOf('_')
            require(idSeparator > 0)
            RecruitmentPostCommentCursor(
                createdAt = LocalDateTime.parse(value.substring(0, idSeparator)),
                id = value.substring(idSeparator + 1).toLong(),
            )
        }.getOrElse {
            throw InvalidRequestParameterException("cursor", "유효하지 않은 댓글 커서입니다.")
        }
    }

}
