# Attendance App: Mobile UI Revamp Plan (Findez-style dark theme)

Target: `android/` (native Kotlin, Jetpack Compose, Hilt). Reference: Dribbble "Findez – Digital Attendance App".
Scope: **visual and layout revamp only.** Existing routes, ViewModels, repositories, sync, face pipeline, geofencing and networking stay as they are, apart from the small additive changes listed in section 6.

Put this file at `docs/UI_REVAMP_PLAN.md` and the reference folder at `docs/design-reference/` (see section 12) before starting any agent.

---

## 1. Locked decisions

| # | Decision |
|---|---|
| 1 | **Dark theme only** for now. The light theme is deferred; do not delete the light tokens' names, only repoint them (see section 3). |
| 2 | **Icons:** use the app's existing icons where the app already has one (location, refresh, warning, check-circle, account, etc.). For Findez-specific icons that have no source file, **trace them by hand into Android vector drawables** from the crops in `docs/design-reference/icons/` (see section 4). |
| 3 | **Home stat grid = 4 tiles:** Checked In, Checked Out, **Location status** (replaces "On Time %"), Total Attended. |
| 4 | **Ring = work hours:** worked time computed from punch-in and punch-out. If punched in but not out, it is the live elapsed time (trusted clock). |
| 5 | **Bottom bar:** Home, Calendar, a **raised center punch button** (Punch In by default, becomes Punch Out when punched in and not out), and **Settings** (the existing `profile_settings` screen). **Admin lives inside Settings only**, removed from the bar. |
| 6 | **Calendar:** horizontally scrollable week strip (Findez style) **with a month toggle** that reveals the existing month grid. |
| 7 | **Swipe-to-punch slider is included.** Placement: pinned at the bottom of Home, above the nav bar (section 5.1). |
| 8 | **Bell dropped.** Tasks, Analytics, Manager/Employee split and onboarding carousel are out of scope. |

---

## 2. Hard constraints (apply to every agent)

