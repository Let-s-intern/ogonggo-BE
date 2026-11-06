package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampCurriculumDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampPartnerDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampUpdateDto
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 기업회원 부트캠프 등록·수정 입력의 칸 사이 규칙이다. 도메인도 막지만 요청 단계에서 어느 칸이 틀렸는지 알린다.
 * 기업회원의 등록·수정(사용자 API)과 운영자의 대리 등록(관리자 API)이 같은 규칙을 쓴다.
 * 실패를 어떤 예외로 알릴지는 응답 형식을 정하는 API가 [invalid]로 넘겨준다.
 */
object BootcampWriteValidator {

    fun validate(command: BootcampAppendDto, invalid: (field: String, reason: String) -> Nothing) = validate(
        Values(
            recruitmentType = command.recruitmentType,
            recruitmentStartAt = command.recruitmentStartAt,
            recruitmentEndAt = command.recruitmentEndAt,
            programStartDate = command.programStartDate,
            programEndDate = command.programEndDate,
            publicationStartAt = command.publicationStartAt,
            publicationEndAt = command.publicationEndAt,
            applicationMethod = command.applicationMethod,
            applicationUrl = command.applicationUrl,
            optionalTexts = listOf(
                "eligibilityAndSelectionProcess" to command.eligibilityAndSelectionProcess,
                "logoUrl" to command.logoUrl,
                "instructorInfo" to command.instructorInfo,
                "programFeatures" to command.programFeatures,
                "completionRequirements" to command.completionRequirements,
                "managerEmail" to command.managerEmail,
                "inquiryUrl" to command.inquiryUrl,
                "sourceUrl" to command.sourceUrl,
            ),
            partners = command.partners,
            curriculums = command.curriculums,
        ),
        invalid,
    )

    fun validate(command: BootcampUpdateDto, invalid: (field: String, reason: String) -> Nothing) = validate(
        Values(
            recruitmentType = command.recruitmentType,
            recruitmentStartAt = command.recruitmentStartAt,
            recruitmentEndAt = command.recruitmentEndAt,
            programStartDate = command.programStartDate,
            programEndDate = command.programEndDate,
            publicationStartAt = command.publicationStartAt,
            publicationEndAt = command.publicationEndAt,
            applicationMethod = command.applicationMethod,
            applicationUrl = command.applicationUrl,
            optionalTexts = listOf(
                "eligibilityAndSelectionProcess" to command.eligibilityAndSelectionProcess,
                "logoUrl" to command.logoUrl,
                "instructorInfo" to command.instructorInfo,
                "programFeatures" to command.programFeatures,
                "completionRequirements" to command.completionRequirements,
                "managerEmail" to command.managerEmail,
                "inquiryUrl" to command.inquiryUrl,
                "sourceUrl" to command.sourceUrl,
            ),
            partners = command.partners,
            curriculums = command.curriculums,
        ),
        invalid,
    )

    private class Values(
        val recruitmentType: BootcampRecruitmentType,
        val recruitmentStartAt: LocalDateTime?,
        val recruitmentEndAt: LocalDateTime?,
        val programStartDate: LocalDate,
        val programEndDate: LocalDate,
        val publicationStartAt: LocalDateTime?,
        val publicationEndAt: LocalDateTime?,
        val applicationMethod: BootcampApplicationMethod,
        val applicationUrl: String?,
        /** 빈 문자열은 `@Email`·`@URL`을 통과하지만 도메인은 공백 값을 막으므로 여기서 어느 칸인지 알린다. */
        val optionalTexts: List<Pair<String, String?>>,
        val partners: List<BootcampPartnerDto.Request>,
        val curriculums: List<BootcampCurriculumDto.Request>,
    )

    private fun validate(values: Values, invalid: (field: String, reason: String) -> Nothing) {
        val recruitmentStart = values.recruitmentStartAt
        val recruitmentEnd = values.recruitmentEndAt
        val publicationStart = values.publicationStartAt
        val publicationEnd = values.publicationEndAt
        if (values.recruitmentType == BootcampRecruitmentType.PERIOD && recruitmentStart == null) {
            invalid("recruitmentStartAt", "기간 모집의 시작 일시는 필수입니다.")
        }
        if (values.recruitmentType == BootcampRecruitmentType.PERIOD && recruitmentEnd == null) {
            invalid("recruitmentEndAt", "기간 모집의 종료 일시는 필수입니다.")
        }
        if (values.recruitmentType == BootcampRecruitmentType.ALWAYS_OPEN && recruitmentEnd != null) {
            invalid("recruitmentEndAt", "상시 모집에는 모집 종료 일시를 둘 수 없습니다.")
        }
        if (recruitmentStart != null && recruitmentEnd != null && recruitmentStart.isAfter(recruitmentEnd)) {
            invalid("recruitmentStartAt", "모집 종료 일시보다 늦을 수 없습니다.")
        }
        if (values.programStartDate.isAfter(values.programEndDate)) {
            invalid("programStartDate", "교육 종료일보다 늦을 수 없습니다.")
        }
        if (publicationStart != null && publicationEnd != null && publicationStart.isAfter(publicationEnd)) {
            invalid("publicationStartAt", "공개 종료 일시보다 늦을 수 없습니다.")
        }
        if (values.applicationMethod == BootcampApplicationMethod.EXTERNAL_PAGE && values.applicationUrl.isNullOrBlank()) {
            invalid("applicationUrl", "외부 페이지 지원 링크는 필수입니다.")
        }
        if (values.applicationMethod == BootcampApplicationMethod.EMAIL && values.applicationUrl != null) {
            invalid("applicationUrl", "이메일 지원에는 외부 지원 링크를 설정할 수 없습니다.")
        }
        values.optionalTexts.firstOrNull { (_, value) -> value != null && value.isBlank() }?.let { (field, _) ->
            invalid(field, "공백일 수 없습니다.")
        }
        if (values.partners.map { it.partnerName }.distinct().size != values.partners.size) {
            invalid("partners", "중복된 파트너사명은 등록할 수 없습니다.")
        }
        values.curriculums.forEachIndexed { index, curriculum ->
            if (curriculum.startWeek > curriculum.endWeek) {
                invalid("curriculums[$index].startWeek", "종료 주차보다 클 수 없습니다.")
            }
        }
    }
}
