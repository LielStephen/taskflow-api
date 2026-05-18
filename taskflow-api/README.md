# TaskFlow API

TaskFlow API is a standalone Java 25 project for managing work items with a lightweight REST API, JSON persistence, and zero external dependencies.

## Why this project is useful

- Clean layered structure: model, persistence, repository, service, and web packages.
- Works with the JDK alone: no Maven or Gradle required for local builds.
- Persists tasks to `data/tasks.json`.
- Supports filtering, status updates, and project summaries.
- Includes local test scripts and a GitHub Actions workflow.

## Features

- `GET /health`
- `GET /api/tasks`
- `POST /api/tasks`
- `GET /api/tasks/{id}`
- `PUT /api/tasks/{id}`
- `PATCH /api/tasks/{id}/status`
- `DELETE /api/tasks/{id}`
- `GET /api/summary`

### Query filters

`GET /api/tasks?status=IN_PROGRESS&priority=HIGH&tag=backend&overdue=true`

## Request examples

### Create a task

```bash
curl -X POST http://localhost:8080/api/tasks ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"Ship portfolio API\",\"description\":\"Finalize the urgent Java submission\",\"priority\":\"CRITICAL\",\"dueDate\":\"2026-05-20\",\"tags\":[\"portfolio\",\"backend\"]}"
```

### Update status

```bash
curl -X PATCH http://localhost:8080/api/tasks/<task-id>/status ^
  -H "Content-Type: application/json" ^
  -d "{\"status\":\"COMPLETED\"}"
```

## Local commands

```powershell
pwsh ./scripts/build.ps1
pwsh ./scripts/test.ps1
pwsh ./scripts/run.ps1
```

## Project structure

```text
src/
  main/java/com/lielstephen/taskflow/
  test/java/com/lielstephen/taskflow/
scripts/
.github/workflows/ci.yml
```

