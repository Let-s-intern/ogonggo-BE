package com.ogonggo.adminapi.banner.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.advertiser.implement.AdvertiserReader
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import com.ogonggo.core.banner.domain.Banner
import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.core.banner.implement.BannerAppender
import com.ogonggo.core.banner.implement.BannerManager
import com.ogonggo.core.banner.implement.BannerReader
import com.ogonggo.core.banner.implement.dto.BannerAppendDto
import com.ogonggo.core.image.implement.ImageAssetReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 관리자 콘솔의 배너 세팅이다. 사용자 API의 공개 조회는 다른 프로세스라 여기서 그 캐시를 비울 수 없다.
 * 공개 조회 캐시는 짧은 만료 시간으로만 맞춘다.
 */
@Service
class AdminBannerService(
    private val bannerReader: BannerReader,
    private val bannerAppender: BannerAppender,
    private val bannerManager: BannerManager,
    private val advertiserReader: AdvertiserReader,
    private val advertiserContractReader: AdvertiserContractReader,
    private val imageAssetReader: ImageAssetReader,
    private val clock: Clock,
) {

    /**
     * [placement]가 있으면 그 자리의 배너 전부(꺼진 것·기간이 지난 것 포함)를 순서대로, [advertiserId]가 있으면 그 광고주의 배너만 준다.
     * [placement] 없이 [advertiserId]만 있으면 모든 자리에서 자리 선언 순서·자리 안 순서대로 준다. 둘 중 하나는 있어야 한다.
     */
    fun getBanners(placement: BannerPlacement?, advertiserId: Long?): List<AdminBannerResult> {
        val banners = when {
            placement != null -> bannerReader.readAllInPlacement(placement)
                .filter { advertiserId == null || it.advertiserId == advertiserId }
            advertiserId != null -> bannerReader.readAllByAdvertiser(advertiserId)
            else -> throw IllegalArgumentException("자리나 광고주 중 하나는 있어야 합니다.")
        }
        return toResults(banners)
    }

    fun getBanner(bannerId: Long): AdminBannerResult = toResults(listOf(bannerReader.read(bannerId))).single()

    /** 그 자리 맨 뒤에 등록하고 식별자를 돌려준다. */
    @Transactional
    fun createBanner(command: AdminBannerCreateCommand): Long {
        val placement = command.placement ?: throw placementError()
        val settings = command.toDraft().toSettings(placement, references)
        return bannerAppender.append(BannerAppendDto(placement = placement, settings = settings)).requiredId()
    }

    /** 보낸 칸만 바꾼다. 자리는 바꾸지 않고, 검증은 저장된 값과 합친 결과로 한다. */
    @Transactional
    fun updateBanner(bannerId: Long, command: AdminBannerUpdateCommand) {
        val banner = bannerReader.readForUpdate(bannerId)
        bannerManager.change(banner, command.mergeInto(banner).toSettings(banner.placement, references))
    }

    /** 지우고 그 자리에 남은 배너의 순서를 1부터 다시 매긴다. 이미 지운 배너를 다시 지워도 성공한다. */
    @Transactional
    fun deleteBanner(bannerId: Long) {
        bannerManager.delete(bannerReader.readForDelete(bannerId), LocalDateTime.now(clock))
    }

    /** 자리 안 순서를 통째로 바꾼다. [bannerIds]는 그 자리의 배너를 빠짐없이 한 번씩 담아야 한다. */
    @Transactional
    fun replaceOrder(placement: BannerPlacement?, bannerIds: List<Long?>?) {
        val targetPlacement = placement ?: throw placementError()
        val banners = bannerReader.readAllInPlacementForUpdate(targetPlacement).associateBy { it.requiredId() }
        val ids = bannerIds.orEmpty()
        if (ids.size != banners.size || ids.toSet().size != ids.size || !banners.keys.containsAll(ids.filterNotNull())) {
            throw AdminConsoleBadRequestException.field("bannerIds", "그 자리의 배너 id 를 빠짐없이 한 번씩 보내 주세요.")
        }
        bannerManager.reorder(ids.map { banners.getValue(checkNotNull(it)) })
    }

    private fun toResults(banners: List<Banner>): List<AdminBannerResult> {
        val advertisers = advertiserReader.readAllByIds(banners.mapNotNull { it.advertiserId })
        val contracts = advertiserContractReader.readAllByIds(banners.mapNotNull { it.contractId })
        return banners.map { AdminBannerResult.of(it, it.advertiserId?.let(advertisers::get), it.contractId?.let(contracts::get)) }
    }

    private val references = object : AdminBannerReferences {
        override fun isUploadedImage(url: String): Boolean = imageAssetReader.isAvailableUrl(url)

        override fun advertiserExists(advertiserId: Long): Boolean = advertiserReader.readAllByIds(listOf(advertiserId)).isNotEmpty()

        override fun contractAdvertiserId(contractId: Long): Long? =
            advertiserContractReader.readAllByIds(listOf(contractId))[contractId]?.advertiserId
    }

    companion object {
        /** 자리를 빠뜨렸거나 모르는 값일 때의 화면 문구다. 목록 조회의 쿼리 파라미터도 같은 문구를 쓴다. */
        fun placementError(): AdminConsoleBadRequestException = AdminConsoleBadRequestException.field(
            "placement",
            "${BannerPlacement.entries.joinToString(", ") { it.name }} 중 하나를 보내 주세요.",
        )
    }
}
