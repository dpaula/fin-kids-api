#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_PATH="$ROOT_DIR/scripts/release-local.sh"

TMP_ONE="$(mktemp)"
TMP_TWO="$(mktemp)"
trap 'rm -f "$TMP_ONE" "$TMP_TWO"' EXIT

run_and_capture() {
  local output_file="$1"
  shift
  (
    cd "$ROOT_DIR"
    "$SCRIPT_PATH" "$@" >"$output_file"
  )
}

assert_contains() {
  local output_file="$1"
  local expected="$2"
  if ! grep -Fq -- "$expected" "$output_file"; then
    echo "Falha: esperado '$expected' no output." >&2
    echo "------ output ------" >&2
    cat "$output_file" >&2
    echo "--------------------" >&2
    exit 1
  fi
}

assert_not_contains() {
  local output_file="$1"
  local text="$2"
  if grep -Fq -- "$text" "$output_file"; then
    echo "Falha: nao era esperado '$text' no output." >&2
    echo "------ output ------" >&2
    cat "$output_file" >&2
    echo "--------------------" >&2
    exit 1
  fi
}

run_and_capture "$TMP_ONE" 1.2.3-rc1 --dry-run --allow-dirty --no-pull
assert_contains "$TMP_ONE" "Versao atual:"
assert_contains "$TMP_ONE" "Nova versao:  1.2.3-rc1"
assert_contains "$TMP_ONE" "versions:set"
assert_contains "$TMP_ONE" "mvnw verify"
assert_contains "$TMP_ONE" "jib-docker-build"

run_and_capture "$TMP_TWO" 1.2.4-rc1 --dry-run --allow-dirty --skip-tests --no-pull
assert_contains "$TMP_TWO" "Nova versao:  1.2.4-rc1"
assert_contains "$TMP_TWO" "versions:set"
assert_contains "$TMP_TWO" "jib-docker-build"
assert_not_contains "$TMP_TWO" "mvnw verify"

echo "OK: testes de smoke do script de release local passaram."
