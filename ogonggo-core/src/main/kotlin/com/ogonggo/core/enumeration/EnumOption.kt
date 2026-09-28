package com.ogonggo.core.enumeration

/**
 * 클라이언트가 선택지와 라벨을 그리는 데 쓰는 enum 값 하나다.
 *
 * 요청과 응답의 enum은 이름으로 주고받으므로 [name]이 기준 값이고 [desc]는 화면에 보일 한국어 라벨이다.
 * [EnumField.code]는 업무 메타데이터라 클라이언트가 코드 번호로 요청하지 않도록 싣지 않는다.
 * [parent]는 [HierarchicalEnumField]일 때 상위 값의 이름이고, 그 밖에는 null이다.
 */
data class EnumOption(
    val name: String,
    val desc: String,
    val parent: String? = null,
)

/** 선언 순서를 유지한 채 선택지 목록으로 바꾼다. */
fun <E> Iterable<E>.toEnumOptions(): List<EnumOption> where E : Enum<E>, E : EnumField =
    map { value ->
        EnumOption(
            name = value.name,
            desc = value.desc,
            parent = ((value as? HierarchicalEnumField)?.parent as? Enum<*>)?.name,
        )
    }
