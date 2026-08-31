package com.ogonggo.core.user.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "company_profiles")
internal class CompanyProfile(
    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long,

    organizationName: String,
    managerName: String,
) : BaseTimeEntity() {

    init {
        require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
        require(organizationName.isNotBlank()) { "기관명은 비어 있을 수 없습니다." }
        require(managerName.isNotBlank()) { "담당자 이름은 비어 있을 수 없습니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "organization_name", nullable = false, length = 150)
    var organizationName: String = organizationName
        protected set

    @Column(name = "manager_name", nullable = false, length = 100)
    var managerName: String = managerName
        protected set
}
