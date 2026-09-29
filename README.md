# ImageSmith

Evidence-driven Docker image optimization platform.

## Stack

- Frontend: Next.js and TypeScript
- Backend: Java 21 and Spring Boot
- Persistence, queue, and cache: PostgreSQL
- AI: stateless instruction model through a REST client
- Analysis: BuildKit, Trivy, Hadolint, Dive, and custom analyzers

## Run PostgreSQL

```bash
docker compose up -d postgres
```

## Run the API

```bash
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080`.

## API vertical slice

Create a repository:

```bash
curl -X POST http://localhost:8080/api/v1/repositories \\
  -H 'Content-Type: application/json' \\
  -d '{"name":"example","url":"https://github.com/example/example"}'
```

Create an asynchronous optimization run with the returned `repositoryId`:

```bash
curl -X POST http://localhost:8080/api/v1/optimization-runs \\
  -H 'Content-Type: application/json' \\
  -d '{"repositoryId":"REPOSITORY_ID"}'
```

## Architecture boundary

`OptimizationContext` is the evidence object passed to the AI planner. `OptimizationPlan` is structured output. The model proposes hypotheses; deterministic policy checks and measured validation decide whether a candidate succeeds.
