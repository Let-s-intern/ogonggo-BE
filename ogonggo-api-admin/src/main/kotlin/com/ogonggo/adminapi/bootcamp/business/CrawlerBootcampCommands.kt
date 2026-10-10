package com.ogonggo.adminapi.bootcamp.business

import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import java.time.LocalDate
import java.time.LocalDateTime

/** 크롤러가 보낸 부트캠프 한 건의 값이다. 등록과 교체가 같은 값을 쓴다. */
data class CrawlerBootcampCommand(
    val companyName: String,
    val title: String,
    val programType: String,
    val operationType: BootcampOperationType,
    val recruitmentType: BootcampRecruitmentType,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    val capacity: Int?,
    val tuitionType: BootcampTuitionType,
    val tuitionAmount: Long?,
    val representativeImageUrl: String,
    /** 운영 회사 로고. 대표 이미지와 따로 온다. 없으면 로고를 두지 않는다. */
    val logoUrl: String?,
    val shortDescription: String,
    val content: String,
    val eligibilityAndSelectionProcess: String?,
    val applicationMethod: BootcampApplicationMethod,
    val applicationUrl: String?,
    val managerEmail: String?,
    val inquiryUrl: String?,
    val sourceUrl: String,
    /**
     * 모집 상태. `RECRUITING`이나 `CLOSED`만 온다.
     * 없으면 등록은 모집 중으로 저장하고, 교체는 모집 상태를 바꾸지 않는다.
     */
    val status: BootcampRecruitmentStatus?,
    /** 보낸 순서가 노출 순서다. */
    val curriculums: List<CrawlerBootcampCurriculumCommand>,
)

data class CrawlerBootcampCurriculumCommand(
    val startWeek: Int,
    val endWeek: Int,
    val subtitle: String,
)
