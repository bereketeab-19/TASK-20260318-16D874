$ErrorActionPreference = "Stop"

mvn -v *> $null
if ($LASTEXITCODE -eq 0) {
  mvn clean verify
  exit $LASTEXITCODE
}

docker run --rm `
  -v "${PWD}:/workspace" `
  -w /workspace `
  -e "DOCKER_HOST=$($env:DOCKER_HOST ?? 'tcp://host.docker.internal:2375')" `
  -e "TESTCONTAINERS_HOST_OVERRIDE=$($env:TESTCONTAINERS_HOST_OVERRIDE ?? 'host.docker.internal')" `
  maven:3.9.9-eclipse-temurin-17 `
  mvn clean verify

