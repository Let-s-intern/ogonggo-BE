package com.ogonggo.adminapi.bootcamp.presentation.request

import com.ogonggo.adminapi.bootcamp.business.CrawlerBootcampCommand
import com.ogonggo.adminapi.bootcamp.business.CrawlerBootcampCurriculumCommand
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 크롤러가 보내는 부트캠프 한 건이다. 등록과 교체가 같은 칸을 받는다.
 *
 * 필드 사이 규칙은 도메인도 막지만, 도메인에서 막히면 어느 칸이 틀렸는지 알 수 없어 요청 단계에서 먼저 알린다.
 */
@Schema(description = "크롤러가 수집한 부트캠프 등록·교체 요청")
data class CrawlerBootcampRequest(
    @field:Schema(description = "운영 회사명", example = "오공고 교육사")
    @field:NotBlank(message = "운영 회사명은 필수입니다.")
    @field:Size(max = 150, message = "운영 회사명은 150자 이하여야 합니다.")
    val companyName: String,

    @field:Schema(description = "프로그램명", example = "백엔드 부트캠프 3기")
    @field:NotBlank(message = "프로그램명은 필수입니다.")
    @field:Size(max = 255, message = "프로그램명은 255자 이하여야 합니다.")
    val title: String,

    @field:Schema(description = "프로그램 유형", example = "개발")
    @field:NotBlank(message = "프로그램 유형은 필수입니다.")
    @field:Size(max = 50, message = "프로그램 유형은 50자 이하여야 합니다.")
    val programType: String,

    val operationType: OperationType,

    val recruitmentType: BootcampRecruitmentType,

    @field:Schema(description = "모집 시작 일시. 기간 모집이면 필수다. 원문에 시각이 없으면 그날 00:00:00")
    val recruitmentStartAt: LocalDateTime? = null,

    @field:Schema(description = "모집 종료 일시. 기간 모집이면 필수이고 상시 모집이면 보내지 않는다. 원문에 시각이 없으면 그날 23:59:59")
    val recruitmentEndAt: LocalDateTime? = null,

    @field:Schema(description = "교육 시작일")
    val programStartDate: LocalDate,

    @field:Schema(description = "교육 종료일")
    val programEndDate: LocalDate,

    @field:Schema(description = "모집 정원. 원문에 숫자로 적혀 있을 때만 보낸다")
    @field:PositiveOrZero(message = "모집 정원은 0 이상이어야 합니다.")
    val capacity: Int? = null,

    val tuitionType: TuitionType,

    @field:Schema(description = "수강료(원). 원문에 금액이 있을 때만 보낸다")
    @field:PositiveOrZero(message = "수강료는 0 이상이어야 합니다.")
    val tuitionAmount: Long? = null,

    @field:Schema(description = "공고 대표 이미지 주소")
    @field:NotBlank(message = "대표 이미지 주소는 필수입니다.")
    @field:Size(max = 2048, message = "대표 이미지 주소는 2048자 이하여야 합니다.")
    @field:URL(message = "대표 이미지 주소가 URL 형식이 아닙니다.")
    val representativeImageUrl: String,

    @field:Schema(description = "공고 한 줄 소개")
    @field:NotBlank(message = "한 줄 소개는 필수입니다.")
    @field:Size(max = 500, message = "한 줄 소개는 500자 이하여야 합니다.")
    val shortDescription: String,

    @field:Schema(description = "부트캠프 상세 내용")
    @field:NotBlank(message = "상세 내용은 필수입니다.")
    val content: String,

    @field:Schema(description = "지원 자격 및 전형 안내")
    val eligibilityAndSelectionProcess: String? = null,

    val applicationMethod: ApplicationMethod,

    @field:Schema(description = "외부 지원 페이지 주소. 외부 페이지 지원이면 필수이고 이메일 지원이면 보내지 않는다")
    @field:Size(max = 2048, message = "지원 페이지 주소는 2048자 이하여야 합니다.")
    @field:URL(message = "지원 페이지 주소가 URL 형식이 아닙니다.")
    val applicationUrl: String? = null,

    @field:Schema(description = "담당자 이메일", example = "edu@ogonggo.com")
    @field:Size(max = 320, message = "담당자 이메일은 320자 이하여야 합니다.")
    @field:Email(message = "담당자 이메일이 이메일 형식이 아닙니다.")
    val managerEmail: String? = null,

    @field:Schema(description = "문의 링크")
    @field:Size(max = 2048, message = "문의 링크는 2048자 이하여야 합니다.")
    @field:URL(message = "문의 링크가 URL 형식이 아닙니다.")
    val inquiryUrl: String? = null,

    @field:Schema(description = "부트캠프 원문 URL. 같은 원문은 한 번만 등록한다")
    @field:NotBlank(message = "원문 URL은 필수입니다.")
    @field:Size(max = 2048, message = "원문 URL은 2048자 이하여야 합니다.")
    @field:URL(message = "원문 URL이 URL 형식이 아닙니다.")
    val sourceUrl: String,

    @field:Schema(
        description = "모집 상태. RECRUITING(모집 중) 또는 CLOSED(모집 마감)만 보낼 수 있다. " +
            "등록할 때 보내지 않으면 RECRUITING이고, 교체할 때 보내지 않으면 모집 상태를 바꾸지 않는다",
        allowableValues = ["RECRUITING", "CLOSED"],
    )
    val status: BootcampStatus? = null,

    @field:Schema(description = "커리큘럼. 보낸 순서대로 노출한다. 교체하면 기존 커리큘럼을 지우고 이 목록으로 바꾼다")
    @field:Valid
    @field:Size(max = 100, message = "커리큘럼은 100개 이하여야 합니다.")
    val curriculums: List<CrawlerBootcampCurriculumRequest> = emptyList(),
) {

    fun toCommand(): CrawlerBootcampCommand {
        validateRelations()
        return CrawlerBootcampCommand(
            companyName = companyName,
            title = title,
            programType = programType,
            operationType = operationType,
            recruitmentType = recruitmentType,
            recruitmentStartAt = recruitmentStartAt,
            recruitmentEndAt = recruitmentEndAt,
            programStartDate = programStartDate,
            programEndDate = programEndDate,
            capacity = capacity,
            tuitionType = tuitionType,
            tuitionAmount = tuitionAmount,
            representativeImageUrl = representativeImageUrl,
            shortDescription = shortDescription,
            content = content,
            eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
            applicationMethod = applicationMethod,
            applicationUrl = applicationUrl,
            managerEmail = managerEmail,
            inquiryUrl = inquiryUrl,
            sourceUrl = sourceUrl,
            status = status,
            curriculums = curriculums.map(CrawlerBootcampCurriculumRequest::toCommand),
        )
    }

    private fun validateRelations() {
        // 임시저장은 크롤러가 만들 상태가 아니다. 모집 마감 과정도 게시해 두므로 두 값만 받는다.
        if (status == BootcampStatus.DRAFT) {
            invalid("status", "RECRUITING 또는 CLOSED만 보낼 수 있습니다.")
        }
        val recruitmentStart = recruitmentStartAt
        val recruitmentEnd = recruitmentEndAt
        if (recruitmentType == BootcampRecruitmentType.PERIOD && recruitmentStart == null) {
            invalid("recruitmentStartAt", "기간 모집의 시작 일시는 필수입니다.")
        }
        if (recruitmentType == BootcampRecruitmentType.PERIOD && recruitmentEnd == null) {
            invalid("recruitmentEndAt", "기간 모집의 종료 일시는 필수입니다.")
        }
        if (recruitmentType == BootcampRecruitmentType.ALWAYS_OPEN && recruitmentEnd != null) {
            invalid("recruitmentEndAt", "상시 모집에는 모집 종료 일시를 둘 수 없습니다.")
        }
        if (recruitmentStart != null && recruitmentEnd != null && recruitmentStart.isAfter(recruitmentEnd)) {
            invalid("recruitmentStartAt", "모집 종료 일시보다 늦을 수 없습니다.")
        }
        if (programStartDate.isAfter(programEndDate)) {
            invalid("programStartDate", "교육 종료일보다 늦을 수 없습니다.")
        }
        if (applicationMethod == ApplicationMethod.EXTERNAL_PAGE && applicationUrl.isNullOrBlank()) {
            invalid("applicationUrl", "외부 페이지 지원 링크는 필수입니다.")
        }
        if (applicationMethod == ApplicationMethod.EMAIL && applicationUrl != null) {
            invalid("applicationUrl", "이메일 지원에는 외부 지원 링크를 설정할 수 없습니다.")
        }
        listOf(
            "eligibilityAndSelectionProcess" to eligibilityAndSelectionProcess,
            "managerEmail" to managerEmail,
            "inquiryUrl" to inquiryUrl,
        ).firstOrNull { (_, value) -> value != null && value.isBlank() }?.let { (field, _) ->
            invalid(field, "공백일 수 없습니다. 값이 없으면 보내지 않습니다.")
        }
        curriculums.forEachIndexed { index, curriculum ->
            if (curriculum.startWeek > curriculum.endWeek) {
                invalid("curriculums[$index].startWeek", "종료 주차보다 클 수 없습니다.")
            }
        }
    }
}

@Schema(description = "크롤러 부트캠프 커리큘럼 한 줄")
data class CrawlerBootcampCurriculumRequest(
    @field:Schema(description = "시작 주차", example = "1")
    @field:Positive(message = "시작 주차는 1 이상이어야 합니다.")
    val startWeek: Int,

    @field:Schema(description = "종료 주차", example = "4")
    @field:Positive(message = "종료 주차는 1 이상이어야 합니다.")
    val endWeek: Int,

    @field:Schema(description = "소제목", example = "자바와 스프링 기초")
    @field:NotBlank(message = "커리큘럼 소제목은 필수입니다.")
    @field:Size(max = 255, message = "커리큘럼 소제목은 255자 이하여야 합니다.")
    val subtitle: String,
) {
    fun toCommand(): CrawlerBootcampCurriculumCommand =
        CrawlerBootcampCurriculumCommand(startWeek = startWeek, endWeek = endWeek, subtitle = subtitle)
}

private fun invalid(field: String, reason: String): Nothing = throw InvalidRequestFieldException(field, reason)
