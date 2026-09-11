#!/usr/bin/env python3
"""
운영 설정 파일(application.yml)에서 민감해 보이는 값을 GitHub Actions 로그 마스킹에 등록한다.

설정 파일의 내용은 검사하지 않고, 어떤 경우에도 배포를 막지 않는다.

배포 워크플로의 컨테이너 기동 검증이 실패하면 컨테이너 로그를 출력한다. 그때 DB 비밀번호나
JWT 키가 그대로 찍히지 않게 하는 것이 목적이다. `APPLICATION_SECRET` 시크릿은 GitHub이 통째로
하나의 문자열로만 가리므로, 그 안의 개별 값은 따로 등록해야 가려진다.

YAML 로 파싱하지 않고 `키: 값` 모양의 줄을 읽는다. 들여쓰기가 깨진 파일일수록 스프링이 기동 중에
문제가 된 줄을 로그에 옮기므로, 파일이 깨져 있어도 값을 가릴 수 있어야 한다.

    mask_application_secrets.py --file <application.yml>
"""

from __future__ import annotations

import argparse
import re
import sys

# 키 이름에 이 단어가 들어가면 민감한 값으로 본다.
# 설정 키 목록을 따로 관리하지 않기 위해 이름으로 판단한다.
SENSITIVE_KEY_PATTERN = re.compile(r"password|secret|key|token|credential|username", re.IGNORECASE)

# 키 이름으로는 드러나지 않지만 값 자체가 비밀인 경우다. 슬랙 웹훅 URL 은 그 자체가 인증 수단이다.
SENSITIVE_VALUE_PATTERN = re.compile(r"^https://hooks\.slack\.com/")

# 짧은 값까지 가리면 로그가 `***` 범벅이 되어 오히려 원인을 못 찾는다.
MIN_MASK_LENGTH = 8

# `키: 값` 한 줄. 리스트 항목(`- 키: 값`)도 받는다. 콜론 뒤에 공백이 있어야 YAML 매핑이다.
KEY_VALUE_LINE = re.compile(r"^\s*(?:-\s+)?(?P<key>[^\s:#][^:#]*?)\s*:\s+(?P<value>.+)$")


def scalar(text: str) -> str | None:
    """줄에 적힌 값에서 따옴표와 줄 끝 주석을 걷어 로그에 찍힐 모양으로 만든다."""
    text = text.strip()
    if not text or text[0] in "|>&*!{[":
        # 블록 스칼라·앵커·흐름 표기는 한 줄만으로 값을 알 수 없다.
        return None
    if text[0] in "\"'":
        end = text.find(text[0], 1)
        return text[1:end] if end > 0 else text[1:]
    return re.split(r"\s+#", text, maxsplit=1)[0].strip()


def secret_values(raw: str) -> list[str]:
    values: list[str] = []
    for line in raw.splitlines():
        match = KEY_VALUE_LINE.match(line)
        if not match:
            continue
        value = scalar(match.group("value"))
        if not value or len(value) < MIN_MASK_LENGTH:
            continue
        if SENSITIVE_KEY_PATTERN.search(match.group("key")) or SENSITIVE_VALUE_PATTERN.match(value):
            values.append(value)
    return list(dict.fromkeys(values))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--file", required=True, help="application.yml 경로")
    args = parser.parse_args()

    try:
        with open(args.file, encoding="utf-8") as handle:
            raw = handle.read()
    except OSError as exc:
        print(f"::warning::{args.file} 를 읽지 못해 민감한 값을 가리지 못했습니다: {exc}")
        return 0

    values = secret_values(raw)
    for value in values:
        print(f"::add-mask::{value}")
    print(f"{args.file}: 민감해 보이는 값 {len(values)}개를 로그 마스킹에 등록했습니다.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
