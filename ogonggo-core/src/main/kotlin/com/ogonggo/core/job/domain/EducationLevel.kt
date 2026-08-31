package com.ogonggo.core.job.domain

import com.ogonggo.core.enumeration.EnumField

enum class EducationLevel(
    override val code: Int,
    override val desc: String,
) : EnumField {
    ANY(1, "학력 무관"),
    HIGH_SCHOOL(2, "고등학교 졸업"),
    ASSOCIATE(3, "전문학사"),
    BACHELOR(4, "학사"),
    MASTER(5, "석사"),
    DOCTORATE(6, "박사"),
}
