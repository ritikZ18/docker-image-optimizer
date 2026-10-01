# ImageSmith Implementation Roadmap

This roadmap implements ImageSmith as an evidence-first Docker optimization system.
The model proposes hypotheses; Java policy and real build/test measurements decide whether a change succeeds.

## Current Baseline

Already implemented:

- Next.js frontend with public repository connection flow.
- Spring Boot API on Java 21.
- PostgreSQL through Docker Compose.
- Flyway schema management with V1 and V2 migrations.
- Repository cloning and Dockerfile inspection.
- Saved inspection sessions and JSON evidence.
- Session annotations with structured evidence.
- Language, framework, CI/CD, testing, and Docker asset detection.
- PostgreSQL optimization-rule knowledge base.
- Metadata-filtered retrieval capped at three rules.
- Deterministic Dockerfile findings.
- Rule-based optimization plan and policy validation.
- `start.sh --restart` that preserves PostgreSQL data.

Pending local changes should be committed before beginning Phase 1.

## Phase 0: Stabilize the Foundation

Goal: establish a reproducible baseline before adding external tools or model calls.

Tasks:

1. Run `mvn -q test` with Java 21.
2. Run `npm run build` in `frontend/`.
3. Run `./start.sh --restart` and verify API/frontend readiness.
4. Verify Flyway reaches the expected database version without deleting the volume.
5. Commit the current analyzer, planner, Flyway, and startup changes.

Acceptance:

- Existing inspection sessions survive `./start.sh --restart`.
- `GET /api/v1/inspection-sessions/{sessionId}/planning-context` returns JSON.
- `POST /api/v1/inspection-sessions/{sessionId}/plan` returns a policy decision.

Suggested commit:

```text
Stabilize Flyway startup and deterministic optimization pipeline
```

## Phase 1: Make the Job Workflow Real

Goal: turn `optimization_runs` into an asynchronous durable workflow.

Backend:

- Add `jobs` table through `V3__add_job_queue.sql`.
- Add job types: `INSPECT_REPOSITORY`, `ANALYZE_SOURCE`, `GENERATE_PLAN`, `VALIDATE_CANDIDATE`.
- Add job status: `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`.
- Claim jobs with PostgreSQL `FOR UPDATE SKIP LOCKED`.
- Add retry count, lease timeout, and failure message.
- Move repository inspection out of the HTTP request thread.

API:

- `POST /api/v1/optimization-runs` returns `202 Accepted`.
- `GET /api/v1/optimization-runs/{runId}/status` reports current stage.
- Add run event history for progress messages.

Acceptance:

- A run survives API restart.
- A failed job can retry without duplicating the run.
- Two workers cannot claim the same job.

## Phase 2: Build the Deterministic Tool Pack

Goal: produce authoritative evidence before any model call.

Implement adapters behind a common interface:

```java
interface AnalysisTool {
    ToolResult analyze(AnalysisInput input);
}
```

Tools:

- BuildKit: baseline image build and size metrics.
- Trivy: vulnerabilities and severity counts.
- Hadolint: Dockerfile lint findings.
- Dive: layer waste and duplicated bytes.
- Custom Java analyzers: stages, users, package managers, cache cleanup, and base-image pinning.

Store results in separate tables:

- `build_results`
- `scan_results`
- `lint_results`
- `layer_analysis_results`
- `analysis_findings`

Acceptance:

- Every finding has tool name, tool version, evidence reference, severity, and timestamp.
- Tool failure is represented as a failed result, not silently ignored.
- Results are cacheable by source commit, Dockerfile hash, tool version, and configuration hash.

## Phase 3: Improve Knowledge Retrieval

Goal: make retrieval precise and explainable before adding embeddings.

Tasks:

- Add rule applicability fields for runtime, category, base image, and required evidence.
- Add PostgreSQL full-text search over rule description and applicability.
- Rank by runtime match, finding category, and text relevance.
- Return only the top two or three rules.
- Store knowledge-base version in every planning request.
- Add a rule administration/import path instead of hard-coding future rules.

Acceptance:

- Java evidence retrieves `JAVA-004` and Docker runtime rules.
- Python wheel evidence retrieves `PYTHON-003`.
- Retrieved rules include source URL and documentation version.
- Retrieval tests prove irrelevant rules are excluded.

## Phase 4: Add the Model REST Client

Goal: add GPT-6 Luna behind the existing `OptimizationPlanner` interface.

Tasks:

