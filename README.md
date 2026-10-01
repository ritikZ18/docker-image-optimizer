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

## Start the application

Start PostgreSQL, the API, and the frontend with readiness checks:

```bash
./start.sh
```

Restart existing API and frontend processes before starting. PostgreSQL data is preserved:

```bash
./start.sh --restart
```

Reset the database only when explicitly required:

```bash
./start.sh --reset-db
```

Stop the application processes with `Ctrl+C`. Logs are written to `.run/`.

## Run the API

```bash
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080`.

## API vertical slice

Register an existing public repository:

```bash
curl -X POST http://localhost:8080/api/v1/repositories \\
  -H 'Content-Type: application/json' \\
  -d '{"name":"example","url":"https://github.com/example/example"}'
```

Inspect its Dockerfiles:

```bash
curl -X POST http://localhost:8080/api/v1/repositories/REPOSITORY_ID/inspect \
  -H 'Content-Type: application/json' \
  -d '{"sessionName":"baseline review"}'
```

List saved inspection sessions:

```bash
curl http://localhost:8080/api/v1/repositories/REPOSITORY_ID/sessions
```

Load a saved evidence payload:

```bash
curl http://localhost:8080/api/v1/repositories/sessions/SESSION_ID
```

Add an annotation to a saved session:

```bash
curl -X POST http://localhost:8080/api/v1/inspection-sessions/SESSION_ID/annotations \
  -H 'Content-Type: application/json' \
  -d '{"category":"CONTAINERS","severity":"HIGH","filePath":"Dockerfile","lineNumber":1,"title":"No pinned base image","detail":"Base image should use an immutable digest.","evidenceJson":"{\"source\":\"dockerfile\",\"reference\":\"Dockerfile:1\"}"}'
```

List annotations:

```bash
curl http://localhost:8080/api/v1/inspection-sessions/SESSION_ID/annotations
```

Preview the compact grounded context prepared for the reasoning model:

```bash
curl http://localhost:8080/api/v1/inspection-sessions/SESSION_ID/planning-context
```

Run deterministic source analysis and a policy-checked dry-run plan:

```bash
curl http://localhost:8080/api/v1/inspection-sessions/SESSION_ID/findings
curl -X POST http://localhost:8080/api/v1/inspection-sessions/SESSION_ID/plan
```

Create an asynchronous optimization run with the returned `repositoryId`:

```bash
curl -X POST http://localhost:8080/api/v1/optimization-runs \\
  -H 'Content-Type: application/json' \\
  -d '{"repositoryId":"REPOSITORY_ID"}'
```

## Architecture boundary

`OptimizationContext` is the evidence object passed to the AI planner. `OptimizationPlan` is structured output. The model proposes hypotheses; deterministic policy checks and measured validation decide whether a candidate succeeds.
