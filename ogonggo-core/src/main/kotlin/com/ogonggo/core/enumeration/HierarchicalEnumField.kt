package com.ogonggo.core.enumeration

/** 다른 업무 enum의 값 아래에 속하는 enum이다. 시·군·구가 시·도에 속하는 경우처럼 선택지를 묶어 보여 줄 때 쓴다. */
interface HierarchicalEnumField : EnumField {
    val parent: EnumField
}
