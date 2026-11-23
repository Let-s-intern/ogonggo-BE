package com.ogonggo.core.work24jobfield.persistence

import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.work24jobfield.domain.Work24JobField
import org.springframework.data.jpa.repository.JpaRepository

internal interface Work24JobFieldJpaRepository : JpaRepository<Work24JobField, JobField>
