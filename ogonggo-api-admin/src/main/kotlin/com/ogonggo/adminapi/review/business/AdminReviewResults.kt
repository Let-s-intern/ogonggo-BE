package com.ogonggo.adminapi.review.business

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampContentField
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.implement.dto.BootcampCurriculumDto
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.review.domain.ReviewContentType
import com.ogonggo.core.review.domain.ReviewStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 검수 화면이 종류와 무관하게 같은 모양으로 그리도록 서버가 차이를 미리 없앤 항목이다.
 * 화면이 종류로 갈라지기 시작하면 키보드 흐름이 종류마다 어긋난다.
 */
data class AdminReviewItem(
    val type: ReviewContentType,
    val id: Long,
    val title: String,
    val companyName: String,
    val registeredAt: LocalDateTime,
    val sourceUrl: String?,
    val meta: List<AdminReviewMeta>,
    val sections: List<AdminReviewSection>,
) {
    companion object {
        internal fun from(job: Job): AdminReviewItem = AdminReviewItem(
            type = ReviewContentType.JOB,
            id = checkNotNull(job.id) { "채용공고 식별자가 없습니다." },
            title = job.title,
            companyName = job.companyName,
            registeredAt = job.createdAt,
            sourceUrl = job.sourceUrl,
            meta = listOfNotNull(
                AdminReviewMeta("고용 형태", job.employmentType.desc),
                AdminReviewMeta("경력", job.experienceType.desc),
                job.region?.let { AdminReviewMeta("지역", it) },
                AdminReviewMeta(
                    "모집 마감",
                    recruitmentEndText(
                        alwaysOpen = job.recruitmentType == JobRecruitmentType.ALWAYS_OPEN,
                        alwaysOpenText = JobRecruitmentType.ALWAYS_OPEN.desc,
                        recruitmentEndAt = job.recruitmentEndAt,
                    ),
                ),
                job.applicationEmail?.let { AdminReviewMeta("지원 이메일", it) },
                job.inquiryEmail?.let { AdminReviewMeta("문의 이메일", it) },
            ),
            sections = JobContentField.entries.mapNotNull { field ->
                job.contentOf(field)?.let { AdminReviewSection(field.fieldName, field.desc, it) }
            } + listOfNotNull(
                job.recruitmentNotice?.let { AdminReviewSection(NOT_EDITABLE_FIELD, "채용 안내사항", it) },
            ),
        )

        internal fun from(bootcamp: Bootcamp, curriculums: List<BootcampCurriculumDto.Response>): AdminReviewItem =
            AdminReviewItem(
                type = ReviewContentType.BOOTCAMP,
                id = checkNotNull(bootcamp.id) { "부트캠프 식별자가 없습니다." },
                title = bootcamp.title,
                companyName = bootcamp.companyName,
                registeredAt = bootcamp.createdAt,
                // 부트캠프는 원문이 없고 지원 페이지만 있는 경우가 많다.
                sourceUrl = bootcamp.sourceUrl ?: bootcamp.applicationUrl,
                meta = listOf(
                    AdminReviewMeta("진행 방식", bootcamp.operationType.desc),
                    AdminReviewMeta("수강료", tuitionText(bootcamp)),
                    AdminReviewMeta(
                        "교육 기간",
                        "${bootcamp.programStartDate.format(DATE)} ~ ${bootcamp.programEndDate.format(DATE)}",
                    ),
                    AdminReviewMeta(
                        "모집 마감",
                        recruitmentEndText(
                            alwaysOpen = bootcamp.recruitmentType == BootcampRecruitmentType.ALWAYS_OPEN,
                            alwaysOpenText = BootcampRecruitmentType.ALWAYS_OPEN.desc,
                            recruitmentEndAt = bootcamp.recruitmentEndAt,
                        ),
                    ),
                ),
                sections = BootcampContentField.entries.mapNotNull { field ->
                    bootcamp.contentOf(field)?.let { AdminReviewSection(field.fieldName, field.desc, it) }
                } + listOfNotNull(
                    curriculums.takeIf { it.isNotEmpty() }?.let { items ->
                        AdminReviewSection(
                            field = NOT_EDITABLE_FIELD,
                            label = "커리큘럼",
                            body = items.joinToString("\n") { "${it.startWeek}~${it.endWeek}주 ${it.subtitle}" },
                        )
                    },
                ),
            )

        /** 구조가 있는 값처럼 본문 수정으로 고칠 수 없는 섹션은 칸 이름을 비운다. */
        private const val NOT_EDITABLE_FIELD = ""
        private val DATE = DateTimeFormatter.ISO_LOCAL_DATE

        private fun recruitmentEndText(
            alwaysOpen: Boolean,
            alwaysOpenText: String,
            recruitmentEndAt: LocalDateTime?,
        ): String = when {
            alwaysOpen -> alwaysOpenText
            recruitmentEndAt == null -> "미정"
            else -> recruitmentEndAt.toLocalDate().format(DATE)
        }

        private fun tuitionText(bootcamp: Bootcamp): String =
            bootcamp.tuitionAmount?.let { "${bootcamp.tuitionType.desc} (${"%,d".format(it)}원)" }
                ?: bootcamp.tuitionType.desc
    }
}

/** 값은 화면에 그대로 보여 줄 한국어로 풀어서 준다. 화면이 enum을 풀기 시작하면 종류별 분기가 다시 생긴다. */
data class AdminReviewMeta(
    val label: String,
    val value: String,
)

/** `field`는 콘텐츠의 실제 칸 이름이다. 검수 화면에서 본문을 고칠 때 이 이름으로 수정 API에 되돌려 보낸다. */
data class AdminReviewSection(
    val field: String,
    val label: String,
    val body: String,
)

data class AdminReviewDecisionResult(
    val type: ReviewContentType,
    val id: Long,
    val reviewStatus: ReviewStatus,
    /** 판정 뒤에 남은 검수 대기 건수. 채용공고와 부트캠프를 합한다. */
    val remaining: Long,
)
