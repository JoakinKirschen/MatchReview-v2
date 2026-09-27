# MatchReview Pass Changelog

This is the **single source of truth** for implementation-pass notes and change tracking.
It consolidates implementation notes for Passes 1–45.

## How to maintain this file

For every future development pass:

1. Add the pass to the **Unreleased / next passes** table before implementation.
2. Record the scope, files or components affected, migrations, tests, verification, and
   intentionally deferred work.
3. When completed, add a full numbered section using the template below.
4. Update the app version, database version, or backup-format version only when applicable.
5. Do not create another standalone `PASS_<number>_NOTES.md`; update this file instead.

### Entry template

```markdown
## Pass NN — Short title

**Status:** Planned / In progress / Completed  
**Completed:** YYYY-MM-DD  
**Version impact:** App / database / backup format / none

### Added or changed
- ...

### Tests and verification
- ...

### Deferred or known limitations
- ...
```

## Unreleased / next passes

| Pass | Status | Planned change | Version impact |
|---:|---|---|---|
| 46 | Not scheduled | Add the next approved recommendation here | To be determined |

## Documentation changes

### 2026-09-27 — Consolidated pass tracking

- Merged all 35 standalone pass-note files into this changelog.
- Updated README links to use this file.
- Removed the superseded standalone pass-note files.
- Established this file as the required location for future pass changes.
- No application code, Room schema, backup format, or Android version was changed.

### 2026-09-27 — Passes 36–45

- Implemented the next 10 trust-and-resilience passes.
- Advanced the Android release to 1.4.0 (`versionCode = 6`).
- Kept the Room database at version 4.
- Advanced the backup format to 2 while retaining format-1 restore compatibility.
- Added focused domain tests and updated release documentation.

## Completed-pass index

