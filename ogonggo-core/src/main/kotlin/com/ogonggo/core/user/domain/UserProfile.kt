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

    /**
     * 렛츠커리어에서 복제하는 값만 갱신한다.
     * 학력과 희망 조건은 오공고가 소유하므로 여기서 건드리지 않는다.
     */
    fun sync(
        name: String?,
        email: String?,
        nickname: String?,
        profileImageUrl: String?,
        letsCareerUpdatedAt: LocalDateTime?,
        syncedAt: LocalDateTime,
    ) {
        this.name = name
        this.email = email
        this.nickname = nickname
        this.profileImageUrl = profileImageUrl
        this.letsCareerUpdatedAt = letsCareerUpdatedAt
        this.lastSyncedAt = syncedAt
    }

    /**
     * 사용자가 오공고에서 직접 입력하는 학력과 희망 조건을 함께 교체한다.
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
    ) {
        this.university = university
        this.major = major
        this.grade = grade
        this.wishField = wishField
        this.wishJob = wishJob
        this.wishIndustry = wishIndustry
        this.wishEmploymentType = wishEmploymentType
        this.wishCompany = wishCompany
    }
}
