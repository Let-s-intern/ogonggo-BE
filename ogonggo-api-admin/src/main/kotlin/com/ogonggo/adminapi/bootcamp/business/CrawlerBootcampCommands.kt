package com.ogonggo.adminapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import java.time.LocalDate
import java.time.LocalDateTime

/** 크롤러가 보낸 부트캠프 한 건의 값이다. 등록과 교체가 같은 값을 쓴다. */
data class CrawlerBootcampCommand(
    val companyName: String,
    val title: String,
    val programType: String,
    val operationType: OperationType,
    val recruitmentType: BootcampRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    val capacity: Int?,
    val tuitionType: TuitionType,
    val tuitionAmount: Long?,
    val representativeImageUrl: String,
    val shortDescription: String,
    val content: String,
    val eligibilityAndSelectionProcess: String?,
    val applicationMethod: ApplicationMethod,
    val applicationUrl: String?,
    val managerEmail: String?,
    val inquiryUrl: String?,
    val sourceUrl: String,
    /**
     * 모집 상태. `RECRUITING`이나 `CLOSED`만 온다.
     * 없으면 등록은 모집 중으로 저장하고, 교체는 모집 상태를 바꾸지 않는다.
     */
    val status: BootcampStatus?,
    /** 보낸 순서가 노출 순서다. */
    val curriculums: List<CrawlerBootcampCurriculumCommand>,
)

data class CrawlerBootcampCurriculumCommand(
    val startWeek: Int,
    val endWeek: Int,
    val subtitle: String,
)
