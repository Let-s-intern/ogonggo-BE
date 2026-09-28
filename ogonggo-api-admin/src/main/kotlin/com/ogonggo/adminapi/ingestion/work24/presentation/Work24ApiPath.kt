package com.ogonggo.adminapi.ingestion.work24.presentation

import com.ogonggo.adminapi.error.InvalidRequestParameterException
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Api

/** 경로에는 `tomorrow-learning-card-courses`처럼 enum 이름을 kebab-case 소문자로 쓴다. */
internal val Work24Api.pathName: String
    get() = name.lowercase().replace('_', '-')

internal fun parseWork24Api(value: String): Work24Api =
    Work24Api.entries.firstOrNull { it.pathName == value }
        ?: throw InvalidRequestParameterException(WORK24_API_PATH_VARIABLE, "지원하지 않는 고용24 API입니다.")

internal const val WORK24_API_PATH_VARIABLE = "apiName"
