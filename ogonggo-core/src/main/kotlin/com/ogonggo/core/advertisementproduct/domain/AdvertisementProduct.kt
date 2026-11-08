package com.ogonggo.core.advertisementproduct.domain

import com.ogonggo.core.advertisementproduct.error.AdvertisementProductErrorCode
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 오공고가 파는 광고 상품이다. 어드민이 추가하고 지운다.
 *
 * 화면에 노출되는 상품은 하나가 노출 자리 하나다([position]). 같은 배너 카테고리라도 채용공고 홈 상단과 하단은 다른 상품이다.
 * 가격은 상품 소개용이고 실제 결제 금액은 계약([com.ogonggo.core.advertisementcontract.domain.AdvertisementContract.paymentAmount])에 남는다.
 * 그래서 가격을 바꿔도 이력을 남기지 않는다. 설명·단위·태그 같은 화면 문구는 프론트가 가진다.
 */
@Entity
@Table(name = "advertisement_products")
class AdvertisementProduct internal constructor(
    category: AdvertisementProductCategory,
    position: AdvertisementProductPosition?,
    name: String,
    price: Int?,
) : BaseTimeEntity() {

    init {
        validate(category, position, name, price)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 광고 상품 식별자 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: AdvertisementProductCategory = category /* 카테고리 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    var position: AdvertisementProductPosition? = position /* 노출 자리. 외부 채널·자리 미정 상품은 null */
        protected set

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    var name: String = name /* 상품 이름 */
        protected set

    @Column
    var price: Int? = price /* 소개용 가격. null은 견적 문의 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 삭제 일시 */
        protected set

    fun update(category: AdvertisementProductCategory, position: AdvertisementProductPosition?, name: String, price: Int?) {
        checkNotDeleted()
        validate(category, position, name, price)
        this.category = category
        this.position = position
        this.name = name
        this.price = price
    }

    /** 이미 맺은 계약과 노출은 이 상품을 계속 가리킨다. 새로 붙이지 못하게만 한다. */
    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }

    fun checkNotDeleted() {
        check(deletedAt == null) { "삭제된 광고 상품입니다." }
    }

    companion object {
        const val MAX_NAME_LENGTH = 100

        private fun validate(
            category: AdvertisementProductCategory,
            position: AdvertisementProductPosition?,
            name: String,
            price: Int?,
        ) {
            require(name.isNotBlank() && name.length <= MAX_NAME_LENGTH) { "상품 이름은 1~${MAX_NAME_LENGTH}자여야 합니다." }
            require(price == null || price >= 0) { "가격은 0 이상이어야 합니다." }
            if (position != null && position.category != category) {
                throw InvalidValueException(AdvertisementProductErrorCode.ADVERTISEMENT_PRODUCT_POSITION_MISMATCH)
            }
        }
    }
}
