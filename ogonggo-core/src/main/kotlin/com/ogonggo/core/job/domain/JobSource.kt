package com.ogonggo.core.job.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 크롤러가 수집한 채용공고 원문이다. 공고마다 한 행이고, 다시 받으면 같은 행을 바꾼다.
 *
 * 원문 페이지의 본문 영역을 안전한 태그만 남긴 HTML이며, 공고 상세에서 원래 공고처럼 보여 주는 데 쓴다.
 * 공고 목록은 정렬하며 공고 행 전체를 읽으므로, 긴 원문을 공고 행에 두면 정렬 버퍼를 넘긴다. 그래서 따로 둔다.
 * 지우는 경로가 없어 삭제 일시를 두지 않는다. 공고가 지워지면 공고 조회에서 함께 빠진다.
 */
@Entity
@Table(name = "job_sources")
internal class JobSource(
    @Column(name = "job_id", nullable = false, unique = true)
    val jobId: Long, /* 채용공고 식별자 */

    html: String,
) : BaseTimeEntity() {

    init {
        require(jobId > 0) { "채용공고 식별자는 양수여야 합니다." }
        require(html.isNotBlank()) { "원문은 비어 있을 수 없습니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 채용공고 원문 식별자 */
        protected set

    @Column(name = "html", nullable = false, columnDefinition = "LONGTEXT")
    var html: String = html /* 원문 HTML */
        protected set

    fun replace(html: String) {
        require(html.isNotBlank()) { "원문은 비어 있을 수 없습니다." }
        this.html = html
    }
}
