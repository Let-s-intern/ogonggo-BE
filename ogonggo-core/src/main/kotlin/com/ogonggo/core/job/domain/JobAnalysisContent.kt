package com.ogonggo.core.job.domain

/**
 * 채용공고 상세의 '공고 분석' 탭에 보여 주는 정리다. 크롤러가 AI로 만들어 보낸다.
 *
 * 항목의 이름과 개수는 프런트가 그대로 그리는 약속이다. 값이 `null`인 칸은 공고에서 확인할 수 없다는 뜻이며,
 * 프런트는 '공고에 명시 없음'으로 그린다.
 */
data class JobAnalysisContent(
    val tasks: List<Task>,
    val required: List<String>,
    val preferred: List<String>,
    val employment: Employment,
    val submission: Submission,
    val competencies: List<Competency>,
) {

    init {
        require(tasks.size <= MAX_TASKS) { "실제 하는 일은 ${MAX_TASKS}개까지입니다." }
        require(required.size <= MAX_CONDITIONS) { "필수 조건은 ${MAX_CONDITIONS}개까지입니다." }
        require(preferred.size <= MAX_CONDITIONS) { "우대 조건은 ${MAX_CONDITIONS}개까지입니다." }
        require(competencies.size <= MAX_COMPETENCIES) { "역량은 ${MAX_COMPETENCIES}개까지입니다." }
    }

    /** 실제 하는 일 하나. `tag`는 일의 성격을 짧게 적은 것이다(예: 기획). */
    data class Task(val tag: String, val text: String)

    /** 칸 하나의 값과 보충 설명. 공고에 없으면 `value`가 `null`이다. */
    data class Fact(val value: String?, val note: String?)

    /** 고용 형태: 형태·전환·급여·소속 */
    data class Employment(val type: Fact, val conversion: Fact, val salary: Fact, val affiliation: Fact)

    /** 제출물과 전형: 제출 서류·자소서·전형·마감 */
    data class Submission(val documents: Fact, val essay: Fact, val process: Fact, val deadline: Fact)

    /** 연결하기 좋은 경험. `quote`는 그 역량을 요구하는 공고 문장 그대로다. */
    data class Competency(
        val name: String,
        val quote: String,
        val description: String,
        val experiences: List<String>,
    ) {
        init {
            require(experiences.size <= MAX_EXPERIENCES) { "연결할 경험은 ${MAX_EXPERIENCES}개까지입니다." }
        }
    }

    companion object {
        const val MAX_TASKS = 3
        const val MAX_CONDITIONS = 8
        const val MAX_COMPETENCIES = 3
        const val MAX_EXPERIENCES = 3

        /** 본문 해시([Job.contentHash]) 길이와 분석 모델 이름의 최대 길이. 저장 칸의 크기다. */
        const val CONTENT_HASH_LENGTH = 64
        const val MODEL_MAX_LENGTH = 100
    }
}
