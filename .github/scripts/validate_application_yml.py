#!/usr/bin/env python3
"""
배포 직전에 만들어진 운영 설정 파일(application.yml)이 모듈의 설정 계약을 지키는지 검사한다.

배경
----
운영 설정은 저장소에 없고 GitHub 시크릿(APPLICATION_SECRET_*)의 내용을 그대로 파일로 쓴다.
시크릿은 사람이 편집기에서 붙여 넣는 값이라 다음 사고가 실제로 일어난다.

- 시크릿을 등록하지 않아 빈 문자열이 들어온다. GitHub는 없는 시크릿을 빈 값으로 대체하고
  워크플로의 `required: true`는 "호출자가 넘겼는지"만 볼 뿐 값이 비었는지는 보지 않는다.
- 들여쓰기가 한 칸 어긋나 키가 엉뚱한 부모 아래로 들어간다.
  (예: `spring.data.redis`가 `spring.jpa.properties.data.redis`가 된다)
- 같은 키를 두 번 써서 뒤의 블록이 앞을 덮는다.
- 값 앞뒤에 공백이나 줄바꿈이 붙는다.
- 로컬 값(localhost)이 그대로 남는다.

이런 파일도 jar에는 잘 들어가고 이미지도 잘 만들어진다. 문제는 ECS에서 컨테이너가 뜬 다음
드러나고, 그때는 이미 운영 서비스가 죽은 뒤다. 그래서 이미지를 만들기 전에 여기서 잡는다.

검사 항목
--------
1. YAML 파싱 가능 여부와 키 중복
2. 계약에 선언된 필수 키의 존재·비어있지 않음·형식(정규식, 타입, 허용값)
3. 계약에 없는 키(오타이거나 들여쓰기가 어긋난 키)
4. 쓰면 안 되는 키(다른 이름으로 옮겨간 옛 키 등)
5. 값 앞뒤 공백

부수 효과로, 계약에서 `sensitive: true`로 표시한 값은 `::add-mask::`로 등록해서
이후 단계의 로그(컨테이너 기동 로그 등)에 그대로 찍히지 않게 한다.

사용법
------
    validate_application_yml.py --spec <계약파일> --file <검사할 application.yml>
    validate_application_yml.py --spec <계약파일> --check-spec   # 계약 파일 자체를 검사한다
"""

from __future__ import annotations

import argparse
import base64
import binascii
import os
import re
import sys

import yaml

# Spring Boot의 Duration 표기. 접미사가 없으면 밀리초로 해석된다.
DURATION_PATTERN = re.compile(r"^\d+(ns|us|ms|s|m|h|d)?$")

# 계약 파일에서 규칙 하나가 가질 수 있는 필드. 오타를 잡기 위해 명시한다.
RULE_FIELDS = {
    "path",
    "required",
    "sensitive",
    "type",
    "equals",
    "one-of",
    "pattern",
    "forbid-substrings",
    "min-decoded-bytes",
    "min-length",
    "reason",
}

SPEC_FIELDS = {"module", "allowed-prefixes", "rules", "forbidden-paths"}


class DuplicateKeyError(Exception):
    pass


class StrictLoader(yaml.SafeLoader):
    """
    같은 매핑 안에서 키가 중복되면 실패하는 로더.

    기본 로더는 뒤의 값으로 조용히 덮어쓴다. 시크릿을 편집하다 `spring:` 블록을 두 번 만들면
    앞 블록의 datasource 설정이 통째로 사라지는데, 파일만 봐서는 알아채기 어렵다.
    """


def _construct_mapping(loader: StrictLoader, node: yaml.MappingNode, deep: bool = False):
    mapping = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if key in mapping:
            mark = key_node.start_mark
            raise DuplicateKeyError(f"{mark.line + 1}번째 줄에서 키 '{key}' 가 중복됩니다")
        mapping[key] = loader.construct_object(value_node, deep=deep)
    return mapping


StrictLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, _construct_mapping)


