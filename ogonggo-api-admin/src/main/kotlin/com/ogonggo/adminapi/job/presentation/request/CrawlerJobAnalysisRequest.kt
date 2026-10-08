package com.ogonggo.adminapi.job.presentation.request

import com.ogonggo.adminapi.job.business.CrawlerJobAnalysisCommand
import com.ogonggo.core.job.domain.JobAnalysisContent
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

private const val TEXT_MAX = 1000

/** 크롤러가 AI로 만든 공고 분석이다. 칸 이름과 개수는 사용자 상세 응답의 `analysis`와 같다. */
data class CrawlerJobAnalysisRequest(
    @field:Schema(description = "분석 대상을 받을 때 함께 받은 본문 해시. 그사이 본문이 바뀌었으면 409입니다")
    @field:NotBlank
    @field:Size(max = JobAnalysisContent.CONTENT_HASH_LENGTH)
    val contentHash: String,

    @field:Schema(description = "크롤러의 분석 방법 판 번호. 저장하지 않은 방법이면 null")
    @field:PositiveOrZero
    val guideVersion: Int?,

    @field:NotBlank
    @field:Size(max = JobAnalysisContent.MODEL_MAX_LENGTH)
    val model: String,

    @field:Valid
    val analysis: Analysis,
) {

    fun toCommand(): CrawlerJobAnalysisCommand = CrawlerJobAnalysisCommand(
        contentHash = contentHash,
        guideVersion = guideVersion,
        model = model,
        content = analysis.toContent(),
    )

    data class Analysis(
        @field:Size(max = JobAnalysisContent.MAX_TASKS)
        @field:Valid
        val tasks: List<Task>,

        @field:Size(max = JobAnalysisContent.MAX_CONDITIONS)
        val required: List<@NotBlank @Size(max = TEXT_MAX) String>,

        @field:Size(max = JobAnalysisContent.MAX_CONDITIONS)
        val preferred: List<@NotBlank @Size(max = TEXT_MAX) String>,

        @field:Valid
        val employment: Employment,

        @field:Valid
        val submission: Submission,

        @field:Size(max = JobAnalysisContent.MAX_COMPETENCIES)
        @field:Valid
        val competencies: List<Competency>,
    ) {
        fun toContent(): JobAnalysisContent = JobAnalysisContent(
            tasks = tasks.map { JobAnalysisContent.Task(it.tag, it.text) },
            required = required,
            preferred = preferred,
            employment = JobAnalysisContent.Employment(
                type = employment.type.toFact(),
                conversion = employment.conversion.toFact(),
                salary = employment.salary.toFact(),
                affiliation = employment.affiliation.toFact(),
            ),
            submission = JobAnalysisContent.Submission(
                documents = submission.documents.toFact(),
                essay = submission.essay.toFact(),
                process = submission.process.toFact(),
                deadline = submission.deadline.toFact(),
            ),
            competencies = competencies.map {
                JobAnalysisContent.Competency(it.name, it.quote, it.description, it.experiences)
            },
        )
    }

    data class Task(
        @field:NotBlank @field:Size(max = TEXT_MAX) val tag: String,
        @field:NotBlank @field:Size(max = TEXT_MAX) val text: String,
    )

    /** 공고에서 확인할 수 없으면 `value`를 비웁니다. */
    data class Fact(
        @field:Size(max = TEXT_MAX) val value: String?,
        @field:Size(max = TEXT_MAX) val note: String?,
    ) {
        fun toFact(): JobAnalysisContent.Fact =
            JobAnalysisContent.Fact(value = value?.takeIf(String::isNotBlank), note = note?.takeIf(String::isNotBlank))
    }

    data class Employment(
        @field:Valid val type: Fact,
        @field:Valid val conversion: Fact,
        @field:Valid val salary: Fact,
        @field:Valid val affiliation: Fact,
    )

    data class Submission(
        @field:Valid val documents: Fact,
        @field:Valid val essay: Fact,
        @field:Valid val process: Fact,
        @field:Valid val deadline: Fact,
    )

    data class Competency(
        @field:NotBlank @field:Size(max = TEXT_MAX) val name: String,
        @field:Schema(description = "그 역량을 요구하는 공고 문장 그대로")
        @field:NotBlank
        @field:Size(max = TEXT_MAX)
        val quote: String,
        @field:NotBlank @field:Size(max = TEXT_MAX) val description: String,
        @field:Size(max = JobAnalysisContent.MAX_EXPERIENCES)
        val experiences: List<@NotBlank @Size(max = TEXT_MAX) String>,
    )
}
