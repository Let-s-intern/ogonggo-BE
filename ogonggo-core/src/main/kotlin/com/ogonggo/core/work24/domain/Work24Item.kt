package com.ogonggo.core.work24.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 고용24 목록 API에서 받아 온 항목 하나의 원본이다.
 *
 * 같은 항목이 다시 오면 저장하지 않고 건너뛴다. 그래서 `createdAt`이 처음 수집한 시각이고
 * `payload`는 그때의 응답이며, 고용24에서 내용이 바뀌어도 갱신하지 않는다.
 * 채용공고·부트캠프로 옮기는 작업은 이 원본을 읽어 따로 한다.
 *
 * 수집한 사실을 지우지 않으므로 소프트 삭제 컬럼을 두지 않는다.
 */
@Entity
@Table(
    name = "work24_items",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_work24_item_api_external_id", columnNames = ["api", "external_id"]),
    ],
)
internal class Work24Item(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    val api: Work24Api, /* 항목을 받아 온 고용24 API */

    @Column(name = "external_id", nullable = false, length = EXTERNAL_ID_MAX_LENGTH)
    val externalId: String, /* 고용24가 준 항목 식별값. API 안에서만 유일하다 */

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    val payload: String, /* 항목 원본 JSON */
) : BaseTimeEntity() {

    init {
        require(externalId.isNotBlank()) { "고용24 항목 식별값은 비어 있을 수 없습니다." }
        require(externalId.length <= EXTERNAL_ID_MAX_LENGTH) { "고용24 항목 식별값이 너무 깁니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 고용24 항목 식별자 */
        protected set

    companion object {
        const val EXTERNAL_ID_MAX_LENGTH = 200
    }
}
