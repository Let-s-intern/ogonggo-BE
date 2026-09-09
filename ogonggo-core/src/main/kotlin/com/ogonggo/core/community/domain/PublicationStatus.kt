package com.ogonggo.core.community.domain

import com.ogonggo.core.enumeration.EnumField

enum class PublicationStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    PUBLISHED(1, "공개"),
    HIDDEN(2, "비공개"),
}
