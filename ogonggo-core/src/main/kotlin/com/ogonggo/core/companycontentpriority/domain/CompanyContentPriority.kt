package com.ogonggo.core.companycontentpriority.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 기업회원이 비즈니스로 등록한 마감 전 채용공고·부트캠프를 공개 목록 맨 앞에 둘지 정하는 설정이다.
 * 유료 광고 구좌가 나오기 전까지의 임시 혜택이라 운영자가 배포 없이 끈다.
 * 식별자를 [SINGLETON_ID]로 고정해 한 행만 둔다. 행이 없을 때 두 요청이 함께 만들어도 기본 키가 두 번째 행을 막는다.
 * 행이 없으면 켜진 것으로 본다. 지우는 경로가 없어 삭제 일시를 두지 않는다.
 */
@Entity
@Table(name = "company_content_priorities")
class CompanyContentPriority internal constructor(
    enabled: Boolean,
) : BaseTimeEntity() {

    @Id
    var id: Long = SINGLETON_ID /* 우선 노출 설정 식별자 */
        protected set

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = enabled /* 우선 노출 켜짐 여부 */
        protected set

    fun replace(enabled: Boolean) {
        this.enabled = enabled
    }

    companion object {
        /** 하나뿐인 설정 행의 식별자다. 운영 DB에 미리 넣는 기본 행도 이 값이다. */
        const val SINGLETON_ID = 1L

        /** 행이 없을 때의 값이다. 배포만으로 지금 동작이 바뀌지 않게 켜짐이다. */
        const val DEFAULT_ENABLED = true
    }
}
