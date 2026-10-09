package com.ogonggo.adminapi.letscareercontent.presentation.request

import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.letscareercontent.domain.LetsCareerContent
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentTags
import com.ogonggo.core.letscareercontent.domain.LetsCareerContentTopic
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/** 크롤러가 AI로 붙인 추천용 태그다. 직군·직무를 모두 비우면 어느 직무에나 맞는 콘텐츠로 봅니다. */
data class CrawlerLetsCareerContentTagRequest(
    @field:Schema(description = "태그 대상을 받을 때 함께 받은 해시. 그사이 내용이 바뀌었으면 409입니다")
    @field:NotBlank
    @field:Size(max = LetsCareerContent.CONTENT_HASH_LENGTH)
    val contentHash: String,

    @field:Schema(description = "맞는 직군. 특정 직군 전용이 아니면 빈 배열")
    @field:NotNull
    @field:Size(max = MAX_JOB_FIELDS)
    val jobFields: List<JobField>?,

    @field:Schema(description = "맞는 직무. 직군 전체에 맞으면 빈 배열")
    @field:NotNull
    @field:Size(max = MAX_JOB_ROLES)
    val jobRoles: List<JobRole>?,

    @field:Schema(description = "다루는 취업 준비 단계")
    @field:NotNull
    @field:Size(max = MAX_TOPICS)
    val topics: List<LetsCareerContentTopic>?,
) {
    fun toTags() = LetsCareerContentTags(
        jobFields = jobFields.orEmpty().toSet(),
        jobRoles = jobRoles.orEmpty().toSet(),
        topics = topics.orEmpty().toSet(),
    )

    companion object {
        const val MAX_JOB_FIELDS = 5
        const val MAX_JOB_ROLES = 20
        const val MAX_TOPICS = 5
    }
}
