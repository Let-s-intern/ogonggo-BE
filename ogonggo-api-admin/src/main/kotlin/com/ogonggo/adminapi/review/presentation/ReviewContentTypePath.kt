package com.ogonggo.adminapi.review.presentation

import com.ogonggo.adminapi.error.InvalidRequestParameterException
import com.ogonggo.core.review.domain.ReviewContentType

/** 경로의 종류는 `job`·`bootcamp`처럼 소문자로 쓴다. 대소문자는 가리지 않는다. */
internal fun parseReviewContentType(value: String): ReviewContentType =
    ReviewContentType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        ?: throw InvalidRequestParameterException("type", "job 또는 bootcamp만 쓸 수 있습니다.")
