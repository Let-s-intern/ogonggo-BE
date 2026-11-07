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
 * 분석은 AI 서버가 만들어 보내며, 그때의 본문 해시([Job.contentHash])를 함께 남긴다.
 * 본문이 바뀌면 해시가 달라져 사용자에게 보여 주지 않고 다시 분석할 대상이 된다.
 *
 * `jobUpdatedAt`은 분석하거나 본문이 그대로인지 확인했을 때의 공고 수정 일시다. 공고 수정 일시가 이와 다른
 * 공고만 본문을 다시 비교하면 되므로, 분석 대상을 고를 때 모든 공고의 해시를 매번 세지 않는다.
 * 지우는 경로가 없어 삭제 일시를 두지 않는다. 공고가 지워지면 공고 조회에서 함께 빠진다.
 *
 * 운영자가 관리자 콘솔에서 분석을 고치면([edit]) 지금 본문 해시로 저장하고 고친 사람과 일시를 남긴다.
 * 운영자 수정은 AI 분석을 바로잡는 마지막 방어선이라, 본문 해시가 그대로인 동안에는 AI 서버가 덮어쓰지 못한다
 * ([isEditedByAdminFor]). 본문이 바뀌면 운영자가 고친 것이든 아니든 그 분석은 낡은 것이 되어 평소처럼 AI 서버가 다시 분석하고,
 * 그때 표시가 지워진다.
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

    @Column(name = "content_hash", nullable = false, length = JobAnalysisContent.CONTENT_HASH_LENGTH)
    var contentHash: String = contentHash /* 분석한 본문의 해시 */
        protected set

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    var content: String = content /* 분석 내용 JSON */
        protected set

    @Column(name = "guide_version")
    var guideVersion: Int? = guideVersion /* AI 서버의 분석 방법 판 번호 */
        protected set

    @Column(name = "model", nullable = false, length = JobAnalysisContent.MODEL_MAX_LENGTH)
    var model: String = model /* 분석한 AI 모델 */
        protected set

    @Column(name = "job_updated_at", nullable = false)
    var jobUpdatedAt: LocalDateTime = jobUpdatedAt /* 분석하거나 본문을 확인했을 때의 공고 수정 일시 */
        protected set

    @Column(name = "edited_by_user_id")
    var editedByUserId: Long? = null /* 분석을 고친 관리자 식별자. AI 서버 분석이면 null */
        protected set

    @Column(name = "edited_at")
    var editedAt: LocalDateTime? = null /* 관리자가 분석을 고친 일시. AI 서버 분석이면 null */
        protected set

    /** AI 서버가 다시 분석했다. 운영자 수정 표시를 지운다. */
    fun replace(contentHash: String, content: String, guideVersion: Int?, model: String, jobUpdatedAt: LocalDateTime) {
        this.contentHash = contentHash
        this.content = content
        this.guideVersion = guideVersion
        this.model = model
        this.jobUpdatedAt = jobUpdatedAt
        this.editedByUserId = null
        this.editedAt = null
    }

    /** 운영자가 고친 분석이다. 분석 방법 판 번호는 없고 모델 칸에는 [ADMIN_MODEL]을 남긴다. */
    fun edit(contentHash: String, content: String, userId: Long, now: LocalDateTime, jobUpdatedAt: LocalDateTime) {
        require(userId > 0) { "관리자 식별자는 양수여야 합니다." }
        this.contentHash = contentHash
        this.content = content
        this.guideVersion = null
        this.model = ADMIN_MODEL
        this.jobUpdatedAt = jobUpdatedAt
        this.editedByUserId = userId
        this.editedAt = now
    }


    /** 운영자가 이 본문에 대해 고친 분석인지. 참이면 AI 서버가 덮어쓰지 못한다. */
    fun isEditedByAdminFor(contentHash: String): Boolean = editedAt != null && this.contentHash == contentHash

    /** 본문이 그대로임을 확인했다. 다음 대상 선정에서 다시 비교하지 않는다. */
    fun confirm(jobUpdatedAt: LocalDateTime) {
        this.jobUpdatedAt = jobUpdatedAt
    }

    companion object {
        /** 운영자가 고친 분석의 모델 칸 값이다. */
        const val ADMIN_MODEL = "admin"

        internal fun editedByAdmin(
            jobId: Long,
            contentHash: String,
            content: String,
            userId: Long,
            now: LocalDateTime,
            jobUpdatedAt: LocalDateTime,
        ): JobAnalysis = JobAnalysis(jobId, contentHash, content, null, ADMIN_MODEL, jobUpdatedAt)
            .apply { edit(contentHash, content, userId, now, jobUpdatedAt) }
    }
}
