package com.ogonggo.core.concern.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/** 고민글의 조회 수와 답변(부모 댓글) 수다. 고민글을 등록할 때 함께 만든다. */
@Entity
@Table(name = "concern_metrics")
internal class ConcernMetric(
    @Column(name = "concern_id", nullable = false, unique = true)
    val concernId: Long,
) : BaseTimeEntity() {

    init {
        require(concernId > 0) { "고민글 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0
        protected set

    @Column(name = "comment_count", nullable = false)
    var commentCount: Long = 0
        protected set
}
