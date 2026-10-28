package com.ogonggo.core.companyname.implement

import com.ogonggo.core.companyname.persistence.CompanyNameQueryRepository
import org.springframework.stereotype.Component

/**
 * 오공고에 있는 회사명을 찾는다. 광고 문의 폼이 같은 회사의 표기를 맞추도록 이미 있는 이름을 고르게 권하는 데 쓴다.
 *
 * 회사 테이블이 따로 없어 채용공고·부트캠프의 회사명과 기업 회원의 기관명을 합친다.
 * 앞뒤 공백만 다듬고 표기는 바꾸지 않는다. `(주)`를 지우는 식의 정규화는 하지 않는다.
 */
@Component
class CompanyNameReader internal constructor(
    private val companyNameQueryRepository: CompanyNameQueryRepository,
) {

    /**
     * 검색어를 포함하는 이름을 최대 [MAX_RESULTS]개 준다. 대소문자는 가리지 않는다.
     * 검색어로 시작하는 이름이 먼저이고, 같은 묶음 안에서는 짧은 이름이 먼저다. 검색어가 비면 빈 목록이다.
     */
    fun search(keyword: String): List<String> {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return emptyList()

        val names = companyNameQueryRepository.findJobCompanyNames(trimmed, MAX_RESULTS) +
            companyNameQueryRepository.findBootcampCompanyNames(trimmed, MAX_RESULTS) +
            companyNameQueryRepository.findCompanyProfileOrganizationNames(trimmed, MAX_RESULTS)
        val lowerKeyword = trimmed.lowercase()
        return names.distinct()
            .sortedWith(
                compareBy<String>({ if (it.lowercase().startsWith(lowerKeyword)) 0 else 1 }, { it.length }, { it }),
            )
            .take(MAX_RESULTS)
    }

    companion object {
        const val MAX_RESULTS = 10
    }
}
