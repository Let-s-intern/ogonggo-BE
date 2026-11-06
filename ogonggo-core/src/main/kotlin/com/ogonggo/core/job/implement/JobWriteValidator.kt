package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.JobApplicationMethod
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import java.time.LocalDateTime

/**
 * 기업회원 공고 등록·수정 입력의 칸 사이 규칙이다. 도메인도 막지만 요청 단계에서 어느 칸이 틀렸는지 알린다.
 * 기업회원의 등록·수정(사용자 API)과 운영자의 대리 등록(관리자 API)이 같은 규칙을 쓴다.
 * 실패를 어떤 예외로 알릴지는 응답 형식을 정하는 API가 [invalid]로 넘겨준다.
 */
object JobWriteValidator {

    fun validate(command: JobAppendDto, invalid: (field: String, reason: String) -> Nothing) = validate(
        jobField = command.jobField,
        jobRole = command.jobRole,
        logoUrl = command.logoUrl,
        region = command.region,
        subRegion = command.subRegion,
        recruitmentType = command.recruitmentType,
        recruitmentStartAt = command.recruitmentStartAt,
        recruitmentEndAt = command.recruitmentEndAt,
        applicationMethod = command.applicationMethod,
        applyEmail = command.applyEmail,
        invalid = invalid,
    )

    fun validate(command: JobUpdateDto, invalid: (field: String, reason: String) -> Nothing) = validate(
        jobField = command.jobField,
        jobRole = command.jobRole,
        logoUrl = command.logoUrl,
        region = command.region,
        subRegion = command.subRegion,
        recruitmentType = command.recruitmentType,
        recruitmentStartAt = command.recruitmentStartAt,
        recruitmentEndAt = command.recruitmentEndAt,
        applicationMethod = command.applicationMethod,
        applyEmail = command.applyEmail,
        invalid = invalid,
    )

    /**
     * 직무는 함께 보낸 직군에, 시·군·구는 함께 보낸 시·도에 속해야 한다.
     * 상시 채용은 종료 일시가 없어야 하고, 시작 일시는 종료 일시보다 늦을 수 없다.
     * 이메일 지원을 고르면 지원서를 받을 주소가 있어야 한다.
     * 빈 문자열은 `@Email`·`@URL`을 통과하지만 도메인은 공백 값을 막으므로 여기서 어느 칸인지 알린다.
     */
    private fun validate(
        jobField: JobField?,
        jobRole: JobRole?,
        logoUrl: String?,
        region: Region?,
        subRegion: SubRegion?,
        recruitmentType: JobRecruitmentType,
        recruitmentStartAt: LocalDateTime?,
        recruitmentEndAt: LocalDateTime?,
        applicationMethod: JobApplicationMethod?,
        applyEmail: String?,
        invalid: (field: String, reason: String) -> Nothing,
    ) {
        if (jobRole != null && jobRole.jobField != jobField) {
            invalid("jobRole", "jobField와 같은 직군의 직무여야 합니다.")
        }
        if (logoUrl != null && logoUrl.isBlank()) {
            invalid("logoUrl", "공백일 수 없습니다.")
        }
        if (subRegion != null && subRegion.region != region) {
            invalid("subRegion", "region과 같은 시·도의 시·군·구여야 합니다.")
        }
        if (recruitmentType == JobRecruitmentType.ALWAYS_OPEN && recruitmentEndAt != null) {
            invalid("recruitmentEndAt", "상시 채용에는 모집 종료 일시를 둘 수 없습니다.")
        }
        if (recruitmentStartAt != null && recruitmentEndAt != null && recruitmentStartAt.isAfter(recruitmentEndAt)) {
            invalid("recruitmentStartAt", "모집 종료 일시보다 늦을 수 없습니다.")
        }
        if (applicationMethod == JobApplicationMethod.EMAIL && applyEmail == null) {
            invalid("applyEmail", "이메일 지원에는 지원 이메일이 필요합니다.")
        }
        if (applyEmail != null && applyEmail.isBlank()) {
            invalid("applyEmail", "공백일 수 없습니다.")
        }
    }
}
