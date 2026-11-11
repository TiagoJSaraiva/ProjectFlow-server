# ProjectFlow Server

[![CI](https://github.com/TiagoJSaraiva/ProjectFlow-server/actions/workflows/ci.yml/badge.svg)](https://github.com/TiagoJSaraiva/ProjectFlow-server/actions/workflows/ci.yml)

**Read this in other languages:** [Português (Brasil)](LEIA-ME.md)

## Table of contents

1. [Overview](#overview)
2. [Features](#features)
3. [Stack and architecture](#stack-and-architecture)
4. [Data model](#data-model)
5. [API endpoints](#api-endpoints)
6. [Prerequisites](#prerequisites)
7. [Environment variables](#environment-variables)
8. [Running with Docker Compose](#running-with-docker-compose)
9. [Running without Docker](#running-without-docker)
10. [Database](#database)
11. [Tests](#tests)
12. [Continuous integration](#continuous-integration)
13. [Deployment](#deployment)
14. [Implementation notes and known limitations](#implementation-notes-and-known-limitations)
15. [Credits](#credits)

## Overview

ProjectFlow is a flowchart diagram editor: users sign up, create projects and draw diagrams
(rectangle and circle nodes joined by connections) on a canvas. **This repository is the REST API**
(Spring Boot) that stores accounts, projects and diagrams.

- Frontend (Angular): <https://github.com/TiagoJSaraiva/ProjectFlow-webapp>
- Public demo: <https://tiagojsaraiva.github.io/ProjectFlow-webapp/>

> The public demo runs in a **demonstration mode entirely in the browser** and does **not** use this
> server. There is no hosted instance of this API; to use the real frontend-plus-API combination you
> run it locally (see below).

The server code lives in `ProjectFlow-server/`; the repository root holds the Docker Compose file,
the CI workflow and this documentation.

## Features

- Sign-up with e-mail verification (token valid for 60 minutes) and login that returns a JWT (24 h).
- Password recovery by e-mail (reset token valid for 30 minutes) and password change for the logged-in user.
- Projects per user: create, list, read, rename/describe, delete. Each project has one diagram.
- Diagram persistence: nodes (`RECTANGLE`, `CIRCLE`; name up to 35 characters), connections with a bend
  point and style, and the viewport (zoom, offset, grid size). Saving replaces the whole diagram.
- Diagram limits: at most 500 nodes and 1,000 connections, no repeated ids in one save.
- Per-project `ultimoAcesso` (updated when the diagram is loaded) and `ultimaAlteracao` (updated when saved).
- Ownership enforcement: every project, diagram and user operation is restricted to the owner of the JWT;
  other users' resources answer `404`.
- Account deletion that removes all the user's data in one transaction.
- Account updates: only the name can be changed (the e-mail is fixed); a password change requires the current password.
- Tokens issued before a password change or reset stop being accepted.
- Optional SMTP: without it the verification/recovery links are written to the server log.
- Dates are serialised as ISO-8601 strings and generated in UTC.
- A legacy `/api/elements` API (old element model) is still present; the frontend does not use it.

## Stack and architecture

| Item | Choice |
|---|---|
| Language / build | Java 21, Maven Wrapper (`mvnw`) |
| Framework | Spring Boot 3.3.2 (Web, Data JPA, Security, Mail, Validation) |
| Database | MySQL 8 (schema created by Hibernate `ddl-auto=update`) |
| Authentication | Stateless JWT (jjwt 0.11.5, HS512), BCrypt password hashes |
| Tests | JUnit 5, Spring Boot Test, MockMvc, H2 in MySQL mode |

Package `com.polo42.project_flow`, in the usual layers:

- `controller`: REST controllers (`AuthController`, `UserController`, `ProjectController`,
  `DiagramController`, `ElementController`).
- `service`: business rules, ownership checks (`ProjectService`, `DiagramService`, `UserService`,
  `AuthService`, `EmailVerificationService`, `PasswordResetService`, `MailService`, `ElementService`).
- `repository`: Spring Data JPA repositories.
- `model` / `dto`: JPA entities and request/response objects.
- `security`: `SecurityConfig`, `JwtAuthenticationFilter`, `JwtTokenProvider`, JSON handlers for 401/403.
- `exception`: `GlobalExceptionHandler`, which returns a JSON body with a `message` field.
- `config`: CORS and Jackson configuration.

How the main flows work:

- **JWT**: `POST /api/auth/login` returns a token whose subject is the user id. The filter loads the user
  on each request. The token also carries a `pv` claim with the user's last password-change timestamp
  (epoch milliseconds, `0` if never changed); a mismatch rejects the token with `401`.
- **E-mail verification and password recovery**: random UUID tokens are stored in the database
  (`email_verification_tokens`, `password_reset_tokens`) and sent as links built from `FRONTEND_BASE_URL`
  (`/verify-email?token=...`, `/reset?token=...`). Tokens are single-use.
- **Optional e-mail**: `MailService` sends through SMTP only when `MAIL_HOST` is set. Otherwise it logs a
  warning containing the whole message, including the link or code, and does not send anything.
- **Errors**: `{"message": "..."}` is always present; see the status codes below.
- **Time**: the JVM default time zone is forced to UTC and Hibernate uses `jdbc.time_zone=UTC`.

## Data model

| Table | Purpose |
|---|---|
| `usuarios` | Accounts: name, unique e-mail, BCrypt hash, verified flag, `senha_alterada_em` |
| `projetos` | Projects: name, description, created / last changed / last accessed, owner |
| `diagram_nodes` | Nodes: client-generated UUID id, type, `nome` (max 35), description, position, size/radius |
| `diagram_connections` | Connections between two nodes, bend point and style (JSON) |
| `diagram_view_states` | Viewport of a project (one row per project) |
| `email_verification_tokens` | E-mail verification tokens |
| `password_reset_tokens` | Password reset tokens |
| `elementos` | Legacy element model (used only by `/api/elements`) |

Foreign keys have no database-level `ON DELETE CASCADE`; the application deletes dependent rows itself.

## API endpoints

Base URL: `http://localhost:8080`. Protected endpoints require `Authorization: Bearer <jwt>`.
Resources owned by another user answer `404`. Dates are ISO-8601 strings without a time zone, in UTC.

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | Sign up (`nome`, `email`, `senha` of at least 6 characters); sends the verification e-mail |
| POST | `/api/auth/login` | public | Log in (`email`, `senha`); returns `token`, `userId`, `nome`, `email`. Wrong credentials or unverified e-mail: `422` |
| POST | `/api/auth/verify-email` | public | Confirm e-mail with `token` |
| POST | `/api/auth/resend-verification` | public | Resend the verification e-mail (`email`); always `200` with an empty body, whether or not the account exists or is already verified |
| POST | `/api/auth/recover` | public | Request a password reset e-mail (`email`) |
| POST | `/api/auth/reset-password` | public | Set a new password (`token`, `novaSenha`) |
| GET | `/api/users` | JWT | List users: returns only the caller |
| GET | `/api/users/{id}` | JWT | Own account data |
| PUT | `/api/users/{id}` | JWT | Update own name (`nome`, 2 to 100 characters). `email` may be sent only if equal to the current one; changing it answers `422` |
| PUT | `/api/users/{id}/password` | JWT | Change own password: JSON `{"senhaAtual", "novaSenha"}` (new password 6 to 100 characters); wrong current password `422`; returns `204` and invalidates existing tokens |
| DELETE | `/api/users/{id}` | JWT | Delete own account and all its data (`204`) |
| GET | `/api/projects/user/{userId}` | JWT | List own projects (`userId` must be the caller) |
| POST | `/api/projects/user/{userId}` | JWT | Create a project (`nome` required, up to 100; `descricao` up to 10,000) |
| GET | `/api/projects/{id}` | JWT | Read a project |
| PUT | `/api/projects/{id}` | JWT | Update name and description |
| DELETE | `/api/projects/{id}` | JWT | Delete a project, its diagram and elements (`204`) |
| GET | `/api/projects/{projectId}/diagram` | JWT | Load the diagram; updates `ultimoAcesso` |
| PUT | `/api/projects/{projectId}/diagram` | JWT | Replace the diagram and save project name/description; updates `ultimaAlteracao` |
| GET, POST | `/api/elements/project/{projectId}` | JWT | Legacy: list / create elements |
| GET, PUT, DELETE | `/api/elements/{id}` | JWT | Legacy: read / update / delete an element |
| PUT | `/api/elements/{id}/content`, `/coordinates`, `/relations` | JWT | Legacy: partial updates (plain-text body) |

Error status codes (all bodies contain `message`):

| Status | Meaning |
|---|---|
| 400 | Invalid request: DTO validation (one field per error plus `message`), malformed JSON, invalid UUID or enum |
| 401 | Missing, invalid or expired token, or token issued before the last password change |
| 403 | Access denied |
| 404 | Resource not found, owned by someone else, or unknown route |
| 405 | HTTP method not supported |
| 415 | Unsupported content type |
| 422 | Business rule: e-mail in use, invalid token, e-mail change attempt, wrong current password, node name over 35 characters, repeated ids or too many nodes/connections in a diagram, node/connection id already used by another project, login refused |
| 500 | Unexpected error (logged) |

The same contract is described in `ProjectFlow-server/ENDPOINTS - PROJECTFLOW.txt`.

## Prerequisites

- Docker with Compose, **or**
- JDK 21 and a MySQL 8 server (the Maven Wrapper downloads Maven itself).

## Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `JWT_SECRET` | yes | none | JWT signing secret, at least 64 characters, generated by you (`openssl rand -base64 64`). The server refuses to start if it is empty, short, an example value or a secret that was published in this repository's early history |
| `DB_USERNAME` | yes | none | Database user |
| `DB_PASSWORD` | yes | none | Database password |
| `DB_URL` | no | `jdbc:mysql://localhost:3306/project_flow?...` | JDBC URL (the compose file sets it to the `db` service) |
| `DB_ROOT_PASSWORD` | compose only | none | MySQL root password for the compose database container |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:4200` | Comma-separated allowed origins |
| `FRONTEND_BASE_URL` | no | `http://localhost:4200` | Base URL used in e-mail links |
| `SERVER_PORT` | no | `8080` | Host port published by Docker Compose |
| `MAIL_HOST` | no | empty | SMTP host; empty disables sending (links go to the log) |
| `MAIL_PORT` | no | `587` | SMTP port |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | no | empty | SMTP credentials |
| `MAIL_FROM` | no | `MAIL_USERNAME` or `no-reply@projectflow.local` | Sender address |
| `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | no | `true` | SMTP options |

`.env.example` at the repository root lists the variables with fictitious values. Copy it to `.env`
(never committed) and edit it.

## Running with Docker Compose

```bash
cp .env.example .env
# edit .env: set the DB passwords and a JWT_SECRET of your own, e.g. the output of:
#   openssl rand -base64 64
docker compose up --build
```

`JWT_SECRET` is intentionally empty in `.env.example`: Compose and the server both refuse to start until you fill it in.

This starts MySQL 8 (no port published on the host, so it cannot clash with a local MySQL) and the server on
`http://localhost:8080`. Without `MAIL_HOST`, read the verification and recovery links with
`docker compose logs server`.

> The Dockerfile and `docker-compose.yml` were written for this repository, but **they have not been executed
> end to end**; only `docker compose config` was used to validate the syntax.

## Running without Docker

1. Create the database: `CREATE DATABASE project_flow;`.
2. Provide the variables. Either copy `.env.example` to `ProjectFlow-server/.env` and fill it in (read from the
   working directory when the application starts through `main`, for example `spring-boot:run` or `java -jar`) or
   export them in the shell; a variable already set in the environment takes precedence over `.env`.
   `DB_USERNAME`, `DB_PASSWORD` and `JWT_SECRET` are required.
3. Start the server:

```bash
cd ProjectFlow-server
./mvnw spring-boot:run
```

Configure the Angular frontend to use `http://localhost:8080` as the API URL.

## Database

Tables are created and extended by Hibernate (`spring.jpa.hibernate.ddl-auto=update`); there are no migration
scripts. The old `schema.sql` was removed because MySQL never executed it and it no longer matched the entities.

`ddl-auto=update` adds new tables and columns but does not change existing ones. A MySQL database created
by an earlier version needs:

```sql
-- node names now accept up to 35 characters (was 20)
ALTER TABLE diagram_nodes MODIFY nome VARCHAR(35) NOT NULL;
-- added automatically by Hibernate; manual equivalent if you prefer
ALTER TABLE usuarios ADD COLUMN senha_alterada_em DATETIME(6) NULL;
```

Timestamps already stored by earlier versions were written in the server's local time zone; new ones are UTC.

## Tests

```bash
cd ProjectFlow-server
./mvnw test
```

The tests are MockMvc integration tests that start the whole application with the `test` profile: H2 in memory
in MySQL mode, a test-only JWT secret and a mocked `JavaMailSender`. No MySQL, SMTP or Docker is needed.
They cover the context, ownership of every endpoint with two users, one test per fixed bug (diagram persistence,
account deletion, HTTP status codes, ISO dates, 35-character node names), the e-mail fallback and SMTP paths,
UTC dates, token invalidation after a password change, user update and password validation, diagram limits,
verification and reset tokens (expired, reused), unverified login, forged and expired JWTs, `resend-verification`,
and the startup rejection of empty, short or example JWT secrets.

## Continuous integration

`.github/workflows/ci.yml` runs `./mvnw -B test` with JDK 21 (working directory `ProjectFlow-server`) on every
push and pull request to `main`.

## Deployment

There is **no public instance** of this API. The public demo of the frontend does not call it. Hosting it would require:

- a machine or container platform able to run a JVM (the `Dockerfile` builds a runnable image);
- a managed or self-hosted MySQL 8 database;
- a strong `JWT_SECRET`, database credentials and `CORS_ALLOWED_ORIGINS` set to the frontend origin, kept as secrets;
- HTTPS, because an HTTPS frontend cannot call an HTTP API;
- an SMTP provider (or accepting that links only appear in the log) and `FRONTEND_BASE_URL` pointing at the frontend;
- the frontend configured with the API URL, and, before exposing it publicly, the limitations below addressed.

## Implementation notes and known limitations

- The diagram is saved as a whole: the existing rows are deleted and re-inserted. There is no optimistic
  locking, so concurrent saves of the same project overwrite each other.
- Node and connection ids are generated by the client. A save that reuses an id belonging to another project is rejected with `422`.
- Ownership failures return `404` (not `403`) so ids of other users' resources are not revealed.
- Password change or reset invalidates every earlier token, including the caller's own; the client must log in again.
- Login failures use `422` with a generic message, not `401`.
- No rate limiting on login, sign-up, recovery or resend. Sign-up still answers `422` "e-mail already in use", which
  reveals that an address is registered (recovery and resend answer the same way for known and unknown addresses).
- Verification and reset tokens are stored in plain text; used tokens are not purged.
- JWTs last 24 hours; there is no refresh token and no logout/revocation other than a password change.
- In development mode (no SMTP) the log contains verification and recovery links: do not use that mode with real users.
- Schema management relies on `ddl-auto=update`; there are no versioned migrations.
- The legacy `/api/elements` API and `elementos` table are unused by the frontend.
- The Docker setup has not been run end to end, and the GitHub workflow depends on the hosted runner.

## Credits

Written by **Tiago J. Saraiva** (author of all commits in this repository's history).
