package com.ogonggo.core.recruitmentpost.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "recruitment_post_bookmarks",
    uniqueConstraints = [UniqueConstraint(name = "uk_recruitment_post_bookmark_post_user", columnNames = ["post_id", "user_id"])],
    indexes = [Index(name = "idx_recruitment_post_bookmark_user_active", columnList = "user_id, deleted_at, updated_at")],
)
internal class RecruitmentPostBookmark(
    @Column(name = "post_id", nullable = false)
    val postId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,
) : BaseTimeEntity() {

    init {
        require(postId > 0) { "모집글 식별자는 양수여야 합니다." }
        require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set
}
