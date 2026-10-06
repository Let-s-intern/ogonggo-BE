package com.ogonggo.userapi.enumeration.business

import com.ogonggo.core.enumeration.EnumOption
import com.ogonggo.core.enumeration.catalog.EnumOptionReader
import com.ogonggo.core.enumeration.enumOptionMapOf
import com.ogonggo.core.enumeration.enumOptionsOf
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryType
import com.ogonggo.userapi.advertisement.business.AdvertisementPromotionChannel
import org.springframework.stereotype.Service

/**
 * 업무 enum의 선택지를 enum 이름별로 제공한다.
 *
 * 공통 목록은 core의 [EnumOptionReader]가 관리하고, 여기서는 core가 볼 수 없는 사용자 API 전용 enum만 덧붙인다.
 */
@Service
class UserEnumService(
    enumOptionReader: EnumOptionReader,
) {

    private val enums: Map<String, List<EnumOption>> =
        enumOptionMapOf(enumOptionReader.readAll(), USER_API_ENUMS).let { enums ->
            check(LEGACY_KEYS.keys.none(enums::containsKey)) { "예전 키가 현재 enum 이름과 겹칩니다." }
            enums + LEGACY_KEYS.mapValues { (_, key) -> enums.getValue(key) }
        }

    fun getEnums(): Map<String, List<EnumOption>> = enums

    private companion object {
        val USER_API_ENUMS: Map<String, List<EnumOption>> = enumOptionMapOf(
            // 광고 문의
            enumOptionsOf<AdvertisementInquiryType>(),
            enumOptionsOf<AdvertisementPromotionChannel>(),
        )

        /**
         * 모집글 enum 이름에 RecruitmentPost 접두어를 붙이기 전의 키다. 예전 키 → 새 키.
         * 프런트가 새 키로 옮기는 동안 같은 선택지를 예전 키로도 내려 준다. 프런트 배포 후 서버 2차 배포에서 제거한다.
         */
        val LEGACY_KEYS: Map<String, String> = mapOf(
            "RecruitmentType" to "RecruitmentPostType",
            "RecruitmentPosition" to "RecruitmentPostPosition",
            "ProgressMethod" to "RecruitmentPostProgressMethod",
            "ContactMethod" to "RecruitmentPostContactMethod",
            "RecruitmentStatus" to "RecruitmentPostRecruitmentStatus",
            "RecruitmentApplicationProgressStatus" to "RecruitmentPostApplicationProgressStatus",
            "RecruitmentApplicationSortType" to "RecruitmentPostApplicationSortType",
        )
    }
}
