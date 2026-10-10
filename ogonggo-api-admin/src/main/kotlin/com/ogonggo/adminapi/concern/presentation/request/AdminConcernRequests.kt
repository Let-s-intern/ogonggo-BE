package com.ogonggo.adminapi.concern.presentation.request

import com.ogonggo.adminapi.concern.business.AdminConcernVisibilityChangeCommand
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.InvalidRequestFieldException
import jakarta.validation.constraints.Size

/** 채용공고·부트캠프·사이드·스터디 일괄 노출 변경과 같은 상한이다. */
private const val MAX_VISIBILITY_CHANGE_IDS = 1000

/** 사이드·스터디 일괄 노출 변경과 같은 계약이다. 같은 식별자가 여러 번 와도 거절하지 않는다. */
data class ChangeAdminConcernVisibilityRequest(
    @field:Size(min = 1, max = MAX_VISIBILITY_CHANGE_IDS) val ids: List<Long?>,
    val visibility: AdminContentVisibility,
) {
    /** 배열 요소의 제약은 Bean Validation으로 선언할 수 없어 여기서 확인한다. 요소에 null이 와도 500이 되지 않게 한다. */
    fun toCommand(): AdminConcernVisibilityChangeCommand =
        AdminConcernVisibilityChangeCommand(
            concernIds = ids.map { id ->
                id?.takeIf { it > 0 } ?: throw InvalidRequestFieldException("ids", "고민글 식별자는 양수여야 합니다.")
            },
            visibility = visibility,
        )
}