class Report:
    """오류를 모아 두었다가 한 번에 보고한다. 첫 오류에서 멈추면 고치는 데 여러 번 배포해야 한다."""

    def __init__(self) -> None:
        self.errors: list[str] = []
        self.notices: list[str] = []

    def error(self, message: str) -> None:
        self.errors.append(message)

    def notice(self, message: str) -> None:
        self.notices.append(message)

    def flush(self) -> int:
        for message in self.notices:
            print(f"::notice::{message}")
        for message in self.errors:
            print(f"::error::{message}")
        return 1 if self.errors else 0


def flatten(node, prefix: str = "") -> dict[str, object]:
    """중첩 매핑을 `a.b.c` 형태의 잎 경로 사전으로 편다. 리스트는 잎으로 본다."""
    flat: dict[str, object] = {}
    if isinstance(node, dict):
        for key, value in node.items():
            path = f"{prefix}.{key}" if prefix else str(key)
            if isinstance(value, dict) and value:
                flat.update(flatten(value, path))
            else:
                flat[path] = value
    else:
        flat[prefix] = node
    return flat


def is_empty(value) -> bool:
    if value is None:
        return True
    if isinstance(value, str) and value.strip() == "":
        return True
    return False


def covered_by(path: str, prefixes: list[str]) -> bool:
    return any(path == prefix or path.startswith(prefix + ".") for prefix in prefixes)


def mask(value) -> None:
    """
    이후 단계 로그에서 값이 가려지도록 등록한다.

    짧은 값까지 가리면 로그가 `***` 범벅이 되어 오히려 원인을 못 찾는다. 그래서 길이로 자른다.
    """
    text = str(value)
    if len(text) >= 8:
        print(f"::add-mask::{text}")


def load_spec(spec_path: str) -> dict:
    with open(spec_path, encoding="utf-8") as handle:
        return yaml.load(handle, Loader=StrictLoader)


def check_spec(spec: dict, spec_path: str, report: Report) -> None:
    """
    계약 파일 자체를 검사한다.

    계약이 늘어나면서 규칙 필드에 오타가 나거나, 규칙에는 있는데 allowed-prefixes에 없어서
    "필수인데 동시에 알 수 없는 키"가 되는 모순이 생긴다. PR에서 미리 잡는다.
    """
    unknown = set(spec) - SPEC_FIELDS
    if unknown:
        report.error(f"{spec_path}: 알 수 없는 최상위 항목 {sorted(unknown)}")

    for field in ("module", "allowed-prefixes", "rules"):
        if field not in spec:
            report.error(f"{spec_path}: 필수 항목 '{field}' 가 없습니다")
            return

    prefixes = spec["allowed-prefixes"]
    seen: set[str] = set()
    for rule in spec["rules"]:
        path = rule.get("path")
        if not path:
            report.error(f"{spec_path}: path 없는 규칙이 있습니다")
            continue
        if path in seen:
            report.error(f"{spec_path}: 규칙 '{path}' 가 중복 선언되었습니다")
        seen.add(path)

        unknown_fields = set(rule) - RULE_FIELDS
        if unknown_fields:
            report.error(f"{spec_path}: 규칙 '{path}' 에 알 수 없는 필드 {sorted(unknown_fields)}")

        if not covered_by(path, prefixes):
            report.error(
                f"{spec_path}: 규칙 '{path}' 가 allowed-prefixes 에 없습니다. "
                "필수 키가 동시에 '알 수 없는 키'로 걸립니다"
            )

        if "pattern" in rule:
            try:
                re.compile(rule["pattern"])
            except re.error as exc:
                report.error(f"{spec_path}: 규칙 '{path}' 의 pattern 이 정규식이 아닙니다: {exc}")

    for path in spec.get("forbidden-paths", []):
        if path.get("path") in seen:
            report.error(f"{spec_path}: '{path.get('path')}' 가 rules 와 forbidden-paths 에 모두 있습니다")


def normalize(value) -> str:
    """
    YAML 값을 비교용 문자열로 만든다.

    불리언을 str()로 바로 바꾸면 파이썬 표기인 "False"가 나와 YAML 표기 "false"와 어긋난다.
    비교하는 두 값 모두 이 함수를 거쳐야 한다.
    """
    if isinstance(value, bool):
        return "true" if value else "false"
    return str(value).strip()


