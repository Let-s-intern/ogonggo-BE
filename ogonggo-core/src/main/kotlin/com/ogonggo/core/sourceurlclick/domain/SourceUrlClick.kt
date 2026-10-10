package com.ogonggo.core.sourceurlclick.domain

import com.ogonggo.core.jpa.BaseTimeEntity
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

/**
 * 사용자가 채용공고의 원문이나 부트캠프의 지원 페이지처럼 외부 링크로 이동하는 버튼을 눌렀다는 기록이다.
 *
 * 북마크와 달리 취소할 수 없는 사실이므로 소프트 삭제 컬럼을 두지 않는다.
 * 한 사용자가 같은 콘텐츠를 여러 번 눌러도 행은 하나만 남기며, `createdAt`이 최초 이동 시각이 된다.
 */
@Entity
@Table(
    name = "source_url_clicks",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_source_url_click_target_user",
            columnNames = ["target_type", "target_id", "user_id"],
        ),
    ],
    indexes = [Index(name = "idx_source_url_click_user", columnList = "user_id, created_at")],
)
internal class SourceUrlClick(
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    val targetType: SourceUrlClickTargetType, /* 이동한 콘텐츠의 종류 */

    @Column(name = "target_id", nullable = false)
    val targetId: Long, /* 이동한 채용공고 또는 부트캠프 식별자 */

    @Column(name = "user_id", nullable = false)
    val userId: Long, /* 이동한 사용자 식별자 */
) : BaseTimeEntity() {

    init {
        require(targetId > 0) { "콘텐츠 식별자는 양수여야 합니다." }
        require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 외부 링크 이동 기록 식별자 */
        protected set
}
