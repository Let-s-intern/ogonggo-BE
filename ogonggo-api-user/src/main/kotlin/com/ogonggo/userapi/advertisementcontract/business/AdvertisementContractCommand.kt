package com.ogonggo.userapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import java.time.LocalDate

data class CreateAdvertisementContractCommand(
    val promotionTarget: AdvertisementContractPromotionTarget,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentUrl: String,
    val recruitmentDeadline: LocalDate,
    val purpose: AdvertisementContractPurpose,
    val budgetAmount: Int?,
    val categories: Set<AdvertisementProductCategory>,
    val scheduleRequest: String?,
    val advertisementRequest: String?,
    val managerName: String,
    val email: String,
    val phoneNumber: String,
    val businessNumberType: CompanyBusinessNumberType? = null,
    val businessNumber: String? = null,
    val invoiceMethod: AdvertisementContractInvoiceMethod,
    val creativeMethod: AdvertisementContractCreativeMethod,
)
