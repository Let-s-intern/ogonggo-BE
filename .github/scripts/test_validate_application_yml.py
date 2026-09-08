#!/usr/bin/env python3
"""
설정 검증기가 실제로 사고를 잡는지 확인한다.

검증기가 조용히 전부 통과시키면 없느니만 못하다. 배포를 막는 장치이므로
"막아야 할 파일을 막는지"를 여기서 못박아 둔다. PR 빌드(.github/workflows/test.yml)에서 돈다.

    python3 .github/scripts/test_validate_application_yml.py
"""

from __future__ import annotations

import pathlib
import subprocess
import sys
import tempfile
import textwrap
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
VALIDATOR = ROOT / ".github" / "scripts" / "validate_application_yml.py"
USER_SPEC = ROOT / ".github" / "config-schema" / "ogonggo-api-user.yml"
ADMIN_SPEC = ROOT / ".github" / "config-schema" / "ogonggo-api-admin.yml"

# 계약을 통과하는 최소 설정. 각 검사는 여기서 한 곳만 망가뜨린다.
# 값은 전부 형식만 맞춘 가짜다.
VALID_USER_CONFIG = textwrap.dedent(
    """\
    spring:
      application:
        name: ogonggo-api-user
      datasource:
        url: jdbc:mysql://ogonggo-db.example.ap-northeast-2.rds.amazonaws.com:3306/ogonggo?serverTimezone=Asia/Seoul
        username: admin
        password: not-a-real-password
        driver-class-name: com.mysql.cj.jdbc.Driver
      jpa:
        open-in-view: false
        hibernate:
          ddl-auto: update
      mail:
        host: email-smtp.ap-northeast-2.amazonaws.com
        port: 587
        username: AKIAEXAMPLEEXAMPLE00
        password: not-a-real-smtp-password-value
      data:
        redis:
          host: ogonggo-redis.example.apn2.cache.amazonaws.com
          port: 6379

    ogonggo:
      auth:
        jwt:
          secret: {jwt_secret}
          access-token-validity: 30m
          refresh-token-validity: 14d
      letscareer:
        base-url: https://api.letscareer.co.kr
        internal-api-key: 0123456789abcdef0123456789abcdef0123
      advertisement:
        mail:
          from: official@letscareer.co.kr

    server:
      port: 8080
      shutdown: graceful

    springdoc:
      paths-to-match: /api/**
    """
).format(jwt_secret="A" * 88)  # Base64 88자는 66바이트로 디코딩된다.

VALID_ADMIN_CONFIG = textwrap.dedent(
    """\
    spring:
      application:
        name: ogonggo-api-admin
      datasource:
        url: jdbc:mysql://ogonggo-db.example.ap-northeast-2.rds.amazonaws.com:3306/ogonggo
        username: admin
        password: not-a-real-password
      jpa:
        open-in-view: false
        hibernate:
          ddl-auto: none

    ogonggo:
      admin:
        internal:
          api-key: 0123456789abcdef0123456789abcdef0123

    server:
      port: 8081
      shutdown: graceful

    springdoc:
      paths-to-match: /api/**
    """
)


def run_validator(spec: pathlib.Path, content: str | None) -> subprocess.CompletedProcess:
    with tempfile.TemporaryDirectory() as directory:
        target = pathlib.Path(directory) / "application.yml"
        target.write_text("" if content is None else content, encoding="utf-8")
        return subprocess.run(
            [sys.executable, str(VALIDATOR), "--spec", str(spec), "--file", str(target)],
            capture_output=True,
            text=True,
        )


class SpecFilesAreWellFormed(unittest.TestCase):
    """계약 파일 자체가 깨지지 않았는지 본다. 규칙 오타와 접두사 누락을 잡는다."""

    def test_specs_pass_self_check(self):
        for spec in (USER_SPEC, ADMIN_SPEC):
            with self.subTest(spec=spec.name):
                result = subprocess.run(
                    [sys.executable, str(VALIDATOR), "--spec", str(spec), "--check-spec"],
                    capture_output=True,
                    text=True,
                )
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)


class ValidConfigPasses(unittest.TestCase):
    def test_user(self):
        result = run_validator(USER_SPEC, VALID_USER_CONFIG)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_admin(self):
        result = run_validator(ADMIN_SPEC, VALID_ADMIN_CONFIG)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)


