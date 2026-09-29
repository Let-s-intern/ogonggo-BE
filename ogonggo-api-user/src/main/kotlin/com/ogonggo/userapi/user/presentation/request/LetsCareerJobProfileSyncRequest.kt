package com.ogonggo.userapi.user.presentation.request

import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.implement.dto.UserProfileJobInfoDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 렛츠커리어가 보내는 학력·희망 조건 전체다. null이면 비운다.
 * 렛츠커리어가 자유 문자열로 다루는 값이라 길이를 검사하지 않고 그대로 보관한다.
 */
data class LetsCareerJobProfileSyncRequest(
    val university: String? = null,
    val major: String? = null,
    @Schema(description = "렛츠커리어가 오공고에 없는 학년을 보내면 400이다.")
    val grade: UserGrade? = null,
    val wishField: String? = null,
    val wishJob: String? = null,
    val wishIndustry: String? = null,
    val wishEmploymentType: String? = null,
    val wishCompany: String? = null,
    @Schema(description = "렛츠커리어에서 고친 일시. 한 번도 고친 적 없는 과거 계정은 null이다.")
    val updatedAt: LocalDateTime? = null,
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
