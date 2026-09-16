package com.ogonggo.core.community.domain

import com.ogonggo.core.common.BaseTimeEntity
import com.ogonggo.core.user.domain.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "community_post_bookmarks",
    uniqueConstraints = [UniqueConstraint(name = "uk_community_post_bookmark_post_user", columnNames = ["post_id", "user_id"])],
    indexes = [Index(name = "idx_community_post_bookmark_user_active", columnList = "user_id, deleted_at, updated_at")],
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

    /**
     * 쓰기 모델은 ID만 다뤄 Community와 User 도메인의 조회 결합을 만들지 않는다.
     * 다만 같은 컬럼을 읽기 전용 연관관계로 매핑해 Hibernate DDL 생성 시 게시글 FK를 보장한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "post_id",
        insertable = false,
        updatable = false,
        foreignKey = ForeignKey(name = "fk_community_post_bookmarks_post"),
    )
    lateinit var post: RecruitmentPost

    /**
     * 사용자 ID는 쓰기·조회 계약의 기준으로 유지한다.
     * 이 읽기 전용 매핑은 DDL의 사용자 FK 생성과 무결성 검증만 담당한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        insertable = false,
        updatable = false,
        foreignKey = ForeignKey(name = "fk_community_post_bookmarks_user"),
    )
    lateinit var user: User
}
