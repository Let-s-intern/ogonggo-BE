package com.ogonggo.core.job.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "job_metrics")
internal class JobMetric(
    @Column(name = "job_id", nullable = false, unique = true)
    val jobId: Long, /* 지표 대상 채용공고 식별자 */

    viewCount: Long = 0,
    bookmarkCount: Long = 0,
    commentCount: Long = 0,
) : BaseTimeEntity() {

    init {
        require(jobId > 0) { "채용공고 식별자는 양수여야 합니다." }
        require(viewCount >= 0) { "조회 수는 음수일 수 없습니다." }
        require(bookmarkCount >= 0) { "북마크 수는 음수일 수 없습니다." }
        require(commentCount >= 0) { "댓글 수는 음수일 수 없습니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 채용공고 지표 식별자 */
        protected set

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = viewCount /* 조회 수 */
        protected set

    @Column(name = "bookmark_count", nullable = false)
    var bookmarkCount: Long = bookmarkCount /* 북마크 수 */
        protected set

    @Column(name = "comment_count", nullable = false)
    var commentCount: Long = commentCount /* 댓글 수 */
        protected set

    fun increaseViewCount() {
        viewCount++
    }

    fun decreaseViewCount() {
        check(viewCount > 0) { "조회 수는 0보다 작아질 수 없습니다." }
        viewCount--
    }

    fun increaseBookmarkCount() {
        bookmarkCount++
    }

    fun decreaseBookmarkCount() {
        check(bookmarkCount > 0) { "북마크 수는 0보다 작아질 수 없습니다." }
        bookmarkCount--
    }

    fun updateBookmarkCount(count: Long) {
        require(count >= 0) { "북마크 수는 음수일 수 없습니다." }
        bookmarkCount = count
    }

    fun increaseCommentCount() {
        commentCount++
    }

    fun decreaseCommentCount() {
        check(commentCount > 0) { "댓글 수는 0보다 작아질 수 없습니다." }
        commentCount--
    }
}
