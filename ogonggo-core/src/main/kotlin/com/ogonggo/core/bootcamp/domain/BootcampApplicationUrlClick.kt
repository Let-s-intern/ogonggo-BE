package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 사용자가 부트캠프의 외부 지원 페이지로 이동하는 버튼을 눌렀다는 기록이다.
 *
 * 북마크와 달리 취소할 수 없는 사실이므로 소프트 삭제 컬럼을 두지 않는다.
 * 한 사용자가 같은 부트캠프를 여러 번 눌러도 행은 하나만 남기며, `createdAt`이 최초 이동 시각이 된다.
 */
@Entity
@Table(
    name = "bootcamp_application_url_clicks",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_bootcamp_application_url_click_bootcamp_user",
            columnNames = ["bootcamp_id", "user_id"],
        ),
    ],
    indexes = [Index(name = "idx_bootcamp_application_url_click_user", columnList = "user_id, created_at")],
)
internal class BootcampApplicationUrlClick(
    @Column(name = "bootcamp_id", nullable = false)
    val bootcampId: Long, /* 지원 페이지로 이동한 부트캠프 식별자 */

    @Column(name = "user_id", nullable = false)
    val userId: Long, /* 지원 페이지로 이동한 사용자 식별자 */
) : BaseTimeEntity() {

    init {
        require(bootcampId > 0) { "부트캠프 식별자는 양수여야 합니다." }
        require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 지원 페이지 이동 기록 식별자 */
        protected set
}
