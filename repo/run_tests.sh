#!/usr/bin/env bash
set -euo pipefail

if command -v mvn >/dev/null 2>&1; then
  mvn clean verify
  exit 0
fi

export MSYS2_ARG_CONV_EXCL='*'

if command -v cygpath >/dev/null 2>&1; then
  host_pwd="$(cygpath -w "$(pwd)")"
else
  host_pwd="$(pwd)"
fi

docker run --rm \
  -v "${host_pwd}:/workspace" \
  -w "/workspace" \
  -e "DOCKER_HOST=${DOCKER_HOST:-tcp://host.docker.internal:2375}" \
  -e "TESTCONTAINERS_HOST_OVERRIDE=${TESTCONTAINERS_HOST_OVERRIDE:-host.docker.internal}" \
  maven:3.9.9-eclipse-temurin-17 \
  mvn clean verify

