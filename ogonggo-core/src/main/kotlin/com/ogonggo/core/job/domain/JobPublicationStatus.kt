package com.ogonggo.core.job.domain

import com.ogonggo.core.enumeration.EnumField

enum class JobPublicationStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    DRAFT(1, "초안"),
    PUBLISHED(2, "게시"),
    HIDDEN(3, "숨김"),
    ARCHIVED(4, "보관"),
}