class BrokenConfigIsRejected(unittest.TestCase):
    """실제로 겪은 사고를 하나씩 재현한다."""

    def assert_rejected(self, content: str | None, expected: str, spec: pathlib.Path = USER_SPEC):
        result = run_validator(spec, content)
        self.assertEqual(result.returncode, 1, "통과하면 안 되는 설정이 통과했습니다:\n" + result.stdout)
        self.assertIn(expected, result.stdout)

    def test_empty_file(self):
        # 시크릿을 등록하지 않으면 GitHub이 빈 문자열을 넣는다.
        self.assert_rejected(None, "비어 있습니다")

    def test_not_yaml(self):
        self.assert_rejected("spring:\n  datasource:\n   url: a\n  \tbad", "YAML")

    def test_duplicate_key_silently_overwrites(self):
        # 뒤의 spring 블록이 앞의 datasource 설정을 통째로 지운다.
        content = VALID_USER_CONFIG + "\nspring:\n  application:\n    name: ogonggo-api-user\n"
        self.assert_rejected(content, "중복")

    def test_misindented_redis_lands_under_jpa(self):
        """
        들여쓰기가 밀려 Redis 설정이 spring.jpa.properties 아래로 들어간 경우.

        YAML로는 멀쩡해서 파싱도 되고 애플리케이션도 뜬다. 다만 Redis 클라이언트는
        localhost 기본값을 보게 되고, 리프레시 토큰 저장이 운영에서만 실패한다.
        실제로 저장소의 admin dev 설정에 같은 모양의 오류가 있다.
        """
        correct = (
            "  data:\n"
            "    redis:\n"
            "      host: ogonggo-redis.example.apn2.cache.amazonaws.com\n"
            "      port: 6379\n"
        )
        hibernate = "    hibernate:\n      ddl-auto: update\n"
        misindented = hibernate + (
            "    properties:\n"
            "      data:\n"
            "        redis:\n"
            "          host: ogonggo-redis.example.apn2.cache.amazonaws.com\n"
            "          port: 6379\n"
        )
        self.assertIn(correct, VALID_USER_CONFIG, "테스트 픽스처가 예상과 다릅니다")
        self.assertIn(hibernate, VALID_USER_CONFIG, "테스트 픽스처가 예상과 다릅니다")
        broken = VALID_USER_CONFIG.replace(correct, "").replace(hibernate, misindented)
        self.assert_rejected(broken, "설정 계약에 없는 키")

    def test_short_jwt_secret(self):
        # jjwt 가 빈을 만들 때 예외를 던져 컨테이너가 죽는다.
        content = VALID_USER_CONFIG.replace("A" * 88, "c2hvcnQtc2VjcmV0")
        self.assert_rejected(content, "ogonggo.auth.jwt.secret")

    def test_jwt_secret_not_base64(self):
        content = VALID_USER_CONFIG.replace("A" * 88, "이건 base64 가 아닙니다")
        self.assert_rejected(content, "ogonggo.auth.jwt.secret")

    def test_local_datasource_url(self):
        content = VALID_USER_CONFIG.replace(
            "ogonggo-db.example.ap-northeast-2.rds.amazonaws.com", "localhost"
        )
        self.assert_rejected(content, "spring.datasource.url")

    def test_local_redis_host(self):
        content = VALID_USER_CONFIG.replace(
            "ogonggo-redis.example.apn2.cache.amazonaws.com", "localhost"
        )
        self.assert_rejected(content, "spring.data.redis.host")

    def test_missing_mail_host_breaks_context(self):
        content = VALID_USER_CONFIG.replace(
            "    host: email-smtp.ap-northeast-2.amazonaws.com\n", ""
        )
        self.assert_rejected(content, "spring.mail.host")

    def test_wrong_port(self):
        content = VALID_USER_CONFIG.replace("  port: 8080", "  port: 8081")
        self.assert_rejected(content, "server.port")

    def test_open_in_view_enabled(self):
        content = VALID_USER_CONFIG.replace("open-in-view: false", "open-in-view: true")
        self.assert_rejected(content, "spring.jpa.open-in-view")

    def test_destructive_ddl_auto(self):
        content = VALID_USER_CONFIG.replace("ddl-auto: update", "ddl-auto: create-drop")
        self.assert_rejected(content, "ddl-auto")

    def test_trailing_whitespace_in_value(self):
        content = VALID_USER_CONFIG.replace(
            "    username: admin\n", '    username: "admin "\n'
        )
        self.assertNotEqual(content, VALID_USER_CONFIG, "테스트 픽스처가 예상과 다릅니다")
        self.assert_rejected(content, "공백")

    def test_placeholder_left_behind(self):
        content = VALID_USER_CONFIG.replace("internal-api-key: 0123", "internal-api-key: CHANGE_ME0123")
        self.assert_rejected(content, "자리표시자")

    def test_dead_spring_mail_from_key(self):
        content = VALID_USER_CONFIG.replace(
            "    port: 587\n", "    port: 587\n    from: official@letscareer.co.kr\n"
        )
        self.assert_rejected(content, "spring.mail.from")

    def test_user_config_deployed_to_admin(self):
        # 호출자에서 APPLICATION_SECRET_USER 와 _ADMIN 을 바꿔 넣은 경우다.
        self.assert_rejected(VALID_USER_CONFIG, "ogonggo-api-admin", spec=ADMIN_SPEC)

    def test_admin_config_deployed_to_user(self):
        self.assert_rejected(VALID_ADMIN_CONFIG, "ogonggo-api-user")


class SensitiveValuesAreMasked(unittest.TestCase):
    """
    이후 단계 로그에 시크릿이 그대로 찍히지 않도록 마스킹을 등록하는지 본다.

    기동 검증이 실패하면 컨테이너 로그를 통째로 출력하는데, 그때 DB 비밀번호가
    로그에 남으면 안 된다.
    """

    def test_emits_add_mask_for_secrets(self):
        result = run_validator(USER_SPEC, VALID_USER_CONFIG)
        self.assertIn("::add-mask::not-a-real-password", result.stdout)
        self.assertIn("::add-mask::" + "A" * 88, result.stdout)

    def test_error_message_does_not_echo_secret_value(self):
        content = VALID_USER_CONFIG.replace("not-a-real-password", "short")
        result = run_validator(USER_SPEC, content)
        self.assertEqual(result.returncode, 1)
        error_lines = [line for line in result.stdout.splitlines() if line.startswith("::error::")]
        self.assertTrue(error_lines)
        for line in error_lines:
            self.assertNotIn("short", line)


if __name__ == "__main__":
    unittest.main(verbosity=2)
