package com.ogonggo.userapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSearchCondition
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 로그인한 회원이 소유한 광고 계약을 본다. 신청 폼 계약과 비즈니스 공고 등록 계약을 함께 조회한다.
 */
@Service
class MyAdvertisementContractService(
    private val advertisementContractReader: AdvertisementContractReader,
) {

    /** 최근 신청부터 준다. 카테고리는 지연 로딩이라 페이지 조회와 일괄 초기화를 한 영속성 컨텍스트에서 한다. */
    @Transactional(readOnly = true)
    fun getContracts(
        userId: Long,
        statuses: Set<AdvertisementContractStatus>,
        page: Int,
        size: Int,
    ): MyAdvertisementContractPageResult {
        val result = advertisementContractReader.readPage(
            AdvertisementContractSearchCondition(
                userId = userId,
                statuses = statuses,
            ),
            page,
            size,
        )
        return MyAdvertisementContractPageResult.from(result)
    }

    /** 남의 신청, 비회원 신청, 비즈니스 공고 계약은 없는 계약과 같이 404다. */
    @Transactional(readOnly = true)
    fun getContract(userId: Long, contractId: Long): MyAdvertisementContractDetailResult {
        val contract = advertisementContractReader.readApplicationOf(userId, contractId)
        val products = advertisementContractReader.readProducts(contractId)
        return MyAdvertisementContractDetailResult.from(contract, products)
    }
}