def check_value(rule: dict, path: str, value, report: Report) -> None:
    sensitive = bool(rule.get("sensitive"))
    reason = rule.get("reason")
    suffix = f" {reason}" if reason else ""

    if isinstance(value, str) and value != value.strip():
        report.error(f"{path}: 값 앞뒤에 공백이나 줄바꿈이 붙어 있습니다.{suffix}")

    text = normalize(value)
    shown = "(민감한 값이라 표시하지 않습니다)" if sensitive else repr(text)

    if "min-length" in rule and len(text) < rule["min-length"]:
        report.error(f"{path}: 값이 너무 짧습니다. {rule['min-length']}자 이상이어야 합니다.{suffix}")

    expected_type = rule.get("type")
    if expected_type == "int":
        if not re.fullmatch(r"-?\d+", text):
            report.error(f"{path}: 정수여야 하는데 {shown} 입니다.{suffix}")
    elif expected_type == "bool":
        if text not in ("true", "false"):
            report.error(f"{path}: true/false 여야 하는데 {shown} 입니다.{suffix}")
    elif expected_type == "duration":
        if not DURATION_PATTERN.fullmatch(text):
            report.error(
                f"{path}: Spring Duration 표기(예: 30m, 14d, 5s)여야 하는데 {shown} 입니다.{suffix}"
            )
    elif expected_type == "base64":
        try:
            decoded = base64.b64decode(text, validate=True)
        except (binascii.Error, ValueError):
            report.error(f"{path}: Base64 로 디코딩되지 않습니다.{suffix}")
        else:
            minimum = rule.get("min-decoded-bytes")
            if minimum and len(decoded) < minimum:
                report.error(
                    f"{path}: 디코딩 결과가 {len(decoded)}바이트입니다. "
                    f"{minimum}바이트 이상이어야 합니다.{suffix}"
                )

    if "equals" in rule and text != normalize(rule["equals"]):
        report.error(f"{path}: {rule['equals']!r} 여야 하는데 {shown} 입니다.{suffix}")

    if "one-of" in rule:
        allowed = [normalize(item) for item in rule["one-of"]]
        if text not in allowed:
            report.error(f"{path}: 허용값 {allowed} 중 하나여야 하는데 {shown} 입니다.{suffix}")

    if "pattern" in rule and not re.search(rule["pattern"], text):
        report.error(f"{path}: 형식이 올바르지 않습니다. 기대 형식 /{rule['pattern']}/.{suffix}")

    for banned in rule.get("forbid-substrings", []):
        if banned in text:
            report.error(f"{path}: 값에 {banned!r} 가 들어 있습니다.{suffix}")

    # 운영 설정은 완성된 파일이어야 한다. 남아 있는 치환자는 컨테이너에서 기동 실패로 이어진다.
    if "${" in text:
        report.notice(
            f"{path}: 치환자 '${{...}}' 가 남아 있습니다. "
            "ECS 태스크 정의에 해당 환경변수가 없으면 기동에 실패합니다."
        )
    for placeholder in ("CHANGE_ME", "TODO", "여기에"):
        if placeholder in text:
            report.error(f"{path}: 자리표시자 {placeholder!r} 가 남아 있습니다.{suffix}")


