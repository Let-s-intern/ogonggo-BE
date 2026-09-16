package com.ogonggo.userapi.community.presentation.request

import com.fasterxml.jackson.databind.JsonNode
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPostSaveMode
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.implement.RecruitmentPostAppendCommand
import com.ogonggo.core.community.implement.RecruitmentPostDraftAppendCommand
import com.ogonggo.core.community.implement.RecruitmentPostSaveCommand
import com.ogonggo.core.community.implement.RecruitmentPostUpdateCommand
import com.ogonggo.userapi.error.InvalidRequestFieldException
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.net.URI
import java.time.LocalDate

data class CreateRecruitmentPostRequest(
    @field:NotBlank @field:Size(max = 255) val title: String,
    val recruitmentType: RecruitmentType? = null,
    @field:Positive val capacity: Int? = null,
    val progressMethod: ProgressMethod? = null,
    @field:Positive val activityDurationMonths: Int? = null,
    @field:Size(max = 20) val technologyStacks: List<String>? = null,
    @field:Size(max = 500) val summary: String? = null,
    val content: JsonNode? = null,
    val eligibilityAndSelectionProcess: String? = null,
    val recruitmentStartDate: LocalDate? = null,
    val recruitmentEndDate: LocalDate? = null,
    @field:Size(max = 20) val positions: List<RecruitmentPosition>? = null,
    val contactMethod: ContactMethod? = null,
    @field:Size(max = 2048) val contactValue: String? = null,
    val agreedToPolicy: Boolean = false,
    val saveMode: RecruitmentPostSaveMode = RecruitmentPostSaveMode.PUBLISH,
) {
    fun toSaveCommand(authorUserId: Long): RecruitmentPostSaveCommand = when (saveMode) {
        RecruitmentPostSaveMode.DRAFT -> RecruitmentPostSaveCommand.Draft(toDraftCommand(authorUserId))
        RecruitmentPostSaveMode.PUBLISH -> RecruitmentPostSaveCommand.Published(toPublishedCommand(authorUserId))
    }

    /** 기존 호출부와의 호환을 위해 유지하며, 공개 저장용 명령을 반환합니다. */
    fun toCommand(authorUserId: Long): RecruitmentPostAppendCommand = toPublishedCommand(authorUserId)

    fun toDraftCommand(authorUserId: Long): RecruitmentPostDraftAppendCommand {
        validateRelations()
        return RecruitmentPostDraftAppendCommand(
            authorUserId = authorUserId,
            title = title,
            recruitmentType = recruitmentType,
            capacity = capacity,
            progressMethod = progressMethod,
            activityDurationMonths = activityDurationMonths,
            technologyStacks = technologyStacks.orEmpty(),
            summary = summary,
            content = content?.toString(),
            eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
            recruitmentStartDate = recruitmentStartDate,
            recruitmentEndDate = recruitmentEndDate,
            positions = positions.orEmpty(),
            contactMethod = contactMethod,
            contactValue = contactValue,
        )
    }

    private fun toPublishedCommand(authorUserId: Long): RecruitmentPostAppendCommand {
        if (!agreedToPolicy) {
            invalid("agreedToPolicy", "모집글 등록에 필요한 정보 제공 및 운영 정책에 동의해야 합니다.")
        }
        val recruitmentType = required(recruitmentType, "recruitmentType")
        val capacity = required(capacity, "capacity")
        val progressMethod = required(progressMethod, "progressMethod")
        val activityDurationMonths = required(activityDurationMonths, "activityDurationMonths")
        val summary = requiredText(summary, "summary")
        val content = required(content, "content").toString()
        val recruitmentStartDate = required(recruitmentStartDate, "recruitmentStartDate")
        val recruitmentEndDate = required(recruitmentEndDate, "recruitmentEndDate")
        val positions = positions.orEmpty().ifEmpty { invalid("positions", "모집 포지션은 1개 이상이어야 합니다.") }
        val contactMethod = required(contactMethod, "contactMethod")
        val contactValue = requiredText(contactValue, "contactValue")

        validateRelations(recruitmentStartDate, recruitmentEndDate, positions, contactMethod, contactValue)
        return RecruitmentPostAppendCommand(
            authorUserId = authorUserId,
            title = title,
            recruitmentType = recruitmentType,
            capacity = capacity,
            progressMethod = progressMethod,
            activityDurationMonths = activityDurationMonths,
            technologyStacks = technologyStacks.orEmpty(),
            summary = summary,
            content = content,
            eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
            recruitmentStartDate = recruitmentStartDate,
            recruitmentEndDate = recruitmentEndDate,
            positions = positions,
            contactMethod = contactMethod,
            contactValue = contactValue,
        )
    }

    private fun validateRelations() {
        if (recruitmentStartDate != null && recruitmentEndDate != null && recruitmentStartDate.isAfter(recruitmentEndDate)) {
            invalid("recruitmentStartDate", "모집 마감일보다 늦을 수 없습니다.")
        }
        if (technologyStacks.orEmpty().any(String::isBlank)) {
            invalid("technologyStacks", "기술 스택은 공백일 수 없습니다.")
        }
        if (technologyStacks.orEmpty().distinct().size != technologyStacks.orEmpty().size) {
            invalid("technologyStacks", "중복된 기술 스택은 등록할 수 없습니다.")
        }
        if (positions.orEmpty().distinct().size != positions.orEmpty().size) {
            invalid("positions", "중복된 모집 포지션은 등록할 수 없습니다.")
        }
        if (eligibilityAndSelectionProcess != null && eligibilityAndSelectionProcess.isBlank()) {
            invalid("eligibilityAndSelectionProcess", "공백일 수 없습니다.")
        }
        if (contactMethod != null && contactValue != null) {
            when (contactMethod) {
                ContactMethod.EMAIL -> if (!EMAIL_PATTERN.matches(contactValue)) {
                    invalid("contactValue", "이메일 형식으로 입력해 주세요.")
                }
                ContactMethod.OPEN_KAKAO -> if (!isHttpUrl(contactValue)) {
                    invalid("contactValue", "카카오톡 오픈채팅 링크를 입력해 주세요.")
                }
            }
        }
    }

    private fun validateRelations(
        recruitmentStartDate: LocalDate,
        recruitmentEndDate: LocalDate,
        positions: List<RecruitmentPosition>,
        contactMethod: ContactMethod,
        contactValue: String,
    ) {
        if (recruitmentStartDate.isAfter(recruitmentEndDate)) {
            invalid("recruitmentStartDate", "모집 마감일보다 늦을 수 없습니다.")
        }
        if (positions.distinct().size != positions.size) {
            invalid("positions", "중복된 모집 포지션은 등록할 수 없습니다.")
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

    private fun <T> required(value: T?, field: String): T = value ?: invalid(field, "필수 입력값입니다.")

    private fun requiredText(value: String?, field: String): String =
        value?.takeUnless(String::isBlank) ?: invalid(field, "필수 입력값입니다.")

    private fun isHttpUrl(value: String): Boolean = runCatching {
        URI(value).let { it.scheme in setOf("http", "https") && !it.host.isNullOrBlank() }
    }.getOrDefault(false)

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}

data class UpdateRecruitmentPostRequest(
    @field:NotBlank @field:Size(max = 255) val title: String,
    val recruitmentType: RecruitmentType? = null,
    @field:Positive val capacity: Int? = null,
    val progressMethod: ProgressMethod? = null,
    @field:Positive val activityDurationMonths: Int? = null,
    @field:Size(max = 20) val technologyStacks: List<String>? = null,
    @field:Size(max = 500) val summary: String? = null,
    val content: JsonNode? = null,
    val eligibilityAndSelectionProcess: String? = null,
    val recruitmentStartDate: LocalDate? = null,
    val recruitmentEndDate: LocalDate? = null,
    @field:Size(max = 20) val positions: List<RecruitmentPosition>? = null,
    val contactMethod: ContactMethod? = null,
    @field:Size(max = 2048) val contactValue: String? = null,
    val agreedToPolicy: Boolean = false,
    val saveMode: RecruitmentPostSaveMode? = null,
) {
    fun toCommand(): RecruitmentPostUpdateCommand {
        validateRelations()
        if (saveMode == RecruitmentPostSaveMode.PUBLISH) {
            validatePublishFields()
        }
        return RecruitmentPostUpdateCommand(
            title = title,
            recruitmentType = recruitmentType,
            capacity = capacity,
            progressMethod = progressMethod,
            activityDurationMonths = activityDurationMonths,
            technologyStacks = technologyStacks.orEmpty(),
            summary = summary,
            content = content?.toString(),
            eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
            recruitmentStartDate = recruitmentStartDate,
            recruitmentEndDate = recruitmentEndDate,
            positions = positions.orEmpty(),
            contactMethod = contactMethod,
            contactValue = contactValue,
        )
    }

    private fun validateRelations() {
        if (recruitmentStartDate != null && recruitmentEndDate != null && recruitmentStartDate.isAfter(recruitmentEndDate)) {
            invalid("recruitmentStartDate", "모집 마감일보다 늦을 수 없습니다.")
        }
        if (technologyStacks.orEmpty().any(String::isBlank)) {
            invalid("technologyStacks", "기술 스택은 공백일 수 없습니다.")
        }
        if (technologyStacks.orEmpty().distinct().size != technologyStacks.orEmpty().size) {
            invalid("technologyStacks", "중복된 기술 스택은 등록할 수 없습니다.")
        }
        if (positions.orEmpty().distinct().size != positions.orEmpty().size) {
            invalid("positions", "중복된 모집 포지션은 등록할 수 없습니다.")
        }
        if (eligibilityAndSelectionProcess != null && eligibilityAndSelectionProcess.isBlank()) {
            invalid("eligibilityAndSelectionProcess", "공백일 수 없습니다.")
        }
        if (contactValue != null && contactValue.isBlank()) {
            invalid("contactValue", "연락 방법 값은 비어 있을 수 없습니다.")
        }
        if (contactMethod != null && contactValue != null) when (contactMethod) {
            ContactMethod.EMAIL -> if (!EMAIL_PATTERN.matches(contactValue)) {
                invalid("contactValue", "이메일 형식으로 입력해 주세요.")
            }
            ContactMethod.OPEN_KAKAO -> if (!isHttpUrl(contactValue)) {
                invalid("contactValue", "카카오톡 오픈채팅 링크를 입력해 주세요.")
            }
        }
    }

    private fun validatePublishFields() {
        if (!agreedToPolicy) {
            invalid("agreedToPolicy", "모집글 등록에 필요한 정보 제공 및 운영 정책에 동의해야 합니다.")
        }
        if (recruitmentType == null) invalid("recruitmentType", "필수 입력값입니다.")
        if (capacity == null) invalid("capacity", "필수 입력값입니다.")
        if (progressMethod == null) invalid("progressMethod", "필수 입력값입니다.")
        if (activityDurationMonths == null) invalid("activityDurationMonths", "필수 입력값입니다.")
        if (summary.isNullOrBlank()) invalid("summary", "필수 입력값입니다.")
        if (content == null) invalid("content", "필수 입력값입니다.")
        if (recruitmentStartDate == null) invalid("recruitmentStartDate", "필수 입력값입니다.")
        if (recruitmentEndDate == null) invalid("recruitmentEndDate", "필수 입력값입니다.")
        if (positions.isNullOrEmpty()) invalid("positions", "모집 포지션은 1개 이상이어야 합니다.")
        if (contactMethod == null) invalid("contactMethod", "필수 입력값입니다.")
        if (contactValue.isNullOrBlank()) invalid("contactValue", "필수 입력값입니다.")
    }

    private fun isHttpUrl(value: String): Boolean =
        value.startsWith("https://") || value.startsWith("http://")

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}

data class CreateRecruitmentPostDraftRequest(
    @field:NotBlank @field:Size(max = 255) val title: String,
    val recruitmentType: RecruitmentType? = null,
    @field:Positive val capacity: Int? = null,
    val progressMethod: ProgressMethod? = null,
    @field:Positive val activityDurationMonths: Int? = null,
    @field:Size(max = 20) val technologyStacks: List<String>? = null,
    @field:Size(max = 500) val summary: String? = null,
    val content: JsonNode? = null,
    val eligibilityAndSelectionProcess: String? = null,
    val recruitmentStartDate: LocalDate? = null,
    val recruitmentEndDate: LocalDate? = null,
    @field:Size(max = 20) val positions: List<RecruitmentPosition>? = null,
    val contactMethod: ContactMethod? = null,
    @field:Size(max = 2048) val contactValue: String? = null,
) {
    fun toCommand(authorUserId: Long): RecruitmentPostDraftAppendCommand {
        validateRelations()
        return RecruitmentPostDraftAppendCommand(
            authorUserId = authorUserId,
            title = title,
            recruitmentType = recruitmentType,
            capacity = capacity,
            progressMethod = progressMethod,
            activityDurationMonths = activityDurationMonths,
            technologyStacks = technologyStacks.orEmpty(),
            summary = summary,
            content = content?.toString(),
            eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
            recruitmentStartDate = recruitmentStartDate,
            recruitmentEndDate = recruitmentEndDate,
            positions = positions.orEmpty(),
            contactMethod = contactMethod,
            contactValue = contactValue,
        )
    }

    private fun validateRelations() {
        if (recruitmentStartDate != null && recruitmentEndDate != null && recruitmentStartDate.isAfter(recruitmentEndDate)) {
            invalid("recruitmentStartDate", "모집 마감일보다 늦을 수 없습니다.")
        }
        if (technologyStacks.orEmpty().any(String::isBlank)) {
            invalid("technologyStacks", "기술 스택은 공백일 수 없습니다.")
        }
        if (technologyStacks.orEmpty().distinct().size != technologyStacks.orEmpty().size) {
            invalid("technologyStacks", "중복된 기술 스택은 등록할 수 없습니다.")
        }
        if (positions.orEmpty().distinct().size != positions.orEmpty().size) {
            invalid("positions", "중복된 모집 포지션은 등록할 수 없습니다.")
        }
        if (eligibilityAndSelectionProcess != null && eligibilityAndSelectionProcess.isBlank()) {
            invalid("eligibilityAndSelectionProcess", "공백일 수 없습니다.")
        }
        if (contactValue != null && contactValue.isBlank()) {
            invalid("contactValue", "연락 방법 값은 비어 있을 수 없습니다.")
        }
        if (contactMethod != null && contactValue != null) when (contactMethod) {
            ContactMethod.EMAIL -> if (!EMAIL_PATTERN.matches(contactValue)) {
                invalid("contactValue", "이메일 형식으로 입력해 주세요.")
            }
            ContactMethod.OPEN_KAKAO -> if (!isHttpUrl(contactValue)) {
                invalid("contactValue", "카카오톡 오픈채팅 링크를 입력해 주세요.")
            }
        }
    }

    private fun isHttpUrl(value: String): Boolean = runCatching {
        URI(value).let { it.scheme in setOf("http", "https") && !it.host.isNullOrBlank() }
    }.getOrDefault(false)
}

data class PublishRecruitmentPostRequest(
    @field:AssertTrue(message = "모집글 등록에 필요한 정보 제공 및 운영 정책에 동의해야 합니다.")
    val agreedToPolicy: Boolean,
)

private fun invalid(field: String, reason: String): Nothing = throw InvalidRequestFieldException(field, reason)

private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
