package com.ogonggo.core.user.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
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
}
