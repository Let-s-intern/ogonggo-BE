package com.ogonggo.adminapi.enumeration.business

import com.ogonggo.adminapi.content.business.AdminContentSortType
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.enumeration.EnumOption
import com.ogonggo.core.enumeration.catalog.EnumOptionReader
import com.ogonggo.core.enumeration.enumOptionMapOf
import com.ogonggo.core.enumeration.enumOptionsOf
import org.springframework.stereotype.Service

/**
 * 업무 enum의 선택지를 enum 이름별로 제공한다.
 *
 * 공통 목록은 core의 [EnumOptionReader]가 관리하고, 여기서는 core가 볼 수 없는 관리자 API 전용 enum만 덧붙인다.
 */
@Service
class AdminEnumService(
    enumOptionReader: EnumOptionReader,
) {

    private val enums: Map<String, List<EnumOption>> = enumOptionMapOf(enumOptionReader.readAll(), ADMIN_API_ENUMS)

    fun getEnums(): Map<String, List<EnumOption>> = enums

    private companion object {
        val ADMIN_API_ENUMS: Map<String, List<EnumOption>> = enumOptionMapOf(
            // 채용공고·부트캠프·모집글·공지 관리 목록 공통
            enumOptionsOf<AdminContentSortType>(),
            enumOptionsOf<AdminContentVisibility>(),
        )
    }
}
