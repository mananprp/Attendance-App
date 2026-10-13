# Attendance App: Development Plan for Antigravity (Multi-Agent Build)

**Project:** Face-verified attendance app (Android) + backend
**Companion document:** `Attendance_App_PRD.docx` (requirement IDs FR-01 to FR-34 are referenced below)
**Goal:** A complete, working implementation. No stubs, no TODO placeholders, no mock data in production paths.

---

## 0. How to use this plan

1. Start one **Orchestrator agent** (Agent 0). It owns the contracts (API spec, database schema, shared models) and merges everyone's work.
2. Agent 0 finishes **Wave 0** (contracts and scaffolding) first. Nothing else starts until the contracts are committed.
3. Then run the **Wave 1 agents in parallel**, each in its own branch and its own folder (see ownership in section 4).
4. Agents must not edit files they do not own. If a contract must change, the agent writes a request to `docs/CONTRACT_CHANGES.md` and Agent 0 decides.
5. Every agent finishes by running its own tests and filling in its section of `docs/PROGRESS.md` (what was built, what was tested, known gaps).
6. Agent 0 merges branch by branch, runs the full test suite, and fixes integration issues.

Rules for every agent:
- Implement fully. If something is genuinely impossible, stop and record it in `docs/PROGRESS.md` under "Blocked", instead of faking it.
- Follow the business rules in section 2 exactly. Do not invent extra features.
- Keep code readable: small files, clear names, comments only where the reason is not obvious.
- Never hard-code office coordinates, radius, sync windows, or server URLs. They come from config.

---

## 1. Product summary

An Android app where an employee punches in and punches out once per day. A punch is allowed only within 100 m of the office point, and only after an on-device face match against the profile photo. Punches are saved locally and uploaded to the server in scheduled off-peak windows (with random per-device offset), or immediately via a Sync Now button. A calendar page shows the month's records; tapping a date shows that day's times.

---

## 2. Locked decisions and business rules

**Platform**
- Android 8.0 (API 26) minimum, arm64-v8a only.
- On-device database: SQLite (through Room). Server database: MySQL.

**Onboarding**
- Login with user ID + password. Accounts are created by an admin (seed script or CLI). There is no self sign-up.
- After login: capture a live profile photo (camera permission requested here), upload it as photo version 1, then request location permission.
- Only two permissions: camera and location. **No storage permission** (use app-private storage).

**Profile photo rule**
- The user may change the photo **exactly once** (version 2). Both photos stay in the database.
- The rule is enforced in the app and on the server. The server is the source of truth.

**Punching**
- Two buttons: Punch In and Punch Out. Each can be used once per day. After use, the button is blocked. No duplicates.
- Punch Out is enabled only after a Punch In that day.
- Day window is 00:00 to 23:59 in office-location time. At 00:00 the buttons reset for the next day.
- Flow: check geofence (100 m) first. If outside, refuse and show the distance. If inside, open the camera, run the face pipeline, and on success record the trusted time.
- If a day has Punch In but no Punch Out: at the user's **next-day first punch**, that earlier day is marked `MISSED_PUNCH` and its punch-out time is set equal to its punch-in time.
- The 8.5-hour shift logic is **removed for now**. Do not implement hours calculations.

**Trusted time**
- Online: time from NTP (SNTP), converted to the office timezone from server config. The office timezone is a config value (for example `Asia/Kolkata`).
- Offline: fall back to the **device clock** and set `time_unverified = true` on that punch.
- Store the time source (`NTP` or `DEVICE`) with each punch.

**Face verification (on device, ONNX Runtime)**
- Models: `det_10g.onnx` (detection, ~17 MB), `1k3d68.onnx` (3D landmarks used for liveness, ~144 MB), `w600k_r50.onnx` (recognition, ~174 MB). The `genderage` and `2d106det` models are **not used**.
- Models are downloaded on first launch, not bundled. Show progress, allow resume, verify checksums.
- Load models one at a time and release them after use to keep peak memory low.
- The match threshold starts at 55% and must be tunable from config.
- Server re-verification is **not** built now. But the captured image(s) for each punch are stored and uploaded, so it can be added later.

**Sync**
- Every punch is saved locally with status `PENDING`.
- WorkManager runs sync in server-configured windows (initial: around 16:00 and 00:00) with a random per-device offset (up to 30 minutes, configurable).
- A Sync Now button uploads immediately.
- Uploads are idempotent using a device-generated `punch_uuid`. The server ignores repeats.
- Failed uploads retry with backoff. Records stay `PENDING` until acknowledged.