- Add an HTTP client with configurable base URL and model name.
- Read the API key only from an environment variable or secret store.
- Use strict structured JSON output matching `OptimizationPlan`.
- Set input/output token limits.
- Add request timeout, retry policy, and circuit breaker.
- Record model, prompt version, evidence hash, token usage, latency, and outcome.
- Keep `RuleBasedOptimizationPlanner` as fallback when the model is disabled or unavailable.

Configuration:

```text
AI_ENABLED=false
AI_MODEL=gpt-6-luna
AI_API_BASE_URL=...
AI_API_KEY=...
AI_MAX_INPUT_TOKENS=2500
AI_MAX_OUTPUT_TOKENS=400
```

Acceptance:

- No API key is logged.
- Invalid model output is rejected by Java validation.
- The system falls back deterministically when AI is disabled.
- A normalized evidence hash can reuse a previous plan.

## Phase 5: Policy Engine and Candidate Generation

Goal: convert approved plan actions into safe, reviewable Dockerfile candidates.

Tasks:

- Define allowed transformation types and required evidence.
- Reject unsupported transformations, missing evidence, unsafe base-image changes, and low-confidence plans.
- Generate a candidate patch without mutating the source repository.
- Store candidate, diff, parent session, rule IDs, and planner metadata.
- Add approve/reject endpoints.

API:

- `GET /api/v1/optimization-runs/{runId}/candidates`
- `GET /api/v1/candidates/{candidateId}`
- `POST /api/v1/candidates/{candidateId}/approve`
- `POST /api/v1/candidates/{candidateId}/reject`

Acceptance:

- Every candidate is reproducible from source commit plus transformation data.
- No candidate runs unless policy validation passes.
- Human approval is required before publishing or applying a candidate.

## Phase 6: Build and Validate Candidates

Goal: prove optimization claims with experiments, not model confidence.

For each candidate:

1. Create an isolated workspace.
2. Apply the candidate patch.
3. Build with BuildKit.
4. Run the configured test command.
5. Run Trivy, Hadolint, and layer analysis.
6. Compare baseline and candidate metrics.
7. Store pass/fail and regression reasons.

Validation gates:

- Build succeeds.
- Required tests pass.
- Critical vulnerability count does not regress.
- Runtime smoke test passes.
- Image size improves or meets the user objective.
- Policy checks remain satisfied.

Acceptance:

- A candidate cannot be marked successful from model output alone.
- Every metric comparison references baseline and candidate artifacts.
- Failed validation preserves logs and tool evidence.

## Phase 7: Frontend Workflow

Goal: expose the durable workflow without turning the application into a chatbot.

Views:

- Repository/session history.
- Run progress timeline.
- Evidence and findings explorer.
- Retrieved knowledge references.
- Candidate diff viewer.
- Validation comparison: size, vulnerabilities, tests, and runtime.
- Approve/reject controls.

UX rules:

- Show evidence source beside every finding.
- Clearly distinguish `DETECTED`, `MISSING`, `FAILED`, and `NOT_RUN`.
- Keep AI rationale secondary to measured results.
- Keep the WebGPU analysis surface decorative and accessible; never put critical data only on canvas.

Acceptance:

- A user can start, monitor, inspect, approve, reject, and revisit a run.
- Refreshing the browser does not lose progress or saved evidence.

## Phase 8: Evaluation Dataset and Cost Controls

Goal: measure whether AI adds value over deterministic rules.

Create a versioned dataset of 30 to 50 Dockerfiles covering:

- Java
- Python
- Node.js
- Go
- Multi-stage and single-stage builds
- Security and caching issues
- Repositories with no Dockerfile

Compare:

1. Deterministic rules only.
2. Rules plus retrieval plus small model.
3. Rules plus retrieval plus stronger fallback model.

Metrics:

- Valid plan rate
- Successful candidate build rate
- Image-size improvement
- Security regression rate
- Runtime regression rate
- Input/output tokens
- Latency
- Cost per validated optimization

Acceptance:

- Model escalation is based on measured failure or ambiguity.
- Cached plans reuse the evidence hash and prompt/model version.
- The more expensive model is justified by evaluation results.

## Recommended Execution Order

Implement in this order:

1. Commit and stabilize the current work.
2. Add the PostgreSQL job queue.
3. Move inspection and analysis to workers.
4. Add BuildKit and scanner adapters.
5. Improve retrieval and rule administration.
6. Add the REST model client behind `OptimizationPlanner`.
7. Add candidate patches and policy gates.
8. Build and validate candidates experimentally.
9. Finish the run-oriented frontend.
10. Run the evaluation dataset and tune cost/escalation rules.

Do not add vector embeddings, fine-tuning, or GPU hosting before the evaluation phase demonstrates a measurable need.