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

/** enum 하나를 선택지 목록의 한 항목으로 만든다. 키는 enum 클래스 이름이다. */
inline fun <reified E> enumOptionsOf(): Pair<String, List<EnumOption>> where E : Enum<E>, E : EnumField =
    E::class.java.simpleName to enumValues<E>().asIterable().toEnumOptions()

/** 선택지 목록을 합친다. 같은 키가 둘 이상이면 어느 값을 내보낼지 알 수 없으므로 실패한다. */
fun enumOptionMapOf(vararg groups: Map<String, List<EnumOption>>): Map<String, List<EnumOption>> {
    val duplicated = groups.flatMap { it.keys }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    check(duplicated.isEmpty()) { "enum 이름이 겹칩니다: $duplicated" }
    return groups.fold(emptyMap()) { merged, group -> merged + group }
}

fun enumOptionMapOf(vararg entries: Pair<String, List<EnumOption>>): Map<String, List<EnumOption>> =
    enumOptionMapOf(*entries.map { mapOf(it) }.toTypedArray())