**Deferred (do not build)**: shift hours, server re-verification, night shifts, device binding, password reset, GPS accuracy filtering, admin dashboard, multiple office sites.

---

## 3. Tech stack

**Android**
- Kotlin, Jetpack Compose, Material 3 (light theme only)
- MVVM with unidirectional state (ViewModel + StateFlow)
- Hilt for dependency injection
- Room (SQLite), DataStore for small settings, EncryptedSharedPreferences or Keystore-backed storage for the session token
- CameraX (preview + image analysis)
- ONNX Runtime for Android
- FusedLocationProviderClient (Google Play services location)
- WorkManager for scheduled and manual sync
- Retrofit + OkHttp + Kotlin serialization
- SNTP client with several fallback NTP hosts (implement a small one or use a maintained library)
- Tests: JUnit, MockK, Turbine, Compose UI tests, Room in-memory tests

**Backend (proposed; change if you prefer another stack)**
- Node.js + NestJS (TypeScript), MySQL, TypeORM or Prisma with migrations
- JWT auth, argon2 or bcrypt password hashing (passwords are never stored in plain text)
- Image storage behind an interface (local disk for development, S3-compatible for production)
- OpenAPI spec generated and committed
- Tests: Jest unit + e2e (supertest)

---

## 4. Repository layout and ownership

```
/android            Android app (Kotlin)
/server             Backend (NestJS)
/docs
  openapi.yaml      API contract (owned by Agent 0)
  schema.sql        MySQL schema (owned by Agent 0)
  room-schema.md    SQLite/Room entities (owned by Agent 0)
  PROGRESS.md       Each agent updates its own section
  CONTRACT_CHANGES.md
  TEST_MATRIX.md    Manual device test matrix (owned by Agent 8)
/tools              Model parity scripts and helper scripts
```

Android package layout (Agent 1 creates the skeleton; others fill their own packages):

```
com.company.attendance/
  core/        (Agent 1: DI, network, database, config, time provider interface)
  design/      (Agent 2: theme, components)
  feature/auth/        (Agent 3)
  feature/onboarding/  (Agent 3)
  feature/punch/       (Agent 4)
  face/                (Agent 5)
  sync/                (Agent 6)
  feature/calendar/    (Agent 7)
```

---

## 5. Agent roster

### Agent 0: Orchestrator and Architect
**Owns:** `/docs/*` contracts, repo scaffolding, merging, integration.
**Wave 0 deliverables (before anyone else starts):**
1. `docs/openapi.yaml` covering every endpoint in section 6.
2. `docs/schema.sql` (MySQL) and `docs/room-schema.md` (SQLite entities) matching section 7.
3. Empty but buildable `android` and `server` projects (Gradle and Nest scaffolds, CI script that builds both).
4. Shared interface definitions the Android agents code against: `TrustedTimeProvider`, `FaceVerifier`, `LocationChecker`, `PunchRepository`, `SyncScheduler` (signatures and data classes only, documented).
5. `docs/PROGRESS.md` template.
**Later:** merge order, integration testing, resolving contract change requests, final end-to-end run.

### Agent 1: Android Core
**Owns:** `android/.../core/`, Gradle setup.
**Builds:** Hilt modules, Room database and DAOs (tables in section 7), Retrofit client with auth interceptor and error mapping, secure token storage, remote config fetch and cache (`/config`), base error and result types, the `TrustedTimeProvider` implementation (SNTP with fallback hosts, monotonic anchoring, device-clock fallback flagged as unverified, conversion to office timezone and the office-day boundary logic).
**Done when:** unit tests pass for the time provider (online, offline, day boundary at 23:59 and 00:00, timezone conversion) and for all DAOs.

### Agent 2: Design System and Navigation
**Owns:** `android/.../design/`, navigation graph.
**Builds:** the theme and components exactly as in section 8 (colors, typography, spacing, buttons, cards, top bar, calendar cell, status chip, empty/error/loading states), plus the navigation graph and bottom navigation between Home and Calendar.
**Done when:** a component gallery preview screen renders every component, and Compose previews exist for all.

