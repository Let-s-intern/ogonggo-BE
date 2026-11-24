package com.ogonggo.core.featureflag.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 화면이나 기능을 코드 배포 없이 켜고 끄는 기능 플래그 하나다. 플래그 하나가 행 하나이며, 플래그가 늘어도 칼럼이나 API는 늘지 않는다.
 *
 * 코드가 [flagKey] 문자열로 플래그를 찾는다. 새 플래그는 그 플래그를 쓰는 기능의 배포 SQL로 행을 넣고,
 * 이 엔티티를 만들거나 지우는 API는 없다. 운영자는 켜짐 여부만 바꾼다.
 */
@Entity
@Table(
    name = "feature_flags",
    uniqueConstraints = [UniqueConstraint(name = "uk_feature_flags_flag_key", columnNames = ["flag_key"])],
)
class FeatureFlag internal constructor(
    flagKey: String,
    enabled: Boolean,
    description: String,
) : BaseTimeEntity() {

    init {
        require(FLAG_KEY_PATTERN.matches(flagKey) && flagKey.length <= MAX_FLAG_KEY_LENGTH) {
            "플래그 key는 소문자 snake_case이고 ${MAX_FLAG_KEY_LENGTH}자 이하여야 합니다."
        }
        require(description.isNotBlank() && description.length <= MAX_DESCRIPTION_LENGTH) {
            "플래그 설명은 비어 있지 않고 ${MAX_DESCRIPTION_LENGTH}자 이하여야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 기능 플래그 식별자 */
        protected set

    @Column(name = "flag_key", nullable = false, length = MAX_FLAG_KEY_LENGTH)
    val flagKey: String = flagKey /* 코드가 플래그를 찾는 소문자 snake_case 문자열 */

    @Column(nullable = false)
    var enabled: Boolean = enabled /* 켜져 있으면 true */
        protected set

    @Column(nullable = false, length = MAX_DESCRIPTION_LENGTH)
    val description: String = description /* 이 플래그가 무엇을 켜고 끄는지 */

    @Column(name = "updated_by")
    var updatedBy: Long? = null /* 켜짐 여부를 마지막으로 바꾼 관리자의 회원 식별자. 바꾼 적이 없으면 null */
        protected set

    /** 켜짐 여부를 바꾸고 바꾼 관리자를 남긴다. 지금과 같은 값이어도 마지막으로 확인한 관리자로 남는다. */
    fun changeEnabled(enabled: Boolean, updatedBy: Long) {
        require(updatedBy > 0) { "관리자 식별자는 양수여야 합니다." }
        this.enabled = enabled
        this.updatedBy = updatedBy
    }

    companion object {
        const val MAX_FLAG_KEY_LENGTH = 100
        const val MAX_DESCRIPTION_LENGTH = 500
        private val FLAG_KEY_PATTERN = Regex("^[a-z][a-z0-9]*(_[a-z0-9]+)*$")
    }
}
