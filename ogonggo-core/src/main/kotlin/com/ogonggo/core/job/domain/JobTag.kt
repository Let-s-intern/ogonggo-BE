package com.ogonggo.core.job.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "job_tags",
    uniqueConstraints = [UniqueConstraint(name = "uk_job_tag", columnNames = ["job_id", "tag_id"])],
)
internal class JobTag(
    @Column(name = "job_id", nullable = false)
    val jobId: Long, /* 태그가 연결된 채용공고 식별자 */

    @Column(name = "tag_id", nullable = false)
    val tagId: Long, /* 채용공고에 연결된 태그 식별자 */
) : BaseTimeEntity() {

    init {
        require(jobId > 0) { "채용공고 식별자는 양수여야 합니다." }
        require(tagId > 0) { "태그 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 채용공고 태그 연결 식별자 */
        protected set
}