### Agent 3: Auth and Onboarding
**Owns:** `feature/auth/`, `feature/onboarding/`.
**Builds:** login screen; onboarding steps (welcome, camera permission, live photo capture with single-face check, upload, location permission, done); photo-change flow (available exactly once; the entry point hides after use; server response is authoritative); session handling and logout; first-login history pull from the server into SQLite (FR-23).
**Depends on:** Agent 1 (network, storage), Agent 2 (components), Agent 5 (detector for the single-face check and embedding for the reference).
**Done when:** the full flow works against the real backend, permission-denied paths are handled, and the photo-once rule is verified from the app and rejected by the server.

### Agent 4: Punch Flow
**Owns:** `feature/punch/`.
**Builds:** Home screen and state machine (see states below), geofence check (distance to the office point, refusal with distance shown), camera capture screen (2 to 3 seconds of frames via CameraX analysis, guidance text, best-frame selection), calling `FaceVerifier`, trusted time, saving the punch and day summary in SQLite, blocking buttons after use, midnight reset, and next-day missed-punch handling.
**Home button states:**
- Before Punch In: In enabled, Out disabled.
- After Punch In: In blocked (shows time), Out enabled.
- After Punch Out: both blocked (shows both times).
- After 23:59 without Punch Out: buttons reset at 00:00; the earlier day becomes `MISSED_PUNCH` at the first punch of the new day.
**Depends on:** Agents 1, 2, 5.
**Done when:** every state above is covered by tests; refused punches (outside radius, face mismatch, no face, liveness fail) show clear messages and do not consume the day's button.

### Agent 5: Face Pipeline (ONNX)
**Owns:** `android/.../face/`, `/tools` parity scripts.
**Input needed from the human:** the existing Python code of the custom model (preprocessing, liveness logic, score calculation). Port it faithfully. Do not redesign it.
**Builds:** model downloader (manifest from `/models/manifest`, resumable, checksum check, progress state), ONNX session management (sequential load and release), detection, alignment, liveness using the 3D landmark model as in the Python reference, embedding extraction, similarity scoring and threshold from config, `FaceVerifier` implementation returning a typed result (match, mismatch, no face, multiple faces, liveness failed, model missing) with score.
**Parity testing:** a script in `/tools` runs the Python reference and the Android pipeline (instrumented test) on the same set of sample images and compares embeddings and scores within a tolerance. Report results in `PROGRESS.md`.
**Performance work:** measure time and peak memory; evaluate INT8 quantization of the two large models and report accuracy impact at the threshold.
**Done when:** parity tests pass, the sequential load keeps memory within the measured budget, and the download flow handles interruptions.

### Agent 6: Sync Engine
**Owns:** `android/.../sync/`.
**Builds:** WorkManager scheduling for the configured windows with random offset, Sync Now use case, batch upload of `PENDING` punches with images (multipart), per-record acknowledgement handling, retry and backoff, marking `SYNCED` or `FAILED`, sync status state for the UI (last synced, pending count), and safe behavior when interrupted mid-upload.
**Depends on:** Agent 1, the API contract, and the backend (Agent 8).
**Done when:** tests prove no duplicates on retry, no lost records after process death, correct scheduling offsets, and correct behavior with no network.

### Agent 7: Calendar
**Owns:** `feature/calendar/`.
**Builds:** monthly calendar from SQLite (`day_summary`), status markers (complete, missed punch, no record), month navigation, and the day detail bottom sheet (punch-in time, punch-out time, status, `time_unverified` flag, sync status).
**Depends on:** Agents 1 and 2.
**Done when:** UI tests cover month changes, empty months, missed-punch days, and times displayed in the office timezone.

### Agent 8: Backend
**Owns:** `/server`.
**Builds:** everything in the API contract: login (JWT), config, photo v1 upload and photo v2 replacement (one-time rule enforced with a database constraint and an application check), batch sync ingestion (idempotent on `punch_uuid`, transactional, stores images, updates `attendance_days` including missed-punch logic), monthly history endpoint, model manifest endpoint, migrations, an **admin CLI or seed script** (create user with initial password, set office point, radius, timezone, sync windows, match threshold), request validation, rate limiting on login, and structured logging.
**Done when:** e2e tests cover every endpoint and rule (second photo change rejected, duplicate punch upload ignored, batch partially valid, invalid token).

### Agent 9: QA and Integration Testing
**Owns:** `docs/TEST_MATRIX.md`, cross-cutting tests.
**Builds:** end-to-end test scripts (backend up, emulator flow), the manual test matrix (see section 11), and a defect list. Reports to Agent 0.

---

