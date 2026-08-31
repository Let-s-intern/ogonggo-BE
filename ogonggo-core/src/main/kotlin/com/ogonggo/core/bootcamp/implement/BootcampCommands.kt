package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import java.time.LocalDate
import java.time.LocalDateTime

data class BootcampAppendCommand(
    val ownerUserId: Long? = null,
    val companyName: String,
    val title: String,
    val programType: String,
    val operationType: OperationType,
    val recruitmentType: BootcampRecruitmentType,
    val recruitmentStartAt: LocalDateTime? = null,
    val recruitmentEndAt: LocalDateTime? = null,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    val capacity: Int? = null,
    val tuitionType: TuitionType,
    val tuitionAmount: Long? = null,
    val representativeImageUrl: String,
    val shortDescription: String,
    val content: String,
    val eligibilityAndSelectionProcess: String? = null,
    val applicationMethod: ApplicationMethod,
    val applicationUrl: String? = null,
    val managerEmail: String? = null,
    val inquiryUrl: String? = null,
    val publicationStartAt: LocalDateTime? = null,
    val publicationEndAt: LocalDateTime? = null,
    val sourceUrl: String? = null,
    val partners: List<BootcampPartnerCommand> = emptyList(),
    val curriculums: List<BootcampCurriculumCommand> = emptyList(),
    val status: BootcampStatus = BootcampStatus.DRAFT,
    val closedAt: LocalDateTime? = null,
)

data class BootcampUpdateCommand(
    val companyName: String,
    val title: String,
    val programType: String,
    val operationType: OperationType,
    val recruitmentType: BootcampRecruitmentType,
    val recruitmentStartAt: LocalDateTime? = null,
    val recruitmentEndAt: LocalDateTime? = null,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    val capacity: Int? = null,
    val tuitionType: TuitionType,
    val tuitionAmount: Long? = null,
    val representativeImageUrl: String,
    val shortDescription: String,
    val content: String,
    val eligibilityAndSelectionProcess: String? = null,
    val applicationMethod: ApplicationMethod,
    val applicationUrl: String? = null,
    val managerEmail: String? = null,
    val inquiryUrl: String? = null,
    val publicationStartAt: LocalDateTime? = null,
    val publicationEndAt: LocalDateTime? = null,
    val sourceUrl: String? = null,
    val partners: List<BootcampPartnerCommand> = emptyList(),
    val curriculums: List<BootcampCurriculumCommand> = emptyList(),
)

data class BootcampPartnerCommand(
    val partnerName: String,
    val displayOrder: Int = 0,
)

data class BootcampCurriculumCommand(
    val startWeek: Int,
    val endWeek: Int,
    val subtitle: String,
    val displayOrder: Int = 0,
)
