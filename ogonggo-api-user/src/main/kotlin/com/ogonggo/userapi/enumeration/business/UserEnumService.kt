package com.ogonggo.userapi.enumeration.business

import com.ogonggo.core.enumeration.EnumOption
import com.ogonggo.core.enumeration.catalog.EnumOptionReader
import org.springframework.stereotype.Service

/**
 * 업무 enum의 선택지를 enum 이름별로 제공한다.
 *
 * 공통 목록은 core의 [EnumOptionReader]가 관리한다. core가 볼 수 없는 사용자 API 전용 enum이 생기면 여기서 덧붙인다.
 * 지금은 그런 enum이 없다. 광고 문의 유형·홍보 채널은 저장하는 값이 되어 core로 옮겼다.
 */
@Service
class UserEnumService(
    enumOptionReader: EnumOptionReader,
) {

    private val enums: Map<String, List<EnumOption>> = enumOptionReader.readAll()

    fun getEnums(): Map<String, List<EnumOption>> = enums
}
