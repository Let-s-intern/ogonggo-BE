package com.ogonggo.userapi.enumeration.business

import com.ogonggo.core.enumeration.EnumOption
import com.ogonggo.core.enumeration.catalog.EnumOptionReader
import com.ogonggo.core.enumeration.enumOptionMapOf
import com.ogonggo.core.enumeration.enumOptionsOf
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryType
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryPromotionChannel
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

    private val enums: Map<String, List<EnumOption>> = enumOptionMapOf(enumOptionReader.readAll(), USER_API_ENUMS)

    fun getEnums(): Map<String, List<EnumOption>> = enums

    private companion object {
        val USER_API_ENUMS: Map<String, List<EnumOption>> = enumOptionMapOf(
            // 광고 문의
            enumOptionsOf<AdvertisementInquiryType>(),
            enumOptionsOf<AdvertisementInquiryPromotionChannel>(),
        )
    }
}
