package com.ogonggo.userapi.user.presentation.request

import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.implement.dto.UserProfileJobInfoDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

private const val MAX_SCHOOL_LENGTH = 30
private const val MAX_WISH_LENGTH = 1000

/**
 * 사용자가 오공고에서 직접 고치는 값만 담는다.
 * 이름·닉네임·프로필 이미지는 렛츠커리어가 소유하므로 여기서 바꾸지 않는다.
 */
data class ReplaceMyProfileRequest(
    @field:Size(max = MAX_SCHOOL_LENGTH)
    @Schema(description = "대학교", example = "오공고대학교")
    val university: String? = null,

    @field:Size(max = MAX_SCHOOL_LENGTH)
    @Schema(description = "전공", example = "컴퓨터공학과")
    val major: String? = null,

    @Schema(description = "학년")
    val grade: UserGrade? = null,

    @field:Size(max = MAX_WISH_LENGTH)
    @Schema(description = "희망 직군", example = "개발")
    val wishField: String? = null,

    @field:Size(max = MAX_WISH_LENGTH)
    @Schema(description = "희망 직무", example = "백엔드 개발")
    val wishJob: String? = null,

    @field:Size(max = MAX_WISH_LENGTH)
    @Schema(description = "희망 산업", example = "IT, 금융")
    val wishIndustry: String? = null,

    @field:Size(max = MAX_WISH_LENGTH)
    @Schema(description = "희망 구직 조건", example = "정규직")
    val wishEmploymentType: String? = null,

    @field:Size(max = MAX_WISH_LENGTH)
    @Schema(description = "희망 기업", example = "오공고, 렛츠커리어")
    val wishCompany: String? = null,
) {
    fun toCommand(): UserProfileJobInfoDto = UserProfileJobInfoDto(
        university = university,
        major = major,
        grade = grade,
        wishField = wishField,
        wishJob = wishJob,
        wishIndustry = wishIndustry,
        wishEmploymentType = wishEmploymentType,
        wishCompany = wishCompany,
    )
}
