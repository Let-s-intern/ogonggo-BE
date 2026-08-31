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
    name = "tags",
    uniqueConstraints = [UniqueConstraint(name = "uk_tag_name", columnNames = ["name"])],
)
internal class Tag(
    @Column(nullable = false, length = 100)
    val name: String, /* 태그명 */
) : BaseTimeEntity() {

    init {
        require(name.isNotBlank()) { "태그명은 비어 있을 수 없습니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 태그 식별자 */
        protected set
}
