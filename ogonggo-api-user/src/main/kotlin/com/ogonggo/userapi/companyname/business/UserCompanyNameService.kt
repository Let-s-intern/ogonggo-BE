package com.ogonggo.userapi.companyname.business

import com.ogonggo.core.companyname.implement.CompanyNameReader
import org.springframework.stereotype.Service

/** 광고 문의 폼의 회사명 칸이 오공고에 이미 있는 이름을 고르게 권하는 검색이다. */
@Service
class UserCompanyNameService(
    private val companyNameReader: CompanyNameReader,
) {

    /** 검색어가 없거나 비면 빈 목록이다. */
    fun searchCompanyNames(keyword: String?): List<String> =
        keyword?.let(companyNameReader::search) ?: emptyList()
}