## 6. API contract (Agent 0 writes the formal `openapi.yaml`)

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/auth/login` | User ID + password, returns token and profile summary (including `photo_change_used`). |
| GET | `/config` | Office latitude, longitude, radius, timezone, sync windows, sync jitter minutes, match threshold. |
| POST | `/users/me/photo` | Upload photo v1 (multipart). |
| PUT | `/users/me/photo` | Replace with v2. Rejected if already used. |
| GET | `/users/me/photo` | Fetch the active photo (for reference). |
| POST | `/attendance/sync` | Batch of punches with images; response has per-record status (`ACCEPTED`, `DUPLICATE`, `REJECTED` with reason). |
| GET | `/attendance?month=YYYY-MM` | Records for a month (history pull). |
| GET | `/models/manifest` | Model file names, sizes, checksums, download URLs. |

Error format: a consistent JSON error with a machine-readable code and a human message.

---

## 7. Data model (Agent 0 formalizes)

**SQLite / Room**
- `user_local`: user_id, display_name, photo_change_used, reference_embedding, updated_at
- `punches`: punch_uuid (PK), user_id, date_local, type (IN/OUT), punch_time_utc, time_source (NTP/DEVICE), time_unverified, latitude, longitude, distance_m, match_score, image_path, sync_status (PENDING/SYNCED/FAILED), sync_attempts, created_at
- `day_summary`: user_id, date_local, in_time, out_time, status (COMPLETE/MISSED_PUNCH/NO_RECORD)
- `app_config`: office_lat, office_lng, radius_m, timezone, sync_windows, sync_jitter_minutes, match_threshold, last_fetched_at

**MySQL**
- `users`, `user_photos` (unique on user_id + version, version limited to 1 or 2), `attendance_punches` (unique `punch_uuid`), `attendance_days`, `office_config`, `sync_batches`

---

## 8. UI design specification: minimal, light

**Principles:** clean, quiet, lots of white space, one accent color, no gradients, no heavy shadows, no illustrations. Light theme only (no dark theme, no dynamic color).

**Colors**
| Role | Hex |
|---|---|
| Background | `#FAFAFA` |
| Surface (cards, sheets) | `#FFFFFF` |
| Primary text | `#111827` |
| Secondary text | `#6B7280` |
| Border / divider | `#E5E7EB` |
| Accent (buttons, selected date) | `#2563EB` |
| Accent tint (chips, highlights) | `#EFF4FF` |
| Success | `#16A34A` |
| Warning (missed punch) | `#D97706` |
| Error | `#DC2626` |

**Typography:** system sans-serif (Roboto). Title 22 sp semibold, section heading 16 sp medium, body 14 sp regular, caption 12 sp. Large time display on Home: 40 sp medium.

**Shape and spacing:** 8 dp grid. Screen padding 20 dp. Cards 12 dp corners, 1 dp border, no shadow. Buttons 12 dp corners, 52 dp height, full width for primary actions. Minimum touch target 48 dp.

**Components:** primary filled button (accent), secondary outlined button, disabled state in light gray with gray text, status chip (small rounded, tinted), simple top bar (title only, no color fill), bottom navigation with two items (Home, Calendar) and a small line icon set, text fields with 1 dp border and accent focus border, bottom sheets for day detail.

**Motion:** minimal. Short fades and simple state transitions only.

### Screens

1. **Login:** app name, user ID field, password field, Sign in button. Inline error text.
2. **Onboarding:** one step per screen with a short line of text and a single button. Photo capture screen: camera preview in a rounded frame with a simple oval guide, instruction text, capture button. Success confirmation for each step.
3. **Model download (first launch):** a single card with progress bar, size and percentage, and Retry on failure.
4. **Home:** greeting and date at the top; a large current-time display (NTP-based when online); a card with today's Punch In and Punch Out times (dashes when empty); two large buttons (Punch In, Punch Out) with the blocked states described in section 5 (Agent 4); a small sync status line (pending count, last synced) with a Sync Now text button; an unobtrusive "time unverified" note when offline.
5. **Punch capture:** full-screen camera preview with oval guide, short instruction ("Look at the camera"), a thin progress indicator during the 2 to 3 second capture, then a result screen: success with the recorded time, or a clear failure reason with a Try again button.
6. **Calendar:** month title with previous/next arrows; a 7-column grid; a small colored dot under dates (green complete, amber missed punch, none for no record); today outlined; selected date filled with accent color.
7. **Day detail (bottom sheet):** date title, rows for Punch In and Punch Out with times, a status chip, optional "time unverified" tag, and sync status.
8. **Settings/Profile (simple):** user ID, profile photo, "Change photo" (shown only if not used), Sign out.

