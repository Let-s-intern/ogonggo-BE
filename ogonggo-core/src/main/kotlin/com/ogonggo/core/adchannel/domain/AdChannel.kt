package com.ogonggo.core.adchannel.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 채널 광고를 올리는 우리 채널이다. 인스타그램 계정이나 카카오톡 오픈채팅방 하나다.
 *
 * 플랫폼은 등록한 뒤 바꾸지 않는다. 인스타그램만 계정 이름(`handle`, `@` 없이)을 가지고, 계정 이름은 대소문자를 무시하고
 * 겹치지 않는다(관리자 API가 지운 채널을 빼고 본다). 끄면 새 채널 광고에서 고를 수 없지만 이미 잡힌 광고는 그대로다.
 * 채널 광고가 잡힌 채널은 지울 수 없다.
 */
@Entity
@Table(
    name = "ad_channels",
    indexes = [Index(name = "idx_ad_channels_deleted_at", columnList = "deleted_at")],
)
class AdChannel internal constructor(
    platform: AdChannelPlatform,
    profile: AdChannelProfile,
) : BaseTimeEntity() {

    init {
        validate(platform, profile)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 채널 식별자 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val platform: AdChannelPlatform = platform /* 플랫폼 */

    @Column(nullable = false, length = AdChannelProfile.MAX_NAME_LENGTH)
    var name: String = profile.name /* 표시 이름. 오픈채팅은 방 이름 */
        protected set

    @Column(length = AdChannelProfile.MAX_HANDLE_LENGTH)
    var handle: String? = profile.handle.takeIf { platform == AdChannelPlatform.INSTAGRAM } /* 인스타그램 계정 이름(@ 없이) */
        protected set

    @Column(nullable = false, length = AdChannelProfile.MAX_URL_LENGTH)
    var url: String = profile.url /* 프로필 주소, 오픈채팅방 참여 주소 */
        protected set

    @Column(name = "profile_image_url", length = AdChannelProfile.MAX_URL_LENGTH)
    var profileImageUrl: String? = profile.profileImageUrl /* 프로필 이미지 주소나 웹 경로 */
        protected set

    @Column(name = "member_count")
    var memberCount: Int? = profile.memberCount /* 팔로워·참여 인원. 안내용 */
        protected set

    @Column(nullable = false)
    var enabled: Boolean = profile.enabled /* 새 채널 광고에서 고를 수 있는가 */
        protected set

    @Column(length = AdChannelProfile.MAX_MEMO_LENGTH)
    var memo: String? = profile.memo /* 운영자 메모 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 삭제 일시 */
        protected set

    /** 이 채널을 화면에 가리키는 이름이다. 인스타그램은 `@계정`, 오픈채팅은 방 이름. */
    val label: String
        get() = handle?.let { "@$it" } ?: name

    val profile: AdChannelProfile
        get() = AdChannelProfile(
            name = name,
            handle = handle,
            url = url,
            profileImageUrl = profileImageUrl,
            memberCount = memberCount,
            enabled = enabled,
            memo = memo,
        )

    /** 칸을 통째로 바꾼다. 부분 수정은 저장된 값과 합친 [profile]을 넘긴다. */
    fun change(profile: AdChannelProfile) {
        check(deletedAt == null) { "삭제된 채널은 수정할 수 없습니다." }
        validate(platform, profile)
        name = profile.name
        handle = profile.handle.takeIf { platform == AdChannelPlatform.INSTAGRAM }
        url = profile.url
        profileImageUrl = profile.profileImageUrl
        memberCount = profile.memberCount
        enabled = profile.enabled
        memo = profile.memo
    }

    /** 삭제는 반복해도 최초 삭제 일시를 유지한다. */
    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }

    private fun validate(platform: AdChannelPlatform, profile: AdChannelProfile) {
        if (platform == AdChannelPlatform.INSTAGRAM) {
            require(profile.handle != null) { "인스타그램 채널은 계정 이름이 있어야 합니다." }
        }
    }
}

/**
 * 채널 칸이다. 이름·주소는 앞뒤 공백을 빼고, 계정 이름은 앞의 `@`를 떼며, 프로필 이미지·메모는 비면 null로 둔다.
 * 계정 이름은 인스타그램 채널만 저장한다.
 */
class AdChannelProfile(
    name: String,
    handle: String?,
    url: String,
    profileImageUrl: String?,
    val memberCount: Int?,
    val enabled: Boolean,
    memo: String?,
) {
    val name: String = name.trim()
    val handle: String? = handle?.let(::normalizeHandle)?.ifEmpty { null }
    val url: String = url.trim()
    val profileImageUrl: String? = profileImageUrl?.trim()?.ifEmpty { null }
    val memo: String? = memo?.trim()?.ifEmpty { null }

    init {
        require(this.name.isNotEmpty() && this.name.length <= MAX_NAME_LENGTH) { "채널 이름은 1~${MAX_NAME_LENGTH}자여야 합니다." }
        require(this.handle == null || HANDLE.matches(this.handle)) { "계정 이름은 영문·숫자·마침표·밑줄 ${MAX_HANDLE_LENGTH}자까지다." }
        require(URL.matches(this.url) && this.url.length <= MAX_URL_LENGTH) { "채널 주소는 http(s) 주소여야 합니다." }
        require(this.profileImageUrl == null || (IMAGE_URL.matches(this.profileImageUrl) && this.profileImageUrl.length <= MAX_URL_LENGTH)) {
            "프로필 이미지는 http(s) 주소나 / 로 시작하는 경로여야 합니다."
        }
        require(memberCount == null || memberCount in 0..MAX_MEMBER_COUNT) { "인원 수는 0 이상 ${MAX_MEMBER_COUNT} 이하여야 합니다." }
        require((this.memo?.length ?: 0) <= MAX_MEMO_LENGTH) { "메모는 ${MAX_MEMO_LENGTH}자 이하여야 합니다." }
    }

    companion object {
        const val MAX_NAME_LENGTH = 50
        const val MAX_HANDLE_LENGTH = 30
        const val MAX_URL_LENGTH = 500
        const val MAX_MEMO_LENGTH = 500
        const val MAX_MEMBER_COUNT = 99_999_999

        val HANDLE = Regex("^[A-Za-z0-9._]{1,$MAX_HANDLE_LENGTH}$")
        val URL = Regex("^https?://\\S+$")
        val IMAGE_URL = Regex("^(https?://\\S+|/\\S*)$")

        /** 앞뒤 공백과 앞의 `@` 하나를 뗀다. */
        fun normalizeHandle(handle: String): String = handle.trim().removePrefix("@")
    }
}
