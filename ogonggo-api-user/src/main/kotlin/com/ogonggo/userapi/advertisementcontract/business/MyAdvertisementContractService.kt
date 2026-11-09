package com.ogonggo.userapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSearchCondition
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 로그인한 회원이 자기가 낸 광고 신청을 본다. 광고 신청 폼으로 낸 신청만 보이고,
 * 비즈니스 공고 등록으로 생긴 계약은 공고 관리에서 다루므로 넣지 않는다.
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
                source = AdvertisementContractSource.APPLICATION_FORM,
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
