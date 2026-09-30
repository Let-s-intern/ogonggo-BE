package com.ogonggo.core.user.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user_profiles")
internal class UserProfile(
    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long, /* 프로필 소유 사용자 식별자 */

    name: String? = null,
    email: String? = null,
    phoneNum: String? = null,
    letsCareerAuthProvider: LetsCareerAuthProvider? = null,
    nickname: String? = null,
    profileImageUrl: String? = null,
    letsCareerUpdatedAt: LocalDateTime? = null,
    lastSyncedAt: LocalDateTime,
) : BaseTimeEntity() {

    init {
        require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 사용자 프로필 식별자 */
        protected set

    @Column(length = 100)
    var name: String? = name /* 사용자 이름 */
        protected set

    @Column(length = 255)
    var email: String? = email /* 사용자 이메일 */
        protected set

    @Column(name = "phone_num", length = 30)
    var phoneNum: String? = phoneNum /* 휴대폰 번호 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "letscareer_auth_provider", length = 20)
    var letsCareerAuthProvider: LetsCareerAuthProvider? = letsCareerAuthProvider /* 렛츠커리어 가입 경로 */
        protected set

    @Column(length = 100)
    var nickname: String? = nickname /* 사용자 닉네임 */
        protected set

    @Column(name = "profile_image_url", length = 2048)
    var profileImageUrl: String? = profileImageUrl /* 프로필 이미지 URL */
        protected set

    @Column(name = "letscareer_updated_at")
    var letsCareerUpdatedAt: LocalDateTime? = letsCareerUpdatedAt /* 렛츠커리어 프로필 최종 수정 일시 */
        protected set

    @Column(name = "last_synced_at", nullable = false)
    var lastSyncedAt: LocalDateTime = lastSyncedAt /* 렛츠커리어 프로필 최종 동기화 일시 */
        protected set

    @Column(name = "ogonggo_profile_image_id", length = 36)
    var ogonggoProfileImageId: String? = null /* 오공고에서 올린 프로필 이미지 식별자(image_assets.id) */
        protected set

    @Column(name = "ogonggo_profile_image_url", length = 2048)
    var ogonggoProfileImageUrl: String? = null /* 오공고에서 올린 프로필 이미지 URL. 있으면 렛츠커리어 이미지 대신 보인다 */
        protected set

    @Column(name = "notification_email", length = 320)
    var notificationEmail: String? = null /* 오늘의 공고 정보 수신용 이메일 */
        protected set

    @Column(length = 30)
    var university: String? = null /* 대학교 */
        protected set

    @Column(length = 30)
    var major: String? = null /* 전공 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var grade: UserGrade? = null /* 학년 */
        protected set

    @Column(name = "wish_field", columnDefinition = "TEXT")
    var wishField: String? = null /* 희망 직군 */
        protected set

    @Column(name = "wish_job", columnDefinition = "TEXT")
    var wishJob: String? = null /* 희망 직무 */
        protected set

    @Column(name = "wish_industry", columnDefinition = "TEXT")
    var wishIndustry: String? = null /* 희망 산업 */
        protected set

    @Column(name = "wish_employment_type", columnDefinition = "TEXT")
    var wishEmploymentType: String? = null /* 희망 구직 조건 */
        protected set

    @Column(name = "wish_company", columnDefinition = "TEXT")
    var wishCompany: String? = null /* 희망 기업 */
        protected set

    @Column(name = "job_info_updated_at")
    var jobInfoUpdatedAt: LocalDateTime? = null /* 학력·희망 조건 최종 수정 일시. 렛츠커리어와 동기화할 때 나중에 고친 쪽을 가리는 기준이다 */
        protected set

    /**
     * 렛츠커리어에서 복제하는 값만 갱신한다.
     * 학력과 희망 조건, 수신 이메일, 오공고에서 올린 프로필 이미지는 오공고가 소유하므로 여기서 건드리지 않는다.
     */
    fun sync(
        name: String?,
        email: String?,
        phoneNum: String?,
        letsCareerAuthProvider: LetsCareerAuthProvider?,
        nickname: String?,
        profileImageUrl: String?,
        letsCareerUpdatedAt: LocalDateTime?,
        syncedAt: LocalDateTime,
    ) {
        this.name = name
        this.email = email
        this.phoneNum = phoneNum
        this.letsCareerAuthProvider = letsCareerAuthProvider
        this.nickname = nickname
        this.profileImageUrl = profileImageUrl
        this.letsCareerUpdatedAt = letsCareerUpdatedAt
        this.lastSyncedAt = syncedAt
    }

    /**
     * 학력과 희망 조건을 함께 교체하고 고친 일시를 남긴다.
     * 보내지 않은 값은 비우는 것으로 보므로 일부만 바꾸는 용도로 쓰지 않는다.
     */
    fun replaceJobInfo(
        university: String?,
        major: String?,
        grade: UserGrade?,
        wishField: String?,
        wishJob: String?,
        wishIndustry: String?,
        wishEmploymentType: String?,
        wishCompany: String?,
        updatedAt: LocalDateTime?,
    ) {
        this.jobInfoUpdatedAt = updatedAt
        this.university = university
        this.major = major
        this.grade = grade
        this.wishField = wishField
        this.wishJob = wishJob
        this.wishIndustry = wishIndustry
        this.wishEmploymentType = wishEmploymentType
        this.wishCompany = wishCompany
    }

    /**
     * 오늘의 공고를 받을 이메일을 바꾼다. 렛츠커리어 가입 이메일과 따로 두며 오공고가 소유한다.
     * null이면 비운다.
     */
    fun changeNotificationEmail(notificationEmail: String?) {
        require(notificationEmail == null || notificationEmail.isNotBlank()) { "수신 이메일은 공백일 수 없습니다." }
        this.notificationEmail = notificationEmail
    }

    /**
     * 오공고에서 올린 이미지를 프로필 이미지로 쓴다. 오공고가 소유하며 렛츠커리어에 보내지 않는다.
     * 렛츠커리어에서 복제하는 `profileImageUrl`과 칸을 나눠 두어 재로그인의 `sync`가 덮어쓰지 않는다.
     */
    fun changeOgonggoProfileImage(imageId: String, url: String) {
        require(imageId.isNotBlank()) { "프로필 이미지 식별자는 비어 있을 수 없습니다." }
        require(url.isNotBlank()) { "프로필 이미지 URL은 비어 있을 수 없습니다." }
        this.ogonggoProfileImageId = imageId
        this.ogonggoProfileImageUrl = url
    }

    /** 오공고에서 올린 프로필 이미지를 지운다. 그러면 다시 렛츠커리어 이미지가 보인다. */
    fun removeOgonggoProfileImage() {
        this.ogonggoProfileImageId = null
        this.ogonggoProfileImageUrl = null
    }

    /**
     * 렛츠커리어가 보낸 값이 더 나중에 고친 것일 때만 받아들인다.
     * 같은 일시면 이미 받은 수정이다. 렛츠커리어가 일시를 모르면(한 번도 고친 적 없는 과거 계정) 이쪽도 모를 때만 받는다.
     */
    fun acceptsLetsCareerJobInfo(letsCareerUpdatedAt: LocalDateTime?): Boolean {
        val current = jobInfoUpdatedAt ?: return true
        return letsCareerUpdatedAt != null && letsCareerUpdatedAt.isAfter(current)
    }
}
