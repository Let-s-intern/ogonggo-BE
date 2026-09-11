#!/usr/bin/env python3
"""
시크릿 마스킹 스크립트가 민감한 값을 가리고, 어떤 파일에서도 배포를 막지 않는지 확인한다.

기동 검증이 실패하면 컨테이너 로그를 통째로 출력하는데, 그때 DB 비밀번호가 로그에 남으면 안 된다.
PR 빌드(.github/workflows/test.yml)에서 돈다.

    python3 .github/scripts/test_mask_application_secrets.py
"""

from __future__ import annotations

import pathlib
import subprocess
import sys
import tempfile
import textwrap
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT = ROOT / ".github" / "scripts" / "mask_application_secrets.py"

# 실제 운영 설정과 모양이 같은 파일. 값은 전부 가짜다.
USER_CONFIG = textwrap.dedent(
    """\
    spring:
      datasource:
        url: jdbc:mysql://ogonggo-db.example.ap-northeast-2.rds.amazonaws.com:3306/ogonggo
        username: admin
        password: not-a-real-password
      mail:
        host: email-smtp.ap-northeast-2.amazonaws.com
        username: AKIAEXAMPLEEXAMPLE00
        password: "not-a-real-smtp-password-value"
      data:
        redis:
          host: ogonggo-redis.example.apn2.cache.amazonaws.com
          port: 6379

    ogonggo:
      auth:
        jwt:
          secret: {jwt_secret}
          access-token-validity: 30m
      letscareer:
        base-url: https://api.letscareer.co.kr
        internal-api-key: 0123456789abcdef0123456789abcdef0123 # 렛츠커리어와 같은 값
      advertisement:
        slack:
          inquiry-url: 'https://hooks.slack.com/services/T000/B000/not-a-real-webhook'

    server:
      port: 8080
    """
).format(jwt_secret="A" * 88)


def run_script(content: str | None) -> subprocess.CompletedProcess:
    with tempfile.TemporaryDirectory() as directory:
        target = pathlib.Path(directory) / "application.yml"
        if content is not None:
            target.write_text(content, encoding="utf-8")
        return subprocess.run(
            [sys.executable, str(SCRIPT), "--file", str(target)],
            capture_output=True,
            text=True,
        )


class SensitiveValuesAreMasked(unittest.TestCase):
    def test_masks_values_whose_key_names_look_secret(self):
        result = run_script(USER_CONFIG)
        for secret in (
            "not-a-real-password",
            "not-a-real-smtp-password-value",
            "AKIAEXAMPLEEXAMPLE00",
            "A" * 88,
            "0123456789abcdef0123456789abcdef0123",
            "https://hooks.slack.com/services/T000/B000/not-a-real-webhook",
        ):
            with self.subTest(secret=secret[:12]):
                self.assertIn(f"::add-mask::{secret}\n", result.stdout)

    def test_strips_quotes_and_inline_comments(self):
        # 로그에 찍히는 것은 따옴표와 주석을 뺀 값이다. 그 모양으로 등록해야 가려진다.
        result = run_script(USER_CONFIG)
        self.assertNotIn('::add-mask::"not-a-real-smtp-password-value"', result.stdout)
        self.assertNotIn("# 렛츠커리어와 같은 값", result.stdout)

    def test_does_not_mask_ordinary_or_short_values(self):
        # 호스트까지 가리면 기동 실패 로그에서 원인을 찾을 수 없다.
        result = run_script(USER_CONFIG)
        self.assertNotIn("::add-mask::ogonggo-redis.example.apn2.cache.amazonaws.com", result.stdout)
        self.assertNotIn("::add-mask::https://api.letscareer.co.kr", result.stdout)
        self.assertNotIn("::add-mask::admin", result.stdout)

    def test_masks_even_when_indentation_is_broken(self):
        # 깨진 파일일수록 스프링이 문제가 된 줄을 로그에 옮기므로 이때도 가려야 한다.
        broken = USER_CONFIG.replace("    password: not-a-real-password\n", "      password: not-a-real-password\n")
        result = run_script(broken)
        self.assertIn("::add-mask::not-a-real-password\n", result.stdout)


class NeverBlocksDeployment(unittest.TestCase):
    """설정 파일 내용은 검사하지 않는다. 어떤 파일이 와도 성공으로 끝나야 한다."""

    def test_succeeds_for_any_file(self):
        cases = {
            "정상 설정": USER_CONFIG,
            "빈 파일": "",
            "깨진 들여쓰기": "spring:\n  datasource:\n    url: a\n      username: b\n",
            "파일 없음": None,
        }
        for name, content in cases.items():
            with self.subTest(name):
                result = run_script(content)
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_output_does_not_echo_values_outside_mask_commands(self):
        result = run_script(USER_CONFIG)
        other_lines = [line for line in result.stdout.splitlines() if not line.startswith("::add-mask::")]
        for line in other_lines:
            self.assertNotIn("not-a-real-password", line)


if __name__ == "__main__":
    unittest.main(verbosity=2)
