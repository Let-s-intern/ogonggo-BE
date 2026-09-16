package com.ogonggo.userapi.community.presentation

import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.implement.RecruitmentPostCursor
import com.ogonggo.userapi.error.InvalidRequestParameterException
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDate
import java.util.Base64

/** 모집글 목록의 정렬 위치와 조회 조건을 URL-safe opaque cursor로 변환한다. */
object RecruitmentPostCursorCodec {

    fun encode(cursor: RecruitmentPostCursor): String {
        val queryKey = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(cursor.queryKey.toByteArray(UTF_8))
        val position = when (cursor.sortType) {
            RecruitmentPostSortType.LATEST -> listOf("L", queryKey, cursor.id)
            RecruitmentPostSortType.DEADLINE -> listOf("D", queryKey, cursor.deadline, cursor.id)
            RecruitmentPostSortType.VIEW_COUNT -> listOf("V", queryKey, cursor.metricCount, cursor.id)
            RecruitmentPostSortType.COMMENT_COUNT -> listOf("C", queryKey, cursor.metricCount, cursor.id)
        }.joinToString("|")
        return Base64.getUrlEncoder().withoutPadding().encodeToString(position.toByteArray(UTF_8))
    }

    fun decode(value: String?): RecruitmentPostCursor? {
        if (value == null) return null

        return runCatching {
            val tokens = String(Base64.getUrlDecoder().decode(value), UTF_8).split('|')
            require(tokens.size >= 3)
            val sortType = when (tokens[0]) {
                "L" -> RecruitmentPostSortType.LATEST
                "D" -> RecruitmentPostSortType.DEADLINE
                "V" -> RecruitmentPostSortType.VIEW_COUNT
                "C" -> RecruitmentPostSortType.COMMENT_COUNT
                else -> error("unknown sort")
            }
            val queryKey = String(Base64.getUrlDecoder().decode(tokens[1]), UTF_8)
            when (sortType) {
                RecruitmentPostSortType.LATEST -> RecruitmentPostCursor(
                    queryKey = queryKey,
                    sortType = sortType,
                    id = tokens[2].toLong(),
                )
                RecruitmentPostSortType.DEADLINE -> {
                    require(tokens.size == 4)
                    RecruitmentPostCursor(
                        queryKey = queryKey,
                        sortType = sortType,
                        deadline = LocalDate.parse(tokens[2]),
                        id = tokens[3].toLong(),
                    )
                }
                RecruitmentPostSortType.VIEW_COUNT,
                RecruitmentPostSortType.COMMENT_COUNT,
                -> {
                    require(tokens.size == 4)
                    RecruitmentPostCursor(
                        queryKey = queryKey,
                        sortType = sortType,
                        metricCount = tokens[2].toLong(),
                        id = tokens[3].toLong(),
                    )
                }
            }
        }.getOrElse {
            throw InvalidRequestParameterException("cursor", "유효하지 않은 모집글 커서입니다.")
        }
    }
}