| Pass | Summary |
|---:|---|
| 1 | [Match-day foundation](#pass-1-match-day-foundation) |
| 2 | [Available-player selection](#pass-2-available-player-selection) |
| 3 | [Pitch lineup builder](#pass-3-pitch-lineup-builder) |
| 4 | [Drag-and-drop lineup editing](#pass-4-drag-and-drop-lineup-editing) |
| 5 | [Live match clock and period controls](#pass-5-live-match-clock-and-period-controls) |
| 6 | [Live substitutions and player removal](#pass-6-live-substitutions-and-player-removal) |
| 7 | [Goals, assists, score correction, timeline and undo](#pass-7-goals-assists-score-correction-timeline-and-undo) |
| 8 | [CameraX recording](#pass-8-camerax-recording) |
| 9 | [Video/event integration](#pass-9-video-event-integration) |
| 10 | [hardening and release](#pass-10-hardening-and-release) |
| 11 | [changes](#pass-11-changes) |
| 12 | [reliable long-press drag-and-drop and space-optimised pitch](#pass-12-reliable-long-press-drag-and-drop-and-space-optimised-pitch) |
| 13 | [Pass 13](#pass-13-pass-13) |
| 14 | [Encrypted backup and restore](#pass-14-encrypted-backup-and-restore) |
| 15 | [Gradle plugin resolution hardening](#pass-15-gradle-plugin-resolution-hardening) |
| 16 | [Explicit substitution selection](#pass-16-explicit-substitution-selection) |
| 17 | [Fast goal capture](#pass-17-fast-goal-capture) |
| 18 | [Safer live controls](#pass-18-safer-live-controls) |
| 19 | [Persistent recording visibility](#pass-19-persistent-recording-visibility) |
| 20 | [Confirmed operation feedback](#pass-20-confirmed-operation-feedback) |
| 21 | [Contextual navigation and continuation](#pass-21-contextual-navigation-and-continuation) |
| 22 | [Lineup discoverability](#pass-22-lineup-discoverability) |
| 23 | [Restore preflight](#pass-23-restore-preflight) |
| 24 | [Backup confidence](#pass-24-backup-confidence) |
| 25 | [Accessibility and privacy baseline](#pass-25-accessibility-and-privacy-baseline) |
| 26 | [Match-day readiness check](#pass-26-match-day-readiness-check) |
| 27 | [Transactional lineup swaps](#pass-27-transactional-lineup-swaps) |
| 28 | [Focused match review sections](#pass-28-focused-match-review-sections) |
| 29 | [Squad availability controls](#pass-29-squad-availability-controls) |
| 30 | [Match search and status filters](#pass-30-match-search-and-status-filters) |
| 31 | [Safer match-date entry](#pass-31-safer-match-date-entry) |
| 32 | [Backup size preview](#pass-32-backup-size-preview) |
| 33 | [Durable imported-video linking](#pass-33-durable-imported-video-linking) |
| 34 | [Permission recovery](#pass-34-permission-recovery) |
| 35 | [Navigation icons and localization baseline](#pass-35-navigation-icons-and-localization-baseline) |
| 36 | [Match-data integrity checks](#pass-36-match-data-integrity-checks) |
| 37 | [Duplicate live-command protection](#pass-37-duplicate-live-command-protection) |
| 38 | [Clock recovery confidence](#pass-38-clock-recovery-confidence) |
| 39 | [Recording reconciliation](#pass-39-recording-reconciliation) |
| 40 | [Backup integrity format 2](#pass-40-backup-integrity-format-2) |
| 41 | [Privacy-safe CSV exports](#pass-41-privacy-safe-csv-exports) |
| 42 | [Accessible live announcements](#pass-42-accessible-live-announcements) |
| 43 | [Recording storage budget](#pass-43-recording-storage-budget) |
| 44 | [Practice match onboarding](#pass-44-practice-match-onboarding) |
| 45 | [Season summary](#pass-45-season-summary) |

---

# Completed pass details

<a id="pass-1-match-day-foundation"></a>

## Pass 1 — Match-day foundation

### Added
- Match lifecycle states and configurable match format.
- Match-specific squad and availability storage.
- Persistent pitch-placement storage using normalized coordinates.
- Period and durable clock-segment storage.
- Player participation intervals for future minutes-played calculations.
- Segmented recording metadata for the CameraX pass.
- Event links for assists and recording offsets.
- Version 1 to version 2 Room migration.
- Clock calculation unit tests.
- Match setup controls for periods, duration, player count and rolling substitutions.

### Intentionally deferred
The player-selection screen, pitch UI, drag-and-drop, live timer controls, substitutions and CameraX recording are implemented in passes 2–9.


---

<a id="pass-2-available-player-selection"></a>

## Pass 2 — Available-player selection

Started from the latest Pass 1 working project at:

`Historical source:` the preceding project revision.

### Added

- A match-day squad screen immediately after match creation.
- Automatic creation of match-specific squad records from the selected team roster.
- Availability states:
  - Available
  - Unavailable
  - Injured
  - Suspended
  - Not selected
  - Unknown
- Player search by name, shirt number or position.
- Filters for all, selected, available and unavailable players.
- Individual player selection with validation.
- “All available”, “Select available” and “Clear” bulk actions.
- Selected/available/required counters.
- Warning when fewer players are selected than the configured starting-lineup size.
- Ability to save a smaller squad rather than blocking the coach.
- A squad editing entry point from the match review screen.
- Unit tests for squad selection rules.

### Deliberately deferred

Pass 3 adds the actual pitch, bench and starting-lineup placement. Pass 4 adds drag and drop.


---

<a id="pass-3-pitch-lineup-builder"></a>

## Pass 3 — Pitch lineup builder

### Starting point
This pass was built from the latest Pass 2 working project:
`Historical source:` the Pass 2 project revision.

### Added
- A dedicated `lineup/{matchId}` route after squad selection.
- Responsive green football pitch with markings inspired by the supplied reference.
- Starting-player markers with shirt number, name and match role.
- A horizontal substitutes bench containing the remaining selected players.
- Normalized pitch coordinates (`0.0..1.0`) persisted in the existing lineup table.
- Formation-slot generation for configured formations such as 4-3-3.
- Safe fallback layouts when the formation does not match the configured player count.
- One-tap automatic lineup placement.
- Player placement sheet for:
  - placing a substitute in the next free slot;
  - choosing goalkeeper, defence, midfield or forward slots;
  - moving an on-pitch player in four directions;
  - returning a player to the bench.
- Accessibility custom actions for directional player movement.
- Full-lineup validation and explicit confirmation for a smaller lineup.
- Match state changes to `LINEUP_READY` when the lineup is saved.
- Unit tests for formation layout generation.

### Pass boundary
Long-press drag-and-drop, slot snapping and haptic feedback remain Pass 4. Pass 3 intentionally provides tap-based and accessibility-safe placement first.

### Verification
Run:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Or push the complete project to GitHub and run the included build workflow.


---

<a id="pass-4-drag-and-drop-lineup-editing"></a>

## Pass 4 — Drag-and-drop lineup editing

### Starting point
This pass was built from the latest Pass 3 working project:
`Historical source:` the Pass 3 project revision.

### Added
- Long-press drag gestures for players on the pitch and substitutes on the bench.
- Bench-to-pitch, pitch-to-bench and pitch-to-pitch movement.
- A lifted player ghost that follows the finger across the lineup editor.
- Highlighted pitch and bench drop targets.
- Automatic snapping to the nearest unoccupied formation slot.
- A highlighted formation target before the player is released.
- Free placement when the player is not close to a formation slot.
- Marker-safe coordinate clamping at the edges of the pitch.
- Persistence only after a valid drop, avoiding database writes for every pointer movement.
- Full-pitch validation when a substitute is dragged onto the field.
- Haptic feedback when a drag begins and when a drop is accepted or rejected.
- Snackbar confirmation for saved, snapped, benched, full-pitch and cancelled drops.
- Existing tap-based placement and accessibility movement actions remain available.
- Pure drag/drop rule tests for normalization, snapping, occupied slots and edge constraints.

### Gesture
1. Long-press a player.
2. Drag the lifted marker.
3. Release over the pitch to place the player.
4. Release near a free formation circle to snap into that role.
5. Release over the bench to remove an on-pitch player from the starting lineup.
6. Release elsewhere to cancel without changing the saved position.

### Pass boundary
Pass 5 adds the live match screen, kickoff, durable clock, pause/resume and period transitions.

### Verification
Run:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Or push the complete project to GitHub and run the included build workflow.


---

<a id="pass-5-live-match-clock-and-period-controls"></a>

## Pass 5 — Live match clock and period controls

Started from the Pass 4 project revision.

### Implemented

- Dedicated immersive live match screen based on the supplied match-day reference.
- Red scoreboard card with score, cumulative match clock, period number, and period clock.
- Kick-off, pause, resume, end-period, next-period, and finish-match controls.
- Persistent Room-backed match state transitions.
- Clock segments based on `SystemClock.elapsedRealtime()` rather than a database counter.
- Wall-clock fallback after a device reboot, capped to avoid adding an implausibly long interval.
- Initial player participation intervals at kick-off and each new period.
- Live player-minute badges derived from participation intervals.
- Bench minute cards and player information sheets.
- Period records and automatic closing of participation intervals at period end.
- Active-match resume card on the dashboard.
- Match state remains recoverable when leaving the screen or after Activity/process recreation.
- Screen stays awake during match-day states.
- Confirmation before leaving or finishing a match.
- Additional clock calculation tests.

### Deliberately deferred

- Substitution transactions and moving players during the live match: Pass 6.
- Goal, assist, score, and undo actions: Pass 7.
- CameraX recording: Pass 8.
- Final recovery reconciliation UI for ambiguous device-clock changes: Pass 10.

### Important behavior

Leaving the live screen does not stop a running logical match clock. The dashboard displays a **Resume live match** card. Pausing and period transitions are committed to Room immediately.


---

<a id="pass-6-live-substitutions-and-player-removal"></a>

## Pass 6 — Live substitutions and player removal

### Starting point

This pass was built from the Pass 5 project revision.

### Implemented

- Live substitutions from either an on-pitch player or a bench player.
- Atomic substitution transactions in Room.
- Outgoing participation intervals close at the exact logical match time.
- Incoming participation intervals open at the same logical match time.
- Pitch coordinates, tactical role, and formation slot transfer to the incoming player.
- Rolling-substitution behavior:
  - enabled: outgoing players return to the bench and may re-enter;
  - disabled: outgoing players become removed and cannot re-enter.
- Removal without replacement for:
  - normal bench removal;
  - injury;
  - dismissal.
- Ability to add an eligible bench player without replacement when the team is below
  the configured number of players on the pitch.
- Substitution, player-on, injury, dismissal, and player-off timeline events.
- Match-day squad operational states synchronized at kickoff and new periods.
- Live bench cards show minutes played and whether a player cannot return.
- Unit tests for eligibility, pitch capacity, dismissal, and rolling-substitution rules.

### Deferred to Pass 7

- Goals and assists.
- Score updates and corrections.
- Undo of recent match actions.
- Event editing from the live timeline.


---

<a id="pass-7-goals-assists-score-correction-timeline-and-undo"></a>

## Pass 7 — Goals, assists, score correction, timeline and undo

### Starting point

This pass was built from the Pass 6 project revision.

### Implemented

- Goal action from an on-pitch player's action sheet.
- Quick “Our goal” flow with scorer and optional assist selection.
- Opponent-goal confirmation.
- Atomic event insertion and score refresh in Room transactions.
- Score values are derived from persisted `OUR_GOAL` and `OPPONENT_GOAL` events.
- Manual score correction adds or removes timeline goal events, keeping both views consistent.
- Undo removes the latest scoring event and immediately refreshes the score.
- Match/timeline navigation inspired by the supplied mobile screenshot.
- Reverse-chronological live timeline for goals, substitutions, player entries,
  injuries, dismissals and removals.
- Own-goal event editing:
  - change scorer;
  - change or remove assist;
  - move the event time by one-minute steps;
  - delete the event.
- Opponent-goal deletion from the timeline.
- Unknown-scorer and no-assist support.
- Unit tests for score counting, attribution validation and correction deltas.

### Data model

No Room schema migration was required. Pass 7 uses the existing `events` table fields:

- `type` identifies our or the opponent's goal;
- `playerId` stores the scorer;
- `relatedPlayerId` stores the assisting player;
- `timestampMs` stores logical match time;
- `periodNumber` stores the active period.

### Deferred to Pass 8

- CameraX preview and recording.
- Camera and microphone permissions.
- Foreground segmented recording.


---

<a id="pass-8-camerax-recording"></a>

## Pass 8 — CameraX recording

### Starting point

This pass was built from the Pass 7 project revision.

### Implemented

- Camera tab in the live match interface, based on the supplied mobile reference.
- CameraX preview using `PreviewView`.
- Back/front camera selection.
- Torch control for the back camera.
- Runtime camera permission handling.
- Optional microphone audio with separate runtime permission handling.
- Android 13 notification permission request.
- Foreground camera/microphone service for recording outside the visible camera tab.
- Persistent foreground notification with a stop-and-save action.
- HD H.264/MP4 recording through CameraX `Recorder`.
- Videos saved to the system MediaStore under `Movies/MatchReview`.
- Every start/stop creates a separate persisted recording segment.
- Segment metadata includes:
  - match-clock start and end;
  - CameraX recording duration;
  - content URI;
  - orientation;
  - audio setting;
  - bytes recorded;
  - completion, interruption, or failure status.
- Interrupted open segments are marked on service recovery.
- Live recording timer and match-clock overlay.
- Segment list with recording status and errors.
- Unit tests for recording-state and segment-time rules.

### Android behavior

The recording service declares the `camera|microphone` foreground-service types. Recording
must be started while MatchReview is visible, as required by modern Android versions. Once
started, the foreground notification keeps the recording session active when switching tabs
or briefly leaving the app.

### Deferred to Pass 9

- Mapping each match event to the active video segment and recording offset.
- Camera-mode goal/substitution shortcuts over the preview.
- Review playback that jumps directly to the correct recorded segment.
- Clip-oriented event navigation and thumbnails.


---

<a id="pass-9-video-event-integration"></a>

## Pass 9 — Video/event integration

### Starting point

This pass was built from the Pass 8 project revision.

### Implemented

- Match events are linked to the active recording segment when they are created.
- Precise video offsets use wall-clock recording/event timestamps for new recordings.
- A Room 2→3 migration adds the recording start epoch without deleting existing data.
- Existing recordings and events are backfilled using logical match time when wall-clock
  metadata is unavailable.
- Goal corrections remap the event to the appropriate clip.
- Recording finalization retries mapping for events created while a clip was being prepared.
- Camera preview quick actions inspired by the supplied reference:
  - own-team goal;
  - substitution;
  - opponent goal.
- Timeline cards show clip availability and the event's offset in the video.
- Tapping **Clip** opens Media3 playback and seeks directly to the event.
- Playback supports previous/next mapped-event navigation, including events in other segments.
- Mapping and playability unit tests.

### Mapping behavior

For recordings created in Pass 9, the event's real occurrence time is compared with the
recording's persisted wall-clock start. This remains accurate while the match clock is paused.
For older Pass 8 clips, MatchReview falls back to the event and segment match-clock values.

### Deferred to Pass 10

- Storage-pressure warnings and recording deletion.
- Export/share bundles and privacy controls.
- Device/instrumentation migration tests.
- Broader accessibility and interruption hardening.


---

<a id="pass-10-hardening-and-release"></a>

## Pass 10 — hardening and release

Pass 10 completes the planned implementation.

### Reliability and recovery

- App startup now marks orphaned `PREPARING` or `RECORDING` rows as interrupted after an unexpected process/device stop.
- Recording start is guarded in both the Compose camera UI and the foreground service.
- Less than 1 GB free storage displays a warning.
- Less than 250 MB free storage blocks new recordings while still allowing an active recording to be stopped and saved.
- The GitHub Actions build now runs JVM unit tests before assembling the APK.

### Export and privacy

- Match review includes a CSV export through Android's Storage Access Framework.
- Exports contain match metadata, timeline events, player participation intervals, and recording references.
- The UI displays a privacy reminder before export.
- Match deletion requires explicit confirmation and deletes app-created MediaStore recording clips.
- Imported source videos remain owned by the user; the app releases its persisted read grant when the match is deleted.
- Android cloud/device backup and cleartext network traffic are disabled.

### Accessibility

- Camera switching, recording, and torch controls now expose explicit accessibility labels.
- Primary release flows retain text-labelled actions and minimum Material touch targets.
- An instrumentation smoke test checks that the dashboard's primary actions are exposed.

### Verification added

- Storage threshold unit tests.
- CSV escaping and safe-filename unit tests.
- Android migration test for database version 2 to 3.
- Android launch/accessibility smoke test.

Run local JVM tests and build:

```bash
./gradlew testDebugUnitTest assembleDebug
```

Run device/emulator tests:

```bash
./gradlew connectedDebugAndroidTest
```

A physical-device release check should still cover camera permission denial, microphone denial,
low-storage behaviour, device rotation, screen locking, incoming calls, process termination,
and deleting clips on the Android versions supported by the project.


---

<a id="pass-11-changes"></a>

## Pass 11 — changes

- Added a confirmed **Remove team** action to the team screen.
- Player cards are tappable and open an editor for name, shirt number, position,
  preferred foot, and notes.
- Added a confirmed player removal action.
- Simplified match-day player selection: select players directly from the team list,
  with **Select all** and **Clear selection** actions.
- Added explicit 3v3, 5v5, 8v8, and 11v11 match-size presets with legal formations.
- The 8v8 formation list now includes **2-4-1**.
- Lineup placement supports long-press drag-and-drop and retains tap-based and
  accessibility-safe placement controls.
- Enlarged the lineup pitch and subsequently removed the scrolling parent in Pass 12
  so drag gestures are handled reliably.


---

<a id="pass-12-reliable-long-press-drag-and-drop-and-space-optimised-pitch"></a>

## Pass 12 — reliable long-press drag-and-drop and space-optimised pitch

### Changes

- Removed the vertically scrolling parent from the lineup editor so it no longer
  competes with player drag gestures.
- Drag coordinates use the pointer's absolute position on every event instead of
  accumulated deltas.
- After a long press, players can be dragged:
  - from the bench onto the pitch;
  - between pitch positions;
  - from the pitch back to the bench.
- The pitch and bench are one continuous drag surface with highlighted drop feedback.
- Reworked the lineup screen to devote nearly all available height to the pitch.
- Reduced header, bench, spacing, and action-control sizes.
- Reworked the live match screen:
  - compact 50 dp header;
  - compact score action row;
  - pitch expands into all remaining space;
  - 64 dp substitutes strip;
  - compact tabs and match controls;
  - removed the large unused gaps above the system navigation area.
- Corrected live pitch marker positioning so stored coordinates represent marker centres.

Tapping a player still opens placement or substitution controls as a fallback and for
users who do not use drag gestures.


---

<a id="pass-13-pass-13"></a>

## Pass 13 — Pass 13

### New match setup

- Competition, match size, formation, period count and period duration are copied from
  the most recently created match.
- Formation is selected from a legal list for the chosen match size; free-text illegal
  formations can no longer be entered.
- The view model also rejects an unsupported size/formation combination.
- Successful pitch placement no longer shows the "snapped to slot" snackbar. Error and
  cancellation feedback remains available.

### PDF match summary

- The review screen now offers **Download match summary (PDF)**.
- PDFs are created fully offline with Android's built-in `PdfDocument`.
- The summary includes match details, score, setup, timeline, player participation,
  recording totals, review notes and a privacy notice.
- CSV export remains available for detailed machine-readable data.

### Verification

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```


### Current package additions

- Added **2-4-1** to the legal 8v8 formation list, with unit coverage for setup
  validation and generated pitch rows.
- Added an adaptive **Pitch Replay** launcher icon, a monochrome themed-icon layer,
  and two documented alternative icon concepts.


---

<a id="pass-14-encrypted-backup-and-restore"></a>

## Pass 14 — Encrypted backup and restore

### Included

- A **Backup & restore** screen reachable from the dashboard and match review.
- Android Storage Access Framework integration through `CreateDocument` and `OpenDocument`.
- Password-encrypted `.mrbak` files using:
  - PBKDF2-HMAC-SHA256 key derivation;
  - a random 16-byte salt;
  - AES-256-GCM authenticated encryption;
  - 150,000 PBKDF2 iterations.
- A compressed ZIP payload with:
  - `metadata.json`;
  - `database.json`;
  - `media_manifest.json`;
  - optional video files.
- Metadata for backup-format version, Room database version, app version, and UTC creation time.
- Full export of the ten Room tables.
- Transactional replace-on-restore so a failed database import rolls back.
- Validation before replacement, including supported versions and required tables.
- Restore protection while a match or recording is active.
- Explicit confirmation before existing data is replaced.
- Optional video inclusion; videos remain excluded by default.
- Restored packaged videos are copied into app-private storage and their URI references are remapped.
- Warnings when referenced media cannot be read.
- JVM tests for encryption round trips and absence of plaintext payload data.

### Cloud behaviour

MatchReview has no cloud account, server, background sync, or vendor lock-in. Android's
document picker delegates storage to providers installed by the user. A backup can
therefore be stored in Google Drive, OneDrive, another document provider, or local
storage without giving MatchReview direct access to a cloud account.

### Privacy and recovery

All backups require a password of at least six characters. The password is not stored
by MatchReview and cannot be recovered. Video backup remains opt-in because recordings
may contain identifiable people and can create very large files.

### App version

- `versionName`: `1.1.0`
- `versionCode`: `2`
- Room database: `4` (unchanged; backup does not alter the entity schema)
- Backup format: `1`

### Verification

```bash
./gradlew testDebugUnitTest assembleDebug
```

Physical-device tests should cover local files, the club's chosen document provider,
large video packages, incorrect passwords, damaged files, and process interruption.


---

<a id="pass-15-gradle-plugin-resolution-hardening"></a>

## Pass 15 — Gradle plugin resolution hardening

### Changes

- Added canonical Android and Kotlin plugin versions to
  `pluginManagement.plugins` in `settings.gradle.kts`.
- Retained the explicit root-project plugin versions in `build.gradle.kts`.
- This deliberate duplication makes plugin resolution robust when module plugin
  declarations omit versions and gives clearer protection against an incomplete upload.
- Added build troubleshooting guidance to `README.md`.
- Advanced the Android release identifier to `versionName 1.1.1` and `versionCode 3`.

### Expected project layout

```text
.github/
app/
gradle/
build.gradle.kts
settings.gradle.kts
gradlew
gradlew.bat
```

Upload the **contents** of the extracted project folder to the GitHub repository root.
Do not upload only the `app` folder and do not keep a stale `build.gradle.kts` from an
older repository revision.

### Verification

```bash
./gradlew projects --no-daemon
./gradlew testDebugUnitTest assembleDebug --no-daemon
```

The first command must resolve `com.android.application` before Android compilation can
begin.


---

<a id="pass-16-explicit-substitution-selection"></a>

## Pass 16 — Explicit substitution selection

- Replaced automatic first-bench-player selection with one substitution sheet.
- The coach must explicitly choose both **Player off** and **Player on**.
- Player cards can still preselect the player that initiated the action.
- Confirmation names both players before the transaction is submitted.
- No Room schema change.


---

<a id="pass-17-fast-goal-capture"></a>

## Pass 17 — Fast goal capture

- The primary **Our goal** action now records an unattributed goal immediately.
- A snackbar confirms the event and offers **Add details**.
- Choosing Add details opens the existing scorer/assist editor for the newly created event.
- The DAO now returns the inserted event ID so the UI edits the correct goal.
- Unknown goals use the clearer note `Scorer not assigned`.


---

<a id="pass-18-safer-live-controls"></a>

## Pass 18 — Safer live controls

- Increased primary match-day action targets to at least 48 dp.
- Increased score actions to a 56 dp action band.
- Added confirmation before ending a period.
- Removed Finish from the live/paused control row; Finish is offered after a period ends.
- Added a short interaction lock to reduce duplicate taps.


---

<a id="pass-19-persistent-recording-visibility"></a>

## Pass 19 — Persistent recording visibility

- Added a recording status chip above all live tabs.
- The chip shows Preparing, REC elapsed time, audio/silent state, Saving, or Error.
- Tapping the chip opens the Camera tab.
- Recording state is no longer visible only inside the Camera tab.


---

<a id="pass-20-confirmed-operation-feedback"></a>

## Pass 20 — Confirmed operation feedback

- Added snackbars for quick goals, opponent goals, undo, substitutions, and period ending.
- Review saving now displays `Saving…` and only displays `Saved` after persistence completes.
- Live actions use a short lock while an operation is being dispatched.
- Backup and restore retain explicit working/success/error panels.


---

<a id="pass-21-contextual-navigation-and-continuation"></a>

## Pass 21 — Contextual navigation and continuation

- Bottom navigation is now limited to Home, Teams, and Matches.
- Nested screens receive contextual titles and a Back action.
- Match cards route according to status: setup, live match, or review.
- Match cards display the next recommended action.
- A single available team is selected automatically during match creation.


---

<a id="pass-22-lineup-discoverability"></a>

## Pass 22 — Lineup discoverability

- Increased lineup footer actions from 40 dp to 48 dp.
- Renamed compact actions to Auto-place, Squad, and Match day.
- Added confirmation before auto-placement overwrites manual positions.
- Persistent instructions now teach tap-to-place first and long-press drag second.
- The player sheet displays each actual formation slot, including 2-4-1 slots.


---

<a id="pass-23-restore-preflight"></a>

## Pass 23 — Restore preflight

- Added encrypted backup inspection before destructive restore.
- The preview validates the password, format, database version, and required tables.
- It displays creation time, source app version, counts, and media inclusion.
- Destructive confirmation is shown only after successful inspection.
- The preview reminds the user to create a safety backup first.


---

<a id="pass-24-backup-confidence"></a>

## Pass 24 — Backup confidence

- Added last-successful-backup metadata using app preferences.
- Dashboard and Backup & restore show whether a backup has ever succeeded.
- The UI records whether the last successful backup included videos.
- Added show/hide controls for backup passwords.
- Password fields clear after a successful backup or restore.


---

<a id="pass-25-accessibility-and-privacy-baseline"></a>

## Pass 25 — Accessibility and privacy baseline

- Replaced compact custom live-tab buttons with semantic Material tabs.
- Removed emoji/Unicode labels from critical camera quick actions.
- Increased camera quick-action touch targets.
- Camera audio now defaults to off.
- The audio control explains when microphone permission will be requested.
- Release version advanced to 1.2.0 (`versionCode = 4`).


---

<a id="pass-26-match-day-readiness-check"></a>

## Pass 26 — Match-day readiness check

- Added a dedicated readiness screen between lineup editing and the live match.
- The screen summarizes opponent, format, formation, timing, venue, selected players, starters, and free storage.
- It warns when the starting lineup is underfilled or no goalkeeper role can be detected.
- Camera audio expectations are stated before the coach opens match day.


---

<a id="pass-27-transactional-lineup-swaps"></a>

## Pass 27 — Transactional lineup swaps

- Added an explicit **Swap with a player** action to the lineup placement sheet.
- Bench-to-pitch and pitch-to-pitch swaps preserve the destination position and role.
- Both placements and squad states are saved in one Room transaction.
- Added unit coverage for bench-to-pitch swap behavior.


---

<a id="pass-28-focused-match-review-sections"></a>

## Pass 28 — Focused match review sections

- Split the long review workflow into **Summary**, **Timeline**, **Video**, and **Data** sections.
- Raw event codes are converted to readable labels.
- `Tag moment` is disabled until an imported video player is available.
- Export, backup, and deletion actions are isolated in the Data section.


---

<a id="pass-29-squad-availability-controls"></a>

## Pass 29 — Squad availability controls

- Restored match-specific availability controls for Available, Injured, Suspended, and Unavailable.
- Added availability filters alongside player search.
- Unavailable players cannot be selected until their status is changed to Available.
- Changing a selected player to an unavailable state safely removes them from the match squad.


---

<a id="pass-30-match-search-and-status-filters"></a>

## Pass 30 — Match search and status filters

- Added search across opponent, team, venue, and competition.
- Added All, Active, Drafts, and Finished status filters.
- Added a clear empty result state without hiding the New match action.
- Match cards retain status-aware continuation actions.


---

<a id="pass-31-safer-match-date-entry"></a>

## Pass 31 — Safer match-date entry

- Replaced free-form date typing with the Material date picker.
- Dates remain stored in the existing `yyyy-MM-dd` format.
- The picker prevents malformed dates without changing the Room schema.
- The single-team automatic selection from the previous release is retained.


---

<a id="pass-32-backup-size-preview"></a>

## Pass 32 — Backup size preview

- Added reactive totals for stored recording count and bytes.
- The backup screen now estimates package size before the document picker opens.
- The estimate clearly changes when videos are included or excluded.
- Added unit tests for media-inclusive and media-excluding estimates.


---

<a id="pass-33-durable-imported-video-linking"></a>

## Pass 33 — Durable imported-video linking

- Imported videos are linked only after persistable read access is granted.
- Providers that cannot grant lasting access now produce a visible recovery message.
- The app no longer silently stores a link that is likely to break after restart.
- `Tag moment` remains unavailable until playback is ready.


---

<a id="pass-34-permission-recovery"></a>

## Pass 34 — Permission recovery

- Camera permission failures now appear in an actionable error card.
- Users can retry the contextual permission request.
- Users can open MatchReview's system app settings directly after permanent denial.
- Microphone permission remains conditional on explicitly enabling audio.


---

<a id="pass-35-navigation-icons-and-localization-baseline"></a>

## Pass 35 — Navigation icons and localization baseline

- Replaced Unicode bottom-navigation symbols with semantic Material icons.
- Replaced the text Back control with an accessible Up icon.
- Moved top-level navigation and critical live-action labels into Android string resources.
- Advanced the Android release to 1.3.0 (`versionCode = 5`) without a database or backup-format change.

## Pass 36 — Match-data integrity checks

**Status:** Completed  
**Completed:** 2026-09-27  
**Version impact:** App 1.4.0; database unchanged

### Added or changed
- Added `MatchIntegrityRules` for score/event consistency, lineup capacity, participation intervals, completed recordings, and event/video references.
- Added an integrity panel to match review so detected inconsistencies are visible before export.

### Tests and verification
- Added `MatchIntegrityRulesTest`.

### Deferred or known limitations
- Lineup checks in the review panel use the data already loaded by that screen; a later pass can add a full database-wide repair tool.

## Pass 37 — Duplicate live-command protection

**Status:** Completed  
**Completed:** 2026-09-27  
**Version impact:** App only

### Added or changed
- Added `LiveCommandGate`, a bounded thread-safe duplicate-action window.
- Applied it to kickoff, pause, resume, goals, and substitutions in `MainViewModel`.
- Existing Room transactions remain the authoritative persistence boundary.

### Tests and verification
- Added `LiveCommandGateTest`.

### Deferred or known limitations
- The gate protects accidental rapid repeats in one process; durable cross-process command receipts remain future work.

## Pass 38 — Clock recovery confidence

**Status:** Completed  
**Completed:** 2026-09-27  
**Version impact:** App only

### Added or changed
- Added `ClockRecoveryRules` to classify same-boot recovery, reboot recovery, and suspicious wall-clock drift.
- Live match now displays a warning when clock recovery needs operator confirmation.

### Tests and verification
- Added same-boot and wall-clock-drift tests.

### Deferred or known limitations
- The current warning does not yet provide a dedicated clock-correction dialog.

## Pass 39 — Recording reconciliation

**Status:** Completed  
**Completed:** 2026-09-27  
**Version impact:** App only

### Added or changed
- Added `MediaIntegrityRules` for stale active segments, missing URIs, empty completed files, and failures without diagnostics.
- Added the active recording-segment identifier to the camera state API.
- Match review now displays recording integrity warnings.

### Tests and verification
- Added `MediaIntegrityRulesTest`.

### Deferred or known limitations
- File existence is still provider-dependent; the current pass validates persisted metadata without deleting or rewriting media automatically.

## Pass 40 — Backup integrity format 2

**Status:** Completed  
**Completed:** 2026-09-27  
**Version impact:** Backup format 2; format 1 remains readable

### Added or changed
- New backups include a SHA-256 digest of `database.json` inside the authenticated encrypted package.
- Inspection and restore verify the digest before parsing or replacing data.
- Metadata validation accepts legacy format 1 and current format 2.
- Backup UI reports the current format dynamically.

### Tests and verification
- Existing authenticated-encryption tests remain applicable.
- Static checks confirm all restore paths verify format-2 content before database replacement.

### Deferred or known limitations
- Media entries rely on the AES-GCM envelope for whole-package integrity; per-media digests can be added in a future format.

## Pass 41 — Privacy-safe CSV exports

**Status:** Completed  
**Completed:** 2026-09-27  
**Version impact:** App only

### Added or changed
- Added `ExportPrivacyOptions` and `ExportPrivacyRules`.
- CSV exports default in the review UI to redacting player names, free-text notes, and media URIs.
- Coaches can explicitly disable redaction when an identifiable report is required.
- Export privacy notices now reflect the selected mode.

### Tests and verification
- Added `ExportPrivacyRulesTest`.
- Existing formatter calls remain source-compatible through default options.

### Deferred or known limitations
- PDF redaction remains separate from CSV redaction and should be aligned in a later pass.

## Pass 42 — Accessible live announcements

**Status:** Completed  
**Completed:** 2026-09-27  
**Version impact:** App only

### Added or changed
- Added `LiveAnnouncementRules`.
- The scoreboard is an accessibility polite live region whose description changes with the score, not every clock tick.
- Added a rule for minute-boundary clock announcements for future targeted use.

### Tests and verification
- Added `LiveAnnouncementRulesTest`.

### Deferred or known limitations
- Full TalkBack, switch-access, and hardware-keyboard testing still requires Android devices or emulators.

## Pass 43 — Recording storage budget

**Status:** Completed  
**Completed:** 2026-09-27  
**Version impact:** App only

### Added or changed
- Added `StorageBudgetRules` with a 512 MiB safety reserve and conservative recording-time estimate.
- Camera UI displays estimated safe recording minutes.
- Recording start now requires both the existing storage-health threshold and the new safety budget.

### Tests and verification
- Added `StorageBudgetRulesTest`.

### Deferred or known limitations
- The estimate uses a conservative default bitrate rather than device-specific historical measurements.

## Pass 44 — Practice match onboarding

**Status:** Completed  
**Completed:** 2026-09-27  
**Version impact:** App only; database schema unchanged

### Added or changed
- Added an offline `Start a practice match` action to the dashboard.
- Practice creation is transactional and creates or reuses a team, ensures eight sample players, selects the squad, and creates an 8v8 2-4-1 training match.
- The practice match opens at squad review before kickoff.

### Tests and verification
- Added `PracticeMatchRulesTest`.
- Practice templates have unique shirt numbers and include a goalkeeper.

### Deferred or known limitations
- Practice records are ordinary local records and are not automatically deleted.

## Pass 45 — Season summary

**Status:** Completed  
**Completed:** 2026-09-27  
**Version impact:** App only

### Added or changed
- Added `SeasonSummaryRules` for completed-match results and goals.
- Dashboard now shows played, won, drawn, lost, goals for, and goals against.
- Draft and active matches are excluded from the summary.

### Tests and verification
- Added `SeasonSummaryRulesTest`.

### Deferred or known limitations
- The dashboard currently summarizes all completed matches; filtering by team and named season can be added without changing the calculation rules.

