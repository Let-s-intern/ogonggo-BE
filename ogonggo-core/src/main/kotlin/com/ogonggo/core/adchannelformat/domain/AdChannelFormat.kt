package com.ogonggo.core.adchannelformat.domain

import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import java.time.LocalDateTime

/**
 * 채널에 광고를 올리는 방식(피드 게시물, 릴스, 오픈채팅 공지 등)이다. 플랫폼마다 운영자가 정하고 순서를 바꾼다.
 *
 * 플랫폼은 등록한 뒤 바꾸지 않는다. 순서(`displayOrder`)는 플랫폼 안에서 1부터다. 올릴 수 있는 채널(`channelIds`)은 같은
 * 플랫폼의 채널이고, 비어 있으면 그 플랫폼의 모든 채널(나중에 더한 채널 포함)이다. 기본 단가는 안내용이며 계약 금액과 상관없다.
 * 채널 광고가 잡힌 유형은 지울 수 없다.
 */
@Entity
@Table(
    name = "ad_channel_formats",
    indexes = [Index(name = "idx_ad_channel_formats_platform", columnList = "platform, deleted_at, display_order")],
)
class AdChannelFormat internal constructor(
    platform: AdChannelPlatform,
    terms: AdChannelFormatTerms,
    displayOrder: Int,
) : BaseTimeEntity() {

    init {
        require(displayOrder > 0) { "순서는 1부터다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 게재 유형 식별자 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val platform: AdChannelPlatform = platform /* 플랫폼 */

    @Column(nullable = false, length = AdChannelFormatTerms.MAX_NAME_LENGTH)
    var name: String = terms.name /* 이름. 같은 플랫폼 안에서 겹치지 않는다 */
        protected set

    @Column(length = AdChannelFormatTerms.MAX_DESCRIPTION_LENGTH)
    var description: String? = terms.description /* 설명 */
        protected set

    @Column(nullable = false)
    var enabled: Boolean = terms.enabled /* 새 채널 광고·계약 상품에서 고를 수 있는가 */
        protected set

    @Column(name = "default_price")
    var defaultPrice: Long? = terms.defaultPrice /* 기본 단가(원). 안내용 */
        protected set

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "ad_channel_format_channels", joinColumns = [JoinColumn(name = "format_id")])
    @Column(name = "channel_id", nullable = false)
    var channelIds: MutableList<Long> = terms.channelIds.toMutableList()
        get() = field.sorted().toMutableList()
        protected set

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = displayOrder /* 플랫폼 안 순서. 1부터 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 삭제 일시 */
        protected set

    val terms: AdChannelFormatTerms
        get() = AdChannelFormatTerms(
            name = name,
            description = description,
            enabled = enabled,
            defaultPrice = defaultPrice,
            channelIds = channelIds,
        )

    /** 이 유형으로 [channelId] 채널에 올릴 수 있는가. 올릴 수 있는 채널이 비어 있으면 그 플랫폼의 모든 채널이다. */
    fun allows(channelId: Long): Boolean = channelIds.isEmpty() || channelId in channelIds

    /** 칸을 통째로 바꾼다. 부분 수정은 저장된 값과 합친 [terms]를 넘긴다. */
    fun change(terms: AdChannelFormatTerms) {
        check(deletedAt == null) { "삭제된 게재 유형은 수정할 수 없습니다." }
        name = terms.name
        description = terms.description
        enabled = terms.enabled
        defaultPrice = terms.defaultPrice
        if (channelIds != terms.channelIds) {
            channelIds = terms.channelIds.toMutableList()
        }
    }

    fun reorder(displayOrder: Int) {
        require(displayOrder > 0) { "순서는 1부터다." }
        if (this.displayOrder != displayOrder) {
            this.displayOrder = displayOrder
        }
    }

    /** 지운 채널을 올릴 수 있는 채널에서 뺀다. 없으면 그대로 둔다. */
    fun removeChannel(channelId: Long) {
        if (channelId in channelIds) {
            channelIds = channelIds.filter { it != channelId }.toMutableList()
        }
    }

    /** 삭제는 반복해도 최초 삭제 일시를 유지한다. */
    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }
}

/** 게재 유형 칸이다. 이름·설명은 앞뒤 공백을 빼고 설명은 비면 null로, 올릴 수 있는 채널은 겹치지 않게 등록 순(식별자 순)으로 둔다. */
class AdChannelFormatTerms(
    name: String,
    description: String?,
    val enabled: Boolean,
    val defaultPrice: Long?,
    channelIds: Collection<Long>,
) {
    val name: String = name.trim()
    val description: String? = description?.trim()?.ifEmpty { null }
    val channelIds: List<Long> = channelIds.distinct().sorted()

    init {
        require(this.name.isNotEmpty() && this.name.length <= MAX_NAME_LENGTH) { "게재 유형 이름은 1~${MAX_NAME_LENGTH}자여야 합니다." }
        require((this.description?.length ?: 0) <= MAX_DESCRIPTION_LENGTH) { "설명은 ${MAX_DESCRIPTION_LENGTH}자 이하여야 합니다." }
        require(defaultPrice == null || defaultPrice in 0..MAX_PRICE) { "기본 단가는 0 이상 ${MAX_PRICE}원 이하여야 합니다." }
        require(this.channelIds.all { it > 0 }) { "채널 식별자는 양수여야 합니다." }
    }

    companion object {
        const val MAX_NAME_LENGTH = 50
        const val MAX_DESCRIPTION_LENGTH = 500
        const val MAX_PRICE = 9_999_999_999L
    }
}
