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
    logoImageId: String? = null,
    logoUrl: String? = null,
    managerPhone: String? = null,
    notificationEmail: String? = null,
) : BaseTimeEntity() {

    init {
        require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
        validateBasicInfo(organizationName, logoImageId, logoUrl)
        validateManagerInfo(managerName, managerPhone, notificationEmail)
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

    @Column(name = "logo_image_id", length = 36)
    var logoImageId: String? = logoImageId /* 기업 로고 이미지 식별자(image_assets.id) */
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

    /**
     * 마이페이지의 기본 정보(기관명·로고)를 함께 교체한다. 로고가 null이면 지운다.
     * 로고는 이미지 식별자와 주소를 함께 두어 이미지를 바꾸거나 지울 때 이전 이미지를 정리할 수 있게 한다.
     */
    fun replaceBasicInfo(organizationName: String, logoImageId: String?, logoUrl: String?) {
        validateBasicInfo(organizationName, logoImageId, logoUrl)

        this.organizationName = organizationName
        this.logoImageId = logoImageId
        this.logoUrl = logoUrl
    }

    /** 마이페이지의 담당자 정보(이름·연락처·수신 이메일)를 함께 교체한다. 선택 값이 null이면 비운다. */
    fun replaceManagerInfo(managerName: String, managerPhone: String?, notificationEmail: String?) {
        validateManagerInfo(managerName, managerPhone, notificationEmail)

        this.managerName = managerName
        this.managerPhone = managerPhone
        this.notificationEmail = notificationEmail
    }
}

private fun validateBasicInfo(organizationName: String, logoImageId: String?, logoUrl: String?) {
    require(organizationName.isNotBlank()) { "기관명은 비어 있을 수 없습니다." }
    require(logoImageId == null || logoImageId.isNotBlank()) { "기업 로고 식별자는 비어 있을 수 없습니다." }
    require(logoUrl == null || logoUrl.isNotBlank()) { "기업 로고 주소는 비어 있을 수 없습니다." }
    require(logoImageId == null || logoUrl != null) { "기업 로고 식별자가 있으면 주소도 있어야 합니다." }
}

private fun validateManagerInfo(managerName: String, managerPhone: String?, notificationEmail: String?) {
    require(managerName.isNotBlank()) { "담당자 이름은 비어 있을 수 없습니다." }
    require(managerPhone == null || managerPhone.isNotBlank()) { "담당자 연락처는 비어 있을 수 없습니다." }
    require(notificationEmail == null || notificationEmail.isNotBlank()) { "정보 수신용 이메일은 비어 있을 수 없습니다." }
}
