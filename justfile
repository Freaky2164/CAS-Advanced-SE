# Format all code
format:
    cd Code/backend && ./mvnw spotless:apply
    cd Code/frontend && npm run format

# Lint all code
lint:
    cd Code/backend && ./mvnw spotless:check
    cd Code/frontend && npm run lint

# Run tests
test:
    cd Code/backend && ./mvnw test
    cd Code/frontend && npm test

# Start the full application (DB + Backend + Frontend)
up:
    cd Code/backend && APP_ADMIN_PASSWORD=admin docker compose up -d --build
    echo "Application starting at https://localhost"
    echo "Admin credentials: admin / admin"

# Stop the full application
down:
    cd Code/backend && docker compose down

# Run CK metrics via Maven plugin
metrics-ck:
    mkdir -p Code/backend/metrics-ck
    cd Code/backend && ./mvnw exec:java@ck

# Start SonarQube locally (Docker Compose)
sonar-up:
    docker compose -f Code/backend/docker-compose.sonarqube.yml up -d
    echo "SonarQube starting at http://localhost:9000"
    echo "Default credentials: admin/admin (change on first login)"
    echo "Wait 2-3 minutes for startup"

# Stop SonarQube
sonar-down:
    docker compose -f Code/backend/docker-compose.sonarqube.yml down

# Run SonarQube analysis (auto-generates token on first run)
metrics-sonar:
    #!/usr/bin/env bash
    set -euo pipefail
    TOKEN_FILE=".sonar-token/sonar-token"
    if [ ! -f "$TOKEN_FILE" ]; then
      echo "Generating SonarQube analysis token..."
      mkdir -p .sonar-token
      TOKEN=$(curl -s -u admin:admin -X POST "http://localhost:9000/api/user_tokens/generate?name=ci-token-$(date +%s)&type=GLOBAL_ANALYSIS_TOKEN" | jq -r .token)
      echo -n "$TOKEN" > "$TOKEN_FILE"
      echo "Token saved to $TOKEN_FILE"
    fi
    cd Code/backend && ./mvnw sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=$(cat ../../.sonar-token/sonar-token)

# Run all checks
check: format lint test