**Do not change:**
- Route strings, nav arguments, `Screen.kt` route values, `PunchCapture.createRoute(...)` format.
- `core/network`, `core/database` (entities, DAOs' existing queries, schema/version), `sync/`, `face/`, `core/location`, `core/time`, `core/repository`, `server/`, `FaceAPI/`.
- Existing ViewModel public functions and existing `UiState` fields. Only **additive** fields/functions are allowed (section 6).
- Permission, geofence, mock-location, time-unverified and face-verification behaviour.

**Always:**
- Every color, size, radius and text style comes from `design/theme/` tokens. No new `Color(0x…)` literals outside `design/theme/`, no raw `.sp`/`.dp` for design values that belong in tokens.
- Keep user-facing behaviour identical (same enabled/blocked/loading states, same dialogs, same snackbars).
- Every new composable gets `@Preview`s for each meaningful state (dark background).
- Touch targets ≥ 48dp. Every icon-only control has a `contentDescription`. Text/background contrast ≥ WCAG AA (4.5:1 body, 3:1 large text/icons).
- Work on branch `ui-revamp`. One agent = one set of owned files (section 9) to avoid merge conflicts.

---

## 3. Design tokens

> Hex values were **sampled from the supplied screenshots** (pixel colors), so they are close but not designer-exact. Values marked ~ are estimates for thin text anti-aliasing. Treat all as tunable in one place (`Color.kt`).

### 3.1 Color (repoint the existing token names, add new ones)

| Role | Existing token | New value |
|---|---|---|
| Body/panel background | `AppBackground` | `#070B0F` |
| Header zone background (top area above the rounded body panel) | new `AppHeaderBackground` | `#14181D` |
| Card / surface | `AppSurface` | `#14181D` |
| Dimmed / disabled card | `AppDisabled` | `#0C1015` |
| Icon-circle fill | new `AppIconCircle` | `#2A2E33` |
| Accent (mint) | `AppAccent` | `#4EFFCA` |
| Accent tint (chips, selected states) | `AppAccentTint` | mint at ~14% alpha over surface |
| Text on accent | new `AppOnAccent` | near-black `#04120D` |
| Primary text | `AppPrimaryText` | `#FFFFFF` |
| Secondary text | `AppSecondaryText` | ~`#A3A9B0` |
| Disabled/placeholder text | `AppDisabledText` / `AppPlaceholder` | ~`#6B7178` |
| Border / outline (icon buttons, dividers) | `AppBorder` / `AppDivider` | ~`#2A2E33`, subtle outline ~`#3A3F45` |
| Success | `AppSuccess` | = mint accent family |
| Warning | `AppWarning` | retune for dark, amber (~`#FBBF24`), tint = 16% alpha |
| Error | `AppError` | retune for dark, soft red (~`#F87171`), tint = 16% alpha |

Also: repoint `LightColorScheme` → a new `DarkColorScheme` (`darkColorScheme`) mapped to the same tokens; `Theme.kt` uses it, sets `isAppearanceLightStatusBars=false` / `isAppearanceLightNavigationBars=false`, status bar color = header color, navigation bar color = body color.

### 3.2 Typography
- Font: **Poppins** (Regular 400, Medium 500, SemiBold 600). Bundle as `res/font/poppins_*.ttf` (SIL OFL license; download from Google Fonts). Replace `FontFamily.Default` in `Type.kt`.
- Keep token **names** (`ClockTime`, `Title`, `SectionHeading`, `Body`, `BodyMedium`, `Caption`, `CaptionMedium`, `ButtonText`); adjust values only where listed:
  - `ClockTime` 40sp → 48sp (used for the ring's worked-time text)
  - add `StatValue` 22sp Medium, `StatLabel` 13sp Regular secondary, `RingCaption` 14sp Regular secondary, `NavLabel` 12sp.
- Keep other sizes as they are today so the font-size test needs minimal edits.

### 3.3 Shape and spacing
- Card radius **20dp** (`CardCornerRadius`), stat tiles 20dp, chips 12dp, buttons **fully rounded (pill)**, icon buttons circular 44dp with 1dp outline, body panel **top corners 32dp**.
- Keep `ScreenPadding` 20dp, keep the 8dp grid. Elevation stays 0 (separation comes from tone, not shadows).
- `ButtonHeight` 52dp stays; `ButtonCornerRadius` becomes pill (26dp).

---

## 4. Icons

Source of truth for the look: `docs/design-reference/icons/` (cropped from the screenshots, upscaled) and `docs/design-reference/screens/`.
Style spec: 24dp grid, 1.5dp stroke, round caps and joins, no fill (active nav icons are filled mint), rounded-square "tile" metaphor.

Create vector drawables under `res/drawable/` (prefix `ic_findez_`), each traced by hand from the crops:

| Drawable | Source crop | Used for |
|---|---|---|
| `ic_findez_check_in` | `check_in`, `ring_check_in_circle` | Checked In stat tile, attendance cards |
| `ic_findez_check_out` | `check_out`, `ring_check_out_circle` | Checked Out stat tile, attendance cards |
| `ic_findez_checked_box` | `checked_box` | optional (if an On-Time metric returns later) |
| `ic_findez_calendar_dots` | `calendar_dots` | Total Attended tile |
| `ic_findez_calendar_8` | `calendar_8` | Calendar header "go to today" |
| `ic_findez_menu_four_dots` | `menu_four_dots` | Calendar header week/month toggle |
| `ic_findez_nav_home` + `_active` | `nav_home` | bottom bar |
| `ic_findez_nav_schedule` + `_active` | `nav_schedule_active` | bottom bar (Calendar) |
| `ic_findez_nav_settings` + `_active` | `nav_settings` | bottom bar (Settings) |
| `ic_findez_punch_in` | `nav_center_punch` | center button (Punch In) |
| `ic_findez_punch_out` | mirror of punch_in | center button (Punch Out) |

Not needed: bell, analytics, tasks/comment/attachment icons.
Existing Material icons stay for: location on/off, refresh, warning, check-circle, account-circle, chevrons, visibility, lock, person, error.

Because the crops are low-resolution, expect a close match, not pixel-exact. If the designer's SVGs ever surface, swap the drawables and nothing else changes.

---

## 5. Screen specifications

### 5.1 Home (`feature/punch/HomeScreen.kt`)
Two-tone layout: header zone `#14181D` on top, then a darker rounded body panel (`#070B0F`, top corners 32dp) holding the content.

**Top to bottom (scrolling area):**
1. **Header:** avatar (48dp circle, existing profile bitmap, fallback account icon) + display name + subtitle (employee ID). Tapping the avatar still calls `onNavigateToProfile`. No bell.
2. **Stat grid, 2×2** (tiles 20dp radius, `#14181D`, value `StatValue`, label `StatLabel`, icon top-right):
   - **Checked In:** `inTimeFormatted` (or `--:--`), `ic_findez_check_in`.
   - **Checked Out:** `outTimeFormatted` (or `--:--`), `ic_findez_check_out`.
   - **Location:** live from `locationCardState` (mapping below). Tap = refresh (`onRefreshLocation`), shows a small spinner while refreshing. Icon tint reflects state.
   - **Total Attended:** "N days" (this month's days with a punch-in), `ic_findez_calendar_dots`.
3. **Work-hours ring card:**
   - Title line (copy, editable): not punched in → "Check in your presence"; in progress → "You're on the clock"; complete → "Great work today".
   - Ring 200dp, stroke ~14dp, round caps, mint arc on dark track. **Center:** worked time `H:MM` in `ClockTime`, sublabel ("not checked in" / "worked today").
   - Below the ring: office time caption ("Office time HH:MM:SS") from the existing trusted clock, with the existing `TimeUnverifiedBadge` when `isTimeUnverified`.
   - Shift chip below: existing `shiftDisplayName` (e.g. "General Shift (09:00 - 18:00)").
   - Ring states: **NotStarted** (empty track, `0:00`), **InProgress** (live elapsed, ticks with trusted time), **Complete** (fixed total worked), **MissedPunch** (session expired past cutoff: `--:--`, warning tint, "Missed punch").
   - Ring target = **9 hours** (all three shifts are 9h spans today). Single constant, easy to change. Progress clamps at 100%.
4. **Sync status bar:** existing `SyncStatusBar`, restyled, slim row under the ring card.

**Pinned bottom action area (outside the scroll, above the nav bar):**
- Field-visit warning text (existing copy) when `isNewLocationDay` and punch-out is available.
- **"Punch in at new location"** as a secondary pill, shown only when punch-in is available (same handler as today).
- **Swipe-to-punch slider** (new `SwipeToPunch`):
  - Pill 64dp tall, surface `#14181D`, mint circular thumb (52dp) with arrow icon, centered secondary-text label.
  - Label/mode derived from existing state: punch-in available → "Swipe to Check In"; punch-out available → "Swipe to Check Out"; both done → disabled "Attendance complete"; blocked → disabled showing the existing `punchInButtonText` / `punchOutButtonText`.
  - Trigger at ≥ 85% drag (haptic), otherwise spring back. Fires the same handlers as the old buttons (`onPunchInClicked` / `onPunchOutClicked`). While `isCheckingGeofence`: thumb shows a spinner, input disabled.
  - Accessibility: expose a custom accessibility action ("Check in" / "Check out") so TalkBack users don't need to swipe.
- Layout note: the raised center button protrudes above the nav bar, so the action area needs bottom padding of nav height + protrusion + 8dp. Verify on device.
- Fallback if this feels crowded: move the slider into the scroll content directly under the ring card.

**Location tile mapping** (`LocationCardState` → value / label / tint):

| State | Value | Label | Tint |
|---|---|---|---|
| Locating | "Locating…" | "Checking proximity" | neutral (spinner) |
| InOffice | "In Office" | "{d} m away" | success |
| FieldVisit inside | "Field Site" | "{d} m · within radius" | success |
| FieldVisit outside | "Away" | "Return to check-in spot" | warning |
| Outside | "Outside" | "Allowed {r} m" | warning |
| PermissionNeeded | "No permission" | "Tap to allow" | warning |
| ServicesDisabled | "GPS off" | "Turn on location" | warning |
| MockDetected | "Mock GPS" | "Disable fake GPS" | error |
| Unavailable | "Weak signal" | short reason | neutral |

Full detail still appears in the existing geofence dialog when a punch is attempted, and the permission guard dialog is unchanged.

### 5.2 Calendar (`feature/calendar/`)
- **Header zone:** left circular button (`ic_findez_menu_four_dots`) = **toggle week/month view**; center = month and year + full selected date; right circular button (`ic_findez_calendar_8`) = **go to today**.
- **Week strip:** horizontally scrollable, swipe by weeks without a hard limit; day number over weekday label; selected day = mint filled circle; today = mint outline; status dot under the number (complete = mint, missed = amber, none = nothing), as the current `CalendarDayCell` does. Scrolling into another month must update the loaded month.
- **Month toggle:** expands the existing month grid (restyled `CalendarDayCell`, month navigation chevrons stay). Collapses back to the strip.
- **Your Attendance section** (title + "See more"): two cards for the selected day.
  - Check In: time, status text. Check Out: time, or dimmed card with "Not Yet" when no punch-out.
  - Where Findez shows "On Time", show the existing day-status chip instead (Complete / Missed punch / No record), plus the existing unverified-time and sync badges.
  - "See more" opens the existing `DayDetailBottomSheet`, restyled.
- **Month summary card and legend:** keep, restyled, below.
- No task cards.

### 5.3 Bottom bar (`design/components/BottomNavBar.kt`, `design/navigation/`)
- Four slots: **Home | Calendar | raised center button | Settings.**
- Style: body color background, 1dp subtle top border, active tab = mint top indicator line + filled mint icon + mint label; inactive = outline icon, secondary text.
- **Center button:** 56dp mint circle protruding ~20dp. Icon/behaviour by state: punch-in available → `ic_findez_punch_in` (Punch In); punched in, not out → `ic_findez_punch_out` (Punch Out); complete or blocked → dimmed, non-interactive (check mark when complete). Same underlying handlers as Home (geofence → camera flow unchanged).
- Visible on `home`, `calendar`, `profile_settings`. Hidden on login, onboarding, model_download, punch_capture, admin_panel, component_gallery.
- `admin_panel` is **removed from the bar**; it is reached only through the existing "Open Admin Panel" entry on the Profile/Settings screen (already wired). The `isAdmin` parameter on the bar becomes unused.
- Selecting Settings navigates to `profile_settings` using the same single-top/save-state options the bar uses today. The avatar shortcut on Home keeps working.
- **Cross-tab center button (needs a spike, see A7):** preferred = the button punches from any tab by sharing Home's punch logic. Fallback if Home's ViewModel cannot run while Home isn't composed = on Calendar/Settings the button navigates to Home (no auto-punch); on Home it punches directly.

### 5.4 Login (`feature/auth/LoginScreen.kt`)
Dark hero look: header-zone background, large heading, restyled fields (dark filled, mint focus ring), pill mint "Sign In" button, same copy and same error handling. No Manager/Employee buttons.

### 5.5 Everything else (token inheritance plus targeted fixes)
- **Profile/Settings** (`ProfileScreen.kt`): dark cards, pill buttons, mint accents. Keep every section (Biometrics & Photo, Administration → Admin Panel, Sign Out, gallery entry). Fix the 6 `Color.White` spots so they are theme-driven.
- **Punch capture** (`PunchCaptureScreen.kt`): keep the camera UI; replace its 6 hard-coded color literals with tokens; oval guide and progress turn mint; result panels use the new cards/buttons.
- **Onboarding** (`feature/onboarding/`): inherits; guide overlay and photo capture accents turn mint.
- **Admin** (`AdminScreen.kt`, 1437 lines): inherits via components. Review dialogs, dropdowns, switches and text fields for contrast on dark; fix any `Color.White` spots.
- **Component gallery:** update to show the new components (dev-only screen).

---

## 6. Minimal logic additions (all additive, all unit-tested)

1. **Work duration (Home):** a pure helper (suggested name `WorkDurationCalculator`) taking punch-in epoch, punch-out epoch (nullable), current trusted time, shift type, cutoff/expiry state and target hours. Returns worked duration, progress (0..1) and ring state. Cases to cover: no punch-in; in only; in + out; night shift across midnight; out earlier than in (guard); expired session; progress clamp at 100%; unverified/offline time. `HomeViewModel` calls it on its existing tick and exposes **new additive fields** on `HomeUiState` (worked duration, progress, ring state, total attended).
2. **Total Attended:** days in the current office-month with a punch-in. Reuse the existing day-summary data path that `CalendarViewModel` already uses for its month summary. If a new read-only DAO query is unavoidable, add it without touching entities or the schema version.
3. **Calendar week selection:** additive UI-state fields for view mode (week/month) and a "select date" function that also loads the right month when the strip scrolls across a month boundary. No repository changes.
4. **Center-button state:** a small derived value (IN / OUT / DONE / BLOCKED) from existing `HomeUiState` flags, exposed so the nav bar can render the right icon. Cross-tab behavior per the A7 spike.

---

## 7. Test changes

Existing tests that **must be updated on purpose** (they pin the old design):
- `ThemeTokensTest`: new hex values, new `ClockTime` size, new radius/button dimensions.
- `NavigationRoutesTest`: `profile_settings` now shows the bottom bar; `admin_panel` does not; route strings unchanged; `BottomNavItem` now Home, Calendar, Settings.

New tests:
- `WorkDurationCalculator` (all cases in 6.1).
- `HomeViewModelTest` additions: ring state transitions, total attended.
- `CalendarViewModelTest` additions: view mode, month-boundary selection.
- Pure swipe-threshold logic for `SwipeToPunch` (trigger vs spring-back).
- Center-button state derivation (IN/OUT/DONE/BLOCKED).

All other existing tests must pass unchanged.

---

## 8. Phases and dependencies

```
A1 Foundation ─┬─► A3 Restyle components ─┬─► A8 Other screens ─┐
A2 Icons ──────┤                          │                     │
               └─► A4 New components ─────┼─► A5 Home ──────────┤
                                          ├─► A6 Calendar ──────┼─► A9 QA
                                          └─► A7 Nav shell ─────┘
```

A1 and A2 run in parallel first. After A1: A3 and A4 in parallel. After those: A5, A6, A7, A8 in parallel. A9 last.

---

## 9. Agent briefs and file ownership

Each agent must stay inside its owned files. If it needs a change elsewhere, it writes the request in its report instead of editing.

### A1: Foundation (theme, font, tests)
**Owns:** `design/theme/*`, `res/font/*`, `res/values/*` (if needed), `ThemeTokensTest`.
**Do:** section 3 in full (tokens, dark scheme, Poppins, shapes, dimensions, system bars). Update `ThemeTokensTest`.
**Done when:** app builds, tokens test passes, every existing screen renders dark (even if rough).

### A2: Icons
**Owns:** `res/drawable/ic_findez_*`.
**Do:** section 4. Trace from `docs/design-reference/icons/`. Provide an icon preview (a throwaway gallery section or previews) for review.
**Done when:** all drawables exist, tint correctly via `Icon(tint=…)`, and look right at 24dp and 20dp on the dark surface.

### A3: Restyle existing components
**Owns:** `design/components/Buttons.kt`, `TextFields.kt`, `TopAppBar.kt`, `StatusChip.kt`, `StateViews.kt`, `SyncStatusBar.kt`, `TimeUnverifiedBadge.kt`, `PermissionGuardDialog.kt`, `DayDetailSheet.kt`, `CalendarDayCell.kt`, `TimeDisplayCard.kt`.
**Do:** pill mint primary button with `AppOnAccent` text, outlined secondary, mint text buttons, dark filled text fields with mint focus, tinted chips, dark dialogs/sheets (28dp radius), restyled day cell (selected = mint circle).
**Done when:** every component preview is correct on dark, signatures unchanged (no caller edits needed).

### A4: New components
**Owns:** new files in `design/components/` (`StatCard`, `WorkRing`, `SwipeToPunch`, `SectionHeader`, `IconCircleButton`, `WeekStrip`, `PunchNavBar` pieces) and `ComponentGalleryScreen.kt`.
**Do:** build per sections 5.1–5.3 as stateless composables driven by plain parameters (no ViewModel access). Include previews of every state (SwipeToPunch: idle, dragging, loading, disabled, complete; WorkRing: all four states).
**Done when:** each renders correctly in previews and the gallery; swipe threshold logic is a pure, tested function.

### A5: Home
**Owns:** `feature/punch/HomeScreen.kt`, `HomeViewModel.kt`, `HomeUiState.kt`, `WorkDurationCalculator` (+ test), `HomeViewModelTest`.
**Do:** section 5.1 and 6.1/6.2/6.4. Keep every existing handler and dialog.
**Done when:** all Home states in section 10 behave identically to before and look right.

### A6: Calendar
**Owns:** `feature/calendar/*` (+ tests), `DayDetailBottomSheet.kt`.
**Do:** section 5.2 and 6.3.
**Done when:** week strip scrolls across months and loads data, month toggle works, "See more" opens the restyled sheet.

### A7: Nav shell and center button
**Owns:** `design/components/BottomNavBar.kt`, `design/navigation/*`, `MainActivity.kt` (only if required), `NavigationRoutesTest`.
**Do:** section 5.3. **Start with a spike** (report before building): can the center button reuse Home's punch logic from other tabs? Inspect how `HomeViewModel` collects navigation events and whether punch handlers need Home's location monitoring. Choose "punch from any tab" only if safe; otherwise use the fallback.
**Done when:** routes unchanged, bar visibility rules correct, Admin reachable only via Settings, tests updated and green.

### A8: Other screens
**Owns:** `feature/auth/*` (Login, Profile), `feature/punch/capture/*`, `feature/onboarding/*`, `feature/admin/*`.
**Do:** section 5.4–5.5. Remove all hard-coded colors; contrast review.
**Done when:** zero `Color(0x…)` outside `design/theme/`, no unintended `Color.White` left, all screens readable on dark.

### A9: QA and review (read-only first)
**Do:** run the full unit test suite and a debug build; grep for stray color literals; walk the state matrix in section 10; check font scale 1.3× and 360dp width; TalkBack labels; contrast pass; confirm nothing in the "do not change" list changed (diff review). Produce a findings list; fixes go back to the owning agent.

---

## 10. Verification matrix

**Home:** not punched in · punched in (ticking ring) · complete · missed-punch/expired · time unverified · locating · in office · outside radius · field visit inside/outside · permission needed · GPS off · mock GPS · pending sync / syncing / failed · geofence check in progress · swipe in / swipe out / swipe cancelled · center button in each state · "new location" punch · avatar → settings.
**Calendar:** week scroll across a month boundary · toggle to month and back · go-to-today · day with complete/missed/no record · unverified-time badge · See more sheet · summary and legend.
**Shell:** each tab selected state · bar hidden on the right routes · admin user sees Admin only inside Settings · non-admin sees no admin entry · back-stack behaviour for tabs unchanged.
**Global:** font scale 1.3× · 360dp wide device · system bars dark · TalkBack · contrast AA · rotation unchanged from today · cold start routing (login / onboarding / home) unchanged.

---

## 11. Assumptions to confirm (each is a one-line change)

1. Ring target is **9 hours** for every shift.
2. **Poppins** is acceptable and gets bundled.
3. Hex colors above are sampled approximations; replace if you get the designer's values.
4. **Total Attended** = days this month with a punch-in.
5. The Home subtitle under the name is the employee ID; the large trusted clock moves into the ring card as a caption.
6. Ring/title copy strings in 5.1 are placeholders.
7. Settings tab = the existing single `profile_settings` screen (Profile and Settings split can come later).
8. Slider is pinned above the nav bar with the center button as a second entry point to the same action.

---

## 12. Setup for Antigravity

1. Create branch `ui-revamp`.
2. Add `docs/UI_REVAMP_PLAN.md` (this file) and `docs/design-reference/` (unzip `design-reference.zip`: `screens/` = the 7 Dribbble screenshots, `icons/` = cropped icon references, `_contact_sheet.png` = overview).
3. Start A1 and A2 together; give each agent only its brief from section 9, plus sections 2, 3 and the relevant spec. Merge A1 before starting A3/A4.
4. Review each agent's report before unblocking the next phase; A7's spike result decides the center-button behaviour.
5. If the repo tracks progress in `docs/PROGRESS.md`, add a "UI revamp" entry in its existing format.
6. Switch the repo back to private after the code is cloned or pushed as you prefer.
