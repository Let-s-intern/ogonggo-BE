package com.ogonggo.core.job.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 채용공고 하나의 공고 분석이다. 공고마다 한 행이고, 다시 분석하면 같은 행을 바꾼다.
 *
 * 분석은 크롤러가 AI로 만들어 보내며, 그때의 본문 해시([Job.contentHash])를 함께 남긴다.
 * 본문이 바뀌면 해시가 달라져 사용자에게 보여 주지 않고 다시 분석할 대상이 된다.
 *
 * `jobUpdatedAt`은 분석하거나 본문이 그대로인지 확인했을 때의 공고 수정 일시다. 공고 수정 일시가 이와 다른
 * 공고만 본문을 다시 비교하면 되므로, 분석 대상을 고를 때 모든 공고의 해시를 매번 세지 않는다.
 * 지우는 경로가 없어 삭제 일시를 두지 않는다. 공고가 지워지면 공고 조회에서 함께 빠진다.
 */
@Entity
@Table(name = "job_analyses")
internal class JobAnalysis(
    @Column(name = "job_id", nullable = false, unique = true)
    val jobId: Long, /* 분석 대상 채용공고 식별자 */

    contentHash: String,
    content: String,
    guideVersion: Int?,
    model: String,
    jobUpdatedAt: LocalDateTime,
) : BaseTimeEntity() {

    init {
        require(jobId > 0) { "채용공고 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 공고 분석 식별자 */
        protected set

    @Column(name = "content_hash", nullable = false, length = CONTENT_HASH_LENGTH)
    var contentHash: String = contentHash /* 분석한 본문의 해시 */
        protected set

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    var content: String = content /* 분석 내용 JSON */
        protected set

    @Column(name = "guide_version")
    var guideVersion: Int? = guideVersion /* 크롤러의 분석 방법 판 번호 */
        protected set

    @Column(name = "model", nullable = false, length = MODEL_MAX_LENGTH)
    var model: String = model /* 분석한 AI 모델 */
        protected set

    @Column(name = "job_updated_at", nullable = false)
    var jobUpdatedAt: LocalDateTime = jobUpdatedAt /* 분석하거나 본문을 확인했을 때의 공고 수정 일시 */
        protected set

    fun replace(contentHash: String, content: String, guideVersion: Int?, model: String, jobUpdatedAt: LocalDateTime) {
        this.contentHash = contentHash
        this.content = content
        this.guideVersion = guideVersion
        this.model = model
        this.jobUpdatedAt = jobUpdatedAt
    }

    /** 본문이 그대로임을 확인했다. 다음 대상 선정에서 다시 비교하지 않는다. */
    fun confirm(jobUpdatedAt: LocalDateTime) {
        this.jobUpdatedAt = jobUpdatedAt
    }

    companion object {
        const val CONTENT_HASH_LENGTH = 64
        const val MODEL_MAX_LENGTH = 100
    }
}
