package com.ogonggo.core.community.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "community_post_metrics")
internal class PostMetric(
    @Column(name = "post_id", nullable = false, unique = true)
    val postId: Long,
    viewCount: Long = 0,
    commentCount: Long = 0,
    bookmarkCount: Long = 0,
) : BaseTimeEntity() {

    init {
        require(postId > 0) { "모집글 식별자는 양수여야 합니다." }
        require(viewCount >= 0) { "조회 수는 음수일 수 없습니다." }
        require(commentCount >= 0) { "댓글 수는 음수일 수 없습니다." }
        require(bookmarkCount >= 0) { "북마크 수는 음수일 수 없습니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = viewCount
        protected set

    @Column(name = "comment_count", nullable = false)
    var commentCount: Long = commentCount
        protected set

    @Column(name = "bookmark_count", nullable = false, columnDefinition = "bigint default 0")
    var bookmarkCount: Long = bookmarkCount
        protected set
}
