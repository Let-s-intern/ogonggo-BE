package com.ogonggo.core.advertisementcontract.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractCompanyPostingAppendDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractFormAppendDto
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractJpaRepository
import org.springframework.stereotype.Component

@Component
class AdvertisementContractAppender internal constructor(
    private val advertisementContractRepository: AdvertisementContractJpaRepository,
) {

    /** 광고 신청 폼으로 들어온 계약을 신청 상태로 저장한다. */
    fun appendFromForm(dto: AdvertisementContractFormAppendDto): Long = save(
        AdvertisementContract.requestByForm(
            userId = dto.userId,
            promotionTarget = dto.promotionTarget,
            companyName = dto.companyName,
            recruitmentTitle = dto.recruitmentTitle,
            recruitmentUrl = dto.recruitmentUrl,
            recruitmentDeadline = dto.recruitmentDeadline,
            purpose = dto.purpose,
            budgetRange = dto.budgetRange,
            categories = dto.categories,
            scheduleRequest = dto.scheduleRequest,
            advertisementRequest = dto.advertisementRequest,
            managerName = dto.managerName,
            email = dto.email,
            phoneNumber = dto.phoneNumber,
            invoiceMethod = dto.invoiceMethod,
            creativeMethod = dto.creativeMethod,
        ),
    )

    /** 비즈니스로 등록한 공고 하나에 계약 하나를 만든다. 공고 등록과 같은 트랜잭션에서 부른다. */
    fun appendForCompanyPosting(dto: AdvertisementContractCompanyPostingAppendDto): Long = save(
        AdvertisementContract.ofCompanyPosting(
            userId = dto.userId,
            promotionTarget = dto.promotionTarget,
            targetId = dto.targetId,
            companyName = dto.companyName,
            recruitmentTitle = dto.recruitmentTitle,
            recruitmentDeadline = dto.recruitmentDeadline,
        ),
    )

    private fun save(contract: AdvertisementContract): Long =
        checkNotNull(advertisementContractRepository.save(contract).id) { "광고 계약 식별자가 없습니다." }
}
