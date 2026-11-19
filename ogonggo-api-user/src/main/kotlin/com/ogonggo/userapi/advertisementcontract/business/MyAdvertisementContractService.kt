package com.ogonggo.userapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSearchCondition
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 로그인한 회원이 자기 광고 계약을 본다. 광고 신청 폼으로 낸 신청과 비즈니스 공고 등록으로 생긴 계약을 모두 보이고
 * `source`로 가른다. 성과 리포트를 두 계약 모두에서 열기 때문이다(2026-11-19, LC-3647).
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

    /** 남의 계약과 비회원 신청은 없는 계약과 같이 404다. */
    @Transactional(readOnly = true)
    fun getContract(userId: Long, contractId: Long): MyAdvertisementContractDetailResult {
        val contract = advertisementContractReader.readOwnedBy(userId, contractId)
        val products = advertisementContractReader.readProducts(contractId)
        return MyAdvertisementContractDetailResult.from(contract, products)
    }
}
