package com.ogonggo.core.user.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
    businessNumberType: CompanyBusinessNumberType? = null,
    businessNumber: String? = null,
    companyType: CompanyType? = null,
    employeeCountRange: CompanyEmployeeCountRange? = null,
    industry: String? = null,
    address: String? = null,
    homepageUrl: String? = null,
    managerTitle: String? = null,
    managerDuty: CompanyManagerDuty? = null,
    recommender: String? = null,
) : BaseTimeEntity() {

    init {
        require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
        validateBasicInfo(organizationName, logoImageId, logoUrl)
        validateManagerInfo(managerName, managerPhone, notificationEmail)
        validateBusinessNumber(businessNumberType, businessNumber)
        requireNotBlankIfPresent(industry, "산업")
        requireNotBlankIfPresent(address, "주소")
        requireNotBlankIfPresent(homepageUrl, "홈페이지")
        requireNotBlankIfPresent(managerTitle, "담당자 직책")
        requireNotBlankIfPresent(recommender, "추천인")
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

    // 아래는 가입할 때 받는 기업·담당자 정보다. 이 칼럼을 더하기 전에 가입한 회원은 모두 null이다.

    @Enumerated(EnumType.STRING)
    @Column(name = "business_number_type", length = 30)
    var businessNumberType: CompanyBusinessNumberType? = businessNumberType /* 번호 종류. 해외 기업이면 OVERSEAS */
        protected set

    @Column(name = "business_number", length = 13)
    var businessNumber: String? = businessNumber /* 숫자만. 한 회사에 담당자 계정이 여럿일 수 있어 유니크가 아니다 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", length = 30)
    var companyType: CompanyType? = companyType /* 기업 형태 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_count_range", length = 30)
    var employeeCountRange: CompanyEmployeeCountRange? = employeeCountRange /* 직원 수 구간 */
        protected set

    @Column(name = "industry", length = 100)
    var industry: String? = industry /* 산업. 공고의 industry와 같은 문자열 */
        protected set

    @Column(name = "address", length = 255)
    var address: String? = address /* 주소 */
        protected set

    @Column(name = "homepage_url", length = 2048)
    var homepageUrl: String? = homepageUrl /* 홈페이지 */
        protected set

    @Column(name = "manager_title", length = 50)
    var managerTitle: String? = managerTitle /* 담당자 직책 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "manager_duty", length = 30)
    var managerDuty: CompanyManagerDuty? = managerDuty /* 담당 업무 */
        protected set

    @Column(name = "recommender", length = 100)
    var recommender: String? = recommender /* 추천인 */
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

    /**
     * 가입 때 받은 기업 정보 중 보낸 값만 바꾼다. null이면 저장된 값을 그대로 둔다.
     * 이 값을 모르는 이전 화면이 기본 정보를 저장해도 지워지지 않게 하려는 것이다.
     * 번호 종류를 보내면 번호도 함께 바꾸며, 해외 기업이면 번호를 비운다.
     */
    fun updateBusinessDetails(
        businessNumberType: CompanyBusinessNumberType?,
        businessNumber: String?,
        companyType: CompanyType?,
        employeeCountRange: CompanyEmployeeCountRange?,
        industry: String?,
        address: String?,
        homepageUrl: String?,
    ) {
        if (businessNumberType == null) {
            require(businessNumber == null) { "번호 종류 없이 사업자 번호를 바꿀 수 없습니다." }
        } else {
            validateBusinessNumber(businessNumberType, businessNumber)
        }
        requireNotBlankIfPresent(industry, "산업")
        requireNotBlankIfPresent(address, "주소")
        requireNotBlankIfPresent(homepageUrl, "홈페이지")

        if (businessNumberType != null) {
            this.businessNumberType = businessNumberType
            this.businessNumber = businessNumber
        }
        companyType?.let { this.companyType = it }
        employeeCountRange?.let { this.employeeCountRange = it }
        industry?.let { this.industry = it }
        address?.let { this.address = it }
        homepageUrl?.let { this.homepageUrl = it }
    }

    /** 가입 때 받은 담당자 정보 중 보낸 값만 바꾼다. null이면 저장된 값을 그대로 둔다. */
    fun updateManagerDetails(managerTitle: String?, managerDuty: CompanyManagerDuty?, recommender: String?) {
        requireNotBlankIfPresent(managerTitle, "담당자 직책")
        requireNotBlankIfPresent(recommender, "추천인")

        managerTitle?.let { this.managerTitle = it }
        managerDuty?.let { this.managerDuty = it }
        recommender?.let { this.recommender = it }
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

/** 번호는 종류가 있어야 뜻이 있고, 종류가 정해지면 그 종류의 형식을 따라야 한다. */
private fun validateBusinessNumber(type: CompanyBusinessNumberType?, number: String?) {
    if (type == null) {
        require(number == null) { "번호 종류 없이 사업자 번호를 둘 수 없습니다." }
        return
    }
    require(type.accepts(number)) { "${type.desc}의 형식이 아닙니다." }
}

private fun requireNotBlankIfPresent(value: String?, label: String) {
    require(value == null || value.isNotBlank()) { "${label}은(는) 비어 있을 수 없습니다." }
}