**States for every screen:** loading (simple skeleton or spinner), empty, error with retry, and offline notice where relevant.

---

## 9. Execution waves

| Wave | Who | Work | Gate before the next wave |
|---|---|---|---|
| 0 | Agent 0 | Contracts, schema, scaffolds, shared interfaces | Both projects build; contracts reviewed by the human |
| 1 (parallel) | Agents 1, 2, 5, 8 | Core, design system, face pipeline, backend | Each passes its own tests |
| 2 (parallel) | Agents 3, 4, 6, 7 | Onboarding, punch flow, sync, calendar | Each passes its own tests against real backend |
| 3 | Agent 9 + Agent 0 | Integration, end-to-end tests, device matrix, fixes | All acceptance criteria met |

---

## 10. Acceptance criteria (summary)

- A new user can log in, capture a photo, grant permissions, and reach Home.
- The photo can be changed once. A second attempt is rejected by both the app and the server, and both photos exist in the database.
- Punch is refused outside 100 m and shows the distance. Inside 100 m, the face pipeline runs and a matching face records the punch.
- A mismatched face, no face, multiple faces, or failed liveness does not record a punch.
- Punch In and Punch Out can each be used exactly once per day and buttons block after use.
- Offline punches use the device clock and are flagged `time_unverified`. Online punches use NTP time in the office timezone.
- A day with only Punch In becomes `MISSED_PUNCH` at the next-day first punch, with punch-out equal to punch-in.
- Buttons reset at 00:00 office time.
- Records sync in the configured windows with random offset, and via Sync Now. Retrying never creates duplicates on the server.
- Calendar shows the month's statuses and the day detail sheet shows correct times.
- The UI matches section 8.
- Face pipeline parity tests pass against the Python reference.

---

## 11. Testing plan

- **Unit:** time provider, punch state machine, missed-punch logic, sync scheduler offsets, similarity and threshold logic.
- **Integration:** Room DAOs, Retrofit against a mock server, backend e2e for every endpoint.
- **UI:** Compose tests for onboarding, punch states, calendar.
- **Parity:** Python vs Android embeddings and scores on a shared image set.
- **Manual device matrix (Agent 9):** at least one low-end phone (2 to 3 GB RAM) on Android 8 or 9, one mid-range phone on a recent Android; scenarios: airplane mode punch, reboot then offline punch, denied permissions, low storage, interrupted model download, app killed during sync, changing the device clock, punching near the radius boundary, midnight rollover.

---

## 12. Security and non-functional requirements

- HTTPS only. Token stored in Keystore-backed storage. No secrets in the repo.
- Images stored in app-private storage. Delete local images after the server acknowledges them.
- Server enforces all rules (photo limit, duplicates, validation), not only the app.
- Passwords hashed on the server. Login rate limiting.
- Peak memory and punch duration are measured on the low-end phone and recorded in `PROGRESS.md`.
- Biometric data handling (retention, access, consent text) must be reviewed by the human before production.

---

## 13. Items the human must supply or confirm before Wave 1

1. The existing **Python code for the custom face model** (preprocessing, liveness logic, scoring) and a set of sample images for parity tests.
2. Backend stack confirmation (NestJS + MySQL is proposed here).
3. Office point coordinates, timezone, and the initial sync windows (for seed data).
4. Where the model files are hosted for download and how checksums are produced.
5. Confirmation that the **InsightFace model license** allows commercial workplace use, or a plan for other weights.
6. Confirmation that ONNX Runtime's Android build works on API 26 for the chosen version.

---

## 14. Master kickoff prompt (paste into Antigravity for Agent 0)

> You are the Orchestrator for building an Android attendance app and its backend, following `Attendance_App_Development_Plan.md` and `Attendance_App_PRD.docx`. First complete Wave 0: write `docs/openapi.yaml`, `docs/schema.sql`, `docs/room-schema.md`, create buildable `android` and `server` scaffolds, and define the shared interfaces. Stop and show me the contracts for review. After approval, spawn the Wave 1 agents (Core, Design System, Face Pipeline, Backend) in parallel with strict folder ownership, then Wave 2 agents (Auth and Onboarding, Punch Flow, Sync Engine, Calendar), then QA. Require full implementations with tests, no placeholders, and progress notes in `docs/PROGRESS.md`. Do not implement anything listed under Deferred.
