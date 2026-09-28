package com.ogonggo.core.community.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 사용자가 모집글의 외부 지원 연락처를 열었거나, 마이페이지에서 스크랩한 모집글을 지원 준비 중으로 옮긴 이력이다.
 * 실제 지원서 제출이 아니며, 같은 사용자·모집글 조합은 하나의 행으로 관리한다.
 * 스크랩에서 옮긴 이력은 옮긴 시각을 최초·최근 접근 시각으로 기록한다.
 */
@Entity
@Table(
    name = "recruitment_post_applications",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_recruitment_post_application_post_user", columnNames = ["post_id", "user_id"]),
    ],
    indexes = [
        Index(name = "idx_recruitment_post_application_user_last_clicked", columnList = "user_id, last_clicked_at, id"),
    ],
)
internal class RecruitmentPostApplication(
    @Column(name = "post_id", nullable = false)
    val postId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    firstClickedAt: LocalDateTime,
    lastClickedAt: LocalDateTime,
    applicationStatus: RecruitmentApplicationProgressStatus = RecruitmentApplicationProgressStatus.PREPARING,
) : BaseTimeEntity() {

    init {
        require(postId > 0) { "모집글 식별자는 양수여야 합니다." }
        require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
        require(!lastClickedAt.isBefore(firstClickedAt)) { "최근 접근 시각은 최초 접근 시각보다 빠를 수 없습니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "first_clicked_at", nullable = false)
    var firstClickedAt: LocalDateTime = firstClickedAt
        protected set

    @Column(name = "last_clicked_at", nullable = false)
    var lastClickedAt: LocalDateTime = lastClickedAt
        protected set

    @Enumerated(EnumType.STRING)
    @Column(
        name = "application_status",
        nullable = false,
        columnDefinition = "varchar(20) default 'PREPARING'",
    )
    var applicationStatus: RecruitmentApplicationProgressStatus = applicationStatus
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    fun recordClick(clickedAt: LocalDateTime) {
        deletedAt = null
        if (clickedAt.isAfter(lastClickedAt)) {
            lastClickedAt = clickedAt
        }
    }

    /** 연락처를 열지 않고 스크랩에서 옮겨 온 경우다. 지운 이력을 되살리면 지원 준비 중부터 다시 시작한다. */
    fun restoreAsPreparing() {
        deletedAt = null
        applicationStatus = RecruitmentApplicationProgressStatus.PREPARING
    }

    fun changeStatus(status: RecruitmentApplicationProgressStatus) {
        check(deletedAt == null) { "삭제된 지원 이력의 상태는 변경할 수 없습니다." }
        applicationStatus = status
    }

    fun delete(deletedAt: LocalDateTime) {
        if (this.deletedAt == null) {
            this.deletedAt = deletedAt
        }
    }
}
