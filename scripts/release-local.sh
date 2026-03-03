#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MVN_BIN="${MVN_BIN:-$ROOT_DIR/mvnw}"
DOCKER_IMAGE="${DOCKER_IMAGE:-docker.io/fplima/fin-kids-api}"

NEW_VERSION=""
SKIP_TESTS=false
DRY_RUN=false
ALLOW_DIRTY=false
VALIDATE_PULL=true

usage() {
  cat <<'EOF'
Uso:
  ./scripts/release-local.sh <nova-versao> [opcoes]

Opcoes:
  --skip-tests    Nao executa "mvn verify" antes da publicacao.
  --dry-run       Exibe os comandos sem executar.
  --allow-dirty   Permite executar com alteracoes locais nao commitadas.
  --no-pull       Nao valida a imagem publicada com "docker pull".
  -h, --help      Exibe esta ajuda.

Variaveis de ambiente:
  DOCKER_IMAGE         Imagem destino (default: docker.io/fplima/fin-kids-api).
  MVN_BIN              Binario Maven (default: ./mvnw).
EOF
}

print_cmd() {
  printf '+'
  for arg in "$@"; do
    printf ' %q' "$arg"
  done
  printf '\n'
}

run_cmd() {
  print_cmd "$@"
  if [[ "$DRY_RUN" == "true" ]]; then
    return 0
  fi
  "$@"
}

ensure_clean_git_tree() {
  if [[ "$ALLOW_DIRTY" == "true" ]]; then
    return 0
  fi

  if ! git -C "$ROOT_DIR" diff --quiet --ignore-submodules -- || \
     ! git -C "$ROOT_DIR" diff --cached --quiet --ignore-submodules --; then
    echo "Erro: diretorio com alteracoes pendentes. Use --allow-dirty para ignorar." >&2
    exit 1
  fi
}

validate_version() {
  local version="$1"
  if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][A-Za-z0-9._-]+)?$ ]]; then
    echo "Erro: versao invalida '$version'. Exemplo aceito: 0.2.0 ou 0.2.0-rc1." >&2
    exit 1
  fi
}

while (($# > 0)); do
  case "$1" in
    --skip-tests)
      SKIP_TESTS=true
      ;;
    --dry-run)
      DRY_RUN=true
      ;;
    --allow-dirty)
      ALLOW_DIRTY=true
      ;;
    --no-pull)
      VALIDATE_PULL=false
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    -*)
      echo "Opcao desconhecida: $1" >&2
      usage
      exit 1
      ;;
    *)
      if [[ -n "$NEW_VERSION" ]]; then
        echo "Erro: apenas uma versao pode ser informada." >&2
        usage
        exit 1
      fi
      NEW_VERSION="$1"
      ;;
  esac
  shift
done

if [[ -z "$NEW_VERSION" ]]; then
  usage
  exit 1
fi

validate_version "$NEW_VERSION"
ensure_clean_git_tree

if [[ ! -x "$MVN_BIN" ]]; then
  echo "Erro: Maven wrapper nao encontrado/executavel em '$MVN_BIN'." >&2
  exit 1
fi

CURRENT_VERSION="$("$MVN_BIN" -q -DforceStdout help:evaluate -Dexpression=project.version | tail -n 1 | tr -d '\r')"
echo "Versao atual: $CURRENT_VERSION"
echo "Nova versao:  $NEW_VERSION"

if [[ "$CURRENT_VERSION" != "$NEW_VERSION" ]]; then
  run_cmd "$MVN_BIN" -q versions:set "-DnewVersion=$NEW_VERSION" -DgenerateBackupPoms=false
else
  echo "Aviso: nova versao igual a atual; mantendo pom.xml sem alteracao de versao."
fi

if [[ "$SKIP_TESTS" == "false" ]]; then
  run_cmd "$MVN_BIN" verify
fi

run_cmd "$MVN_BIN" -Pjib-docker-build "-Ddocker.image=$DOCKER_IMAGE" -DskipTests package

if [[ "$VALIDATE_PULL" == "true" ]]; then
  if command -v docker >/dev/null 2>&1; then
    run_cmd docker pull "$DOCKER_IMAGE:$NEW_VERSION"
    run_cmd docker pull "$DOCKER_IMAGE:latest"
  else
    echo "Aviso: comando 'docker' nao encontrado; validacao de pull ignorada."
  fi
fi

if [[ "$DRY_RUN" == "true" ]]; then
  cat <<EOF
Simulacao de release concluida.
Tags planejadas:
- $DOCKER_IMAGE:$NEW_VERSION
- $DOCKER_IMAGE:latest
EOF
else
  cat <<EOF
Release local concluido.
Imagem publicada:
- $DOCKER_IMAGE:$NEW_VERSION
- $DOCKER_IMAGE:latest
EOF
fi

cat <<EOF
Proximos passos recomendados:
1) revisar alteracao de versao no pom.xml;
2) criar commit da release;
3) opcional: criar tag git "v$NEW_VERSION".
EOF
