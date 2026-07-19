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

# Run CK metrics (requires ck tool: brew install ck)
metrics-ck:
    ck Code/backend/src/main/java --output metrics-ck.csv

# Run SonarQube analysis (requires sonar-scanner or Maven plugin)
metrics-sonar:
    cd Code/backend && ./mvnw sonar:sonar -Dsonar.host.url=http://localhost:9000

# Run all checks
check: format lint test
