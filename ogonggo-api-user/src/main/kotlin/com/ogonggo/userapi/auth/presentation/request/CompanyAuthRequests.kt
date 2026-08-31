package com.ogonggo.userapi.auth.presentation.request

import com.ogonggo.userapi.auth.business.CompanySignInCommand
import com.ogonggo.userapi.auth.business.CompanySignUpCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CompanySignUpRequest(
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String,
    @field:NotBlank
    @field:Size(min = 8, max = 64)
    val password: String,
    @field:NotBlank
    @field:Size(max = 150)
    val organizationName: String,
    @field:NotBlank
    @field:Size(max = 100)
    val managerName: String,
) {
    fun toCommand(): CompanySignUpCommand = CompanySignUpCommand(
        email = email,
        password = password,
        organizationName = organizationName,
        managerName = managerName,
    )
}

data class CompanySignInRequest(
    @field:NotBlank val email: String,
    @field:NotBlank val password: String,
) {
    fun toCommand(): CompanySignInCommand = CompanySignInCommand(email = email, password = password)
}
