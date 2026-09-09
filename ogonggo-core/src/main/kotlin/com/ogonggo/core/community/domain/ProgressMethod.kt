package com.ogonggo.core.community.domain

import com.ogonggo.core.enumeration.EnumField

enum class ProgressMethod(
    override val code: Int,
    override val desc: String,
) : EnumField {
    ONLINE(1, "온라인"),
    OFFLINE(2, "오프라인"),
    HYBRID(3, "온·오프라인"),
}