def validate(spec: dict, file_path: str, report: Report) -> dict[str, object]:
    try:
        with open(file_path, encoding="utf-8") as handle:
            raw = handle.read()
    except OSError as exc:
        report.error(f"{file_path} 를 읽지 못했습니다: {exc}")
        return {}

    if not raw.strip():
        report.error(
            f"{file_path} 이 비어 있습니다. APPLICATION_SECRET 시크릿이 등록되어 있는지 확인하세요."
        )
        return {}

    try:
        document = yaml.load(raw, Loader=StrictLoader)
    except DuplicateKeyError as exc:
        report.error(f"{file_path}: {exc}. 뒤의 블록이 앞의 설정을 통째로 덮어씁니다.")
        return {}
    except yaml.YAMLError as exc:
        report.error(f"{file_path} 이 올바른 YAML 이 아닙니다. 들여쓰기를 확인하세요: {exc}")
        return {}

    if not isinstance(document, dict):
        report.error(f"{file_path} 의 최상위가 매핑이 아닙니다.")
        return {}

    flat = flatten(document)

    # 민감한 값을 먼저 가린다. 아래에서 오류 메시지를 만들기 전에 등록되어야 한다.
    for rule in spec["rules"]:
        if rule.get("sensitive") and not is_empty(flat.get(rule["path"])):
            mask(flat[rule["path"]])

    for rule in spec["rules"]:
        path = rule["path"]
        required = rule.get("required", True)
        if path not in flat or is_empty(flat[path]):
            if required:
                reason = rule.get("reason")
                suffix = f" {reason}" if reason else ""
                report.error(f"{path}: 필수 설정인데 없거나 비어 있습니다.{suffix}")
            continue
        check_value(rule, path, flat[path], report)

    reported: set[str] = set()
    for entry in spec.get("forbidden-paths", []):
        path = entry["path"]
        if path in flat:
            report.error(f"{path}: 쓰지 않는 키입니다. {entry.get('reason', '')}".strip())
            reported.add(path)

    # 아래 검사는 계약이 모르는 키를 찾는 그물이다. 위에서 이미 이름을 짚어 설명한 키는
    # 같은 내용을 두 번 말하게 되므로 건너뛴다.
    prefixes = spec["allowed-prefixes"]
    for path in sorted(flat):
        if path in reported:
            continue
        if not covered_by(path, prefixes):
            report.error(
                f"{path}: 이 모듈의 설정 계약에 없는 키입니다. "
                "들여쓰기가 어긋났거나 오타입니다. 의도한 키라면 "
                f".github/config-schema/{spec['module']}.yml 의 allowed-prefixes 에 추가하세요."
            )

    return flat


def write_summary(spec: dict, flat: dict[str, object], file_path: str) -> None:
    """
    무엇이 실제로 배포되는지 잡 요약에 남긴다. 민감한 값은 길이만 적는다.

    "시크릿을 고쳤는데 왜 그대로지"를 확인할 때 이 표가 있으면 로그를 뒤지지 않아도 된다.
    """
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return

    lines = [
        f"### {spec['module']} 설정 검증",
        "",
        f"검사 파일: `{file_path}`",
        "",
        "| 키 | 값 |",
        "| --- | --- |",
    ]
    for rule in spec["rules"]:
        path = rule["path"]
        if path not in flat or is_empty(flat[path]):
            lines.append(f"| `{path}` | _없음_ |")
            continue
        if rule.get("sensitive"):
            lines.append(f"| `{path}` | _설정됨 ({len(str(flat[path]))}자)_ |")
        else:
            lines.append(f"| `{path}` | `{flat[path]}` |")
    lines.append("")

    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write("\n".join(lines))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--spec", required=True, help="모듈 설정 계약 파일 경로")
    parser.add_argument("--file", help="검사할 application.yml 경로")
    parser.add_argument(
        "--check-spec",
        action="store_true",
        help="계약 파일 자체만 검사한다. PR 빌드에서 계약이 깨지지 않았는지 본다.",
    )
    args = parser.parse_args()

    report = Report()

    try:
        spec = load_spec(args.spec)
    except DuplicateKeyError as exc:
        report.error(f"{args.spec}: {exc}")
        return report.flush()
    except (OSError, yaml.YAMLError) as exc:
        report.error(f"{args.spec} 를 읽지 못했습니다: {exc}")
        return report.flush()

    check_spec(spec, args.spec, report)
    if report.errors:
        return report.flush()

    if args.check_spec:
        print(f"{args.spec}: 계약 파일 검사를 통과했습니다.")
        return report.flush()

    if not args.file:
        report.error("--file 또는 --check-spec 중 하나가 필요합니다.")
        return report.flush()

    flat = validate(spec, args.file, report)
    exit_code = report.flush()

    if flat:
        write_summary(spec, flat, args.file)

    if exit_code == 0:
        print(f"{args.file}: {spec['module']} 설정 계약을 통과했습니다.")
    else:
        print(
            "설정 파일이 계약을 어겼습니다. 이미지를 만들지 않고 중단합니다. "
            f"GitHub 시크릿 APPLICATION_SECRET_* 의 내용을 고친 뒤 다시 배포하세요.",
            file=sys.stderr,
        )
    return exit_code


if __name__ == "__main__":
    sys.exit(main())
