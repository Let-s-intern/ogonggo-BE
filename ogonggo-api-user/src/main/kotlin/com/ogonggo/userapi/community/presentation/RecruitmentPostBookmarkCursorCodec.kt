package com.ogonggo.userapi.community.presentation

import com.ogonggo.core.community.implement.RecruitmentPostBookmarkCursor
import com.ogonggo.userapi.error.InvalidRequestParameterException
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
import java.util.Base64

/** 내 북마크 목록의 마지막 북마크 위치를 URL-safe opaque cursor로 변환한다. */
object RecruitmentPostBookmarkCursorCodec {

    fun encode(cursor: RecruitmentPostBookmarkCursor): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString("${cursor.updatedAt}|${cursor.id}".toByteArray(UTF_8))

    fun decode(value: String?): RecruitmentPostBookmarkCursor? {
        if (value == null) return null

        return runCatching {
            val tokens = String(Base64.getUrlDecoder().decode(value), UTF_8).split('|')
            require(tokens.size == 2)
            RecruitmentPostBookmarkCursor(
                updatedAt = LocalDateTime.parse(tokens[0]),
                id = tokens[1].toLong().also { require(it > 0) },
            )
        }.getOrElse {
            throw InvalidRequestParameterException("cursor", "유효하지 않은 북마크 커서입니다.")
        }
    }
}
