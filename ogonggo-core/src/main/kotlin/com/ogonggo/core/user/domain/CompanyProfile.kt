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
    logoUrl: String? = null,
    managerPhone: String? = null,
    notificationEmail: String? = null,
) : BaseTimeEntity() {

    init {
        require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
        validate(organizationName, managerName, logoUrl, managerPhone, notificationEmail)
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

    @Column(name = "logo_url", length = 2048)
    var logoUrl: String? = logoUrl /* 기업 로고 이미지 주소 */
        protected set

    @Column(name = "manager_phone", length = 20)
    var managerPhone: String? = managerPhone /* 담당자 연락처 */
        protected set

    @Column(name = "notification_email", length = 320)
    var notificationEmail: String? = notificationEmail /* 로그인 이메일과 따로 받는 정보 수신용 이메일 */
        protected set

    fun replace(
        organizationName: String,
        managerName: String,
        logoUrl: String?,
        managerPhone: String?,
        notificationEmail: String?,
    ) {
        validate(organizationName, managerName, logoUrl, managerPhone, notificationEmail)

        this.organizationName = organizationName
        this.managerName = managerName
        this.logoUrl = logoUrl
        this.managerPhone = managerPhone
        this.notificationEmail = notificationEmail
    }
}

private fun validate(
    organizationName: String,
    managerName: String,
    logoUrl: String?,
    managerPhone: String?,
    notificationEmail: String?,
) {
    require(organizationName.isNotBlank()) { "기관명은 비어 있을 수 없습니다." }
    require(managerName.isNotBlank()) { "담당자 이름은 비어 있을 수 없습니다." }
    require(logoUrl == null || logoUrl.isNotBlank()) { "기업 로고 주소는 비어 있을 수 없습니다." }
    require(managerPhone == null || managerPhone.isNotBlank()) { "담당자 연락처는 비어 있을 수 없습니다." }
    require(notificationEmail == null || notificationEmail.isNotBlank()) { "정보 수신용 이메일은 비어 있을 수 없습니다." }
}
