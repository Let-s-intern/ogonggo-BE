package com.ogonggo.userapi.community.presentation.request

import com.fasterxml.jackson.databind.JsonNode
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.implement.PostAppendCommand
import com.ogonggo.userapi.error.InvalidRequestFieldException
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateRecruitmentPostRequest(
    @field:NotBlank @field:Size(max = 255) val title: String,
    val recruitmentType: RecruitmentType,
    @field:Positive val capacity: Int,
    val progressMethod: ProgressMethod,
    @field:Positive val activityDurationMonths: Int,
    @field:Size(max = 20) val technologyStacks: List<String> = emptyList(),
    @field:NotBlank @field:Size(max = 500) val summary: String,
    @field:NotNull val content: JsonNode,
    val eligibilityAndSelectionProcess: String?,
    val recruitmentStartDate: LocalDate,
    val recruitmentEndDate: LocalDate,
    @field:Size(min = 1, max = 20) val positions: List<RecruitmentPosition>,
    val contactMethod: ContactMethod,
    @field:NotBlank @field:Size(max = 2048) val contactValue: String,
    @field:AssertTrue(message = "모집글 등록에 필요한 정보 제공 및 운영 정책에 동의해야 합니다.")
    val agreedToPolicy: Boolean,
) {
    fun toCommand(authorUserId: Long): PostAppendCommand {
        validateRelations()
        return PostAppendCommand(
            authorUserId = authorUserId,
            title = title,
            recruitmentType = recruitmentType,
            capacity = capacity,
            progressMethod = progressMethod,
            activityDurationMonths = activityDurationMonths,
            technologyStacks = technologyStacks,
            summary = summary,
            content = content.toString(),
            eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
            recruitmentStartDate = recruitmentStartDate,
            recruitmentEndDate = recruitmentEndDate,
            positions = positions,
            contactMethod = contactMethod,
            contactValue = contactValue,
        )
    }

    private fun validateRelations() {
        if (recruitmentStartDate.isAfter(recruitmentEndDate)) {
            invalid("recruitmentStartDate", "모집 마감일보다 늦을 수 없습니다.")
        }
        if (technologyStacks.any(String::isBlank)) {
            invalid("technologyStacks", "기술 스택은 공백일 수 없습니다.")
        }
        if (technologyStacks.distinct().size != technologyStacks.size) {
            invalid("technologyStacks", "중복된 기술 스택은 등록할 수 없습니다.")
        }
        if (positions.distinct().size != positions.size) {
            invalid("positions", "중복된 모집 포지션은 등록할 수 없습니다.")
        }
        if (eligibilityAndSelectionProcess != null && eligibilityAndSelectionProcess.isBlank()) {
            invalid("eligibilityAndSelectionProcess", "공백일 수 없습니다.")
        }
        when (contactMethod) {
            ContactMethod.EMAIL -> if (!EMAIL_PATTERN.matches(contactValue)) {
                invalid("contactValue", "이메일 형식으로 입력해 주세요.")
            }
            ContactMethod.OPEN_KAKAO -> if (!isHttpUrl(contactValue)) {
                invalid("contactValue", "카카오톡 오픈채팅 링크를 입력해 주세요.")
            }
        }
    }

    private fun isHttpUrl(value: String): Boolean =
        value.startsWith("https://") || value.startsWith("http://")

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}

private fun invalid(field: String, reason: String): Nothing = throw InvalidRequestFieldException(field, reason)
