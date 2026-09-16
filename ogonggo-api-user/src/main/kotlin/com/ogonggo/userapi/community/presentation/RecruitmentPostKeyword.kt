package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.error.InvalidRequestParameterException

/**
 * 모집글 목록 검색어를 HTTP 입력에서 내부 조회 조건으로 변환한다.
 * Bean Validation은 trim 전의 원본 파라미터에 적용되므로 trim 후 길이도 여기서 보장한다.
 */
internal fun normalizeRecruitmentPostKeyword(keyword: String?): String? {
    val normalized = keyword?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if (normalized.length !in 2..100) {
        throw InvalidRequestParameterException("keyword", "검색어는 2자 이상 100자 이하여야 합니다.")
    }
    return normalized
}
