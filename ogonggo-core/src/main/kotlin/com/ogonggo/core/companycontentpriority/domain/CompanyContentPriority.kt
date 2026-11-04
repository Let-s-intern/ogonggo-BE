package com.ogonggo.core.companycontentpriority.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 기업회원이 비즈니스로 등록한 마감 전 채용공고·부트캠프를 공개 목록 맨 앞에 둘지 정하는 설정이다.
 * 유료 광고 구좌가 나오기 전까지의 임시 혜택이라 운영자가 배포 없이 끈다.
 * 한 행만 두며, 행이 없으면 켜진 것으로 본다. 지우는 경로가 없어 삭제 일시를 두지 않는다.
 */
@Entity
@Table(name = "company_content_priorities")
class CompanyContentPriority internal constructor(
    enabled: Boolean,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 우선 노출 설정 식별자 */
        protected set

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = enabled /* 우선 노출 켜짐 여부 */
        protected set

    fun replace(enabled: Boolean) {
        this.enabled = enabled
    }

    companion object {
        /** 행이 없을 때의 값이다. 배포만으로 지금 동작이 바뀌지 않게 켜짐이다. */
        const val DEFAULT_ENABLED = true
    }
}
