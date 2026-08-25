# Dotto

A native Android implementation of Dots-and-Boxes, built as the foundation of an
extensible game platform. Phase 1 ships **Human vs. local, offline AI** — the
architecture is deliberately shaped so Human-vs-Human, nearby, and online
multiplayer can be added later without rewriting the game engine.

Dotto requires no network connection, no account, and no backend. It works
fully in airplane mode.

---

## 1. What Dotto is

Players take turns drawing a horizontal or vertical line between two adjacent
dots. Completing the fourth side of a box captures it for that player, shows
their initial inside it, increases their score, and grants them another turn
(one extra turn per move, even if the move completed two boxes at once). When
every line on the board has been drawn, whoever captured the most boxes wins;
equal scores are a draw.

The board size is configurable (3×3 up to 7×7 dots in the Setup screen), and
you can play against three AI difficulties: Easy, Medium, and Hard.

---

## 2. Architecture

```
app
├── domain            — pure Kotlin game rules. No Android imports at all.
│   ├── model          (DotCoordinate, Line, BoxCoordinate, Player, GameState, GameMove, MoveResult...)
│   ├── board           (BoardConfig, BoardState, BoardGeometry)
│   ├── rules            (GameRules, ClassicDottoRules)
│   ├── engine             (GameEngine, GameEngineImpl — the authoritative rulebook)
│   ├── events               (GameEvent — GameStarted, MoveMade, BoxCompleted, TurnChanged, ...)
│   ├── session                (GameSession — lifecycle/history wrapper around a game)
│   ├── player                  (PlayerController, HumanPlayerController)
│   └── ai                       (AiStrategy + Easy/Medium/Hard, BoardAnalyzer, MinimaxSearchStrategy, AiPlayerController)
│
├── application        — use cases and UI-facing state, still Android-light.
│   ├── usecase          (StartNewGame, MakeMoveUseCase, RestartGame)
│   └── state              (SetupConfig, DottoUiState)
│
├── infrastructure/future — empty placeholders (p2p, online) marking where
│                            future transports plug in. Nothing implemented here on purpose.
│
└── presentation        — Jetpack Compose UI only. Talks to the domain
    ├── game               exclusively through DottoViewModel + use cases.
    ├── setup
    ├── result
    ├── components          (DottoBoard canvas renderer, BoardGeometryMapper, ScorePanel, TurnIndicator)
    └── theme                (Color.kt, PlayerPresentation, Theme.kt)
```

**The one rule that shapes everything else:** the domain layer (`domain/`)
never imports Compose, Canvas, Activities, ViewModels, Context, or anything
Android. It is plain Kotlin, unit-testable on a JVM with no emulator. The
`presentation` layer never contains game rules — no score increments, no
"whose turn is it" logic, no winner calculation live in a Composable.

### Why this split helps later

- **Human vs Human / more players**: `GameEngineImpl` already round-robins
  over `GameState.players` in list order — a 3rd or 4th player is just a
  longer list, no engine changes.
- **Nearby / online multiplayer**: moves flow through `PlayerController`
  (`suspend fun selectMove(gameState): GameMove`). The engine doesn't know or
  care whether a move came from a human tap, an AI strategy, or a future
  `NearbyPlayerController` / `OnlinePlayerController` reading a move off the
  wire. Only new controller implementations are needed — `GameEngineImpl` is
  untouched.
- **Replay / history / stats**: `GameState` is fully immutable and
  serializable-shaped, `MoveResult`/`GameEvent` give a complete, structured
  account of every move, and `GameSession` already tracks a `moveHistory`
  list. A replay feature is "feed the move list back through the engine",
  not a new game-logic implementation.
- **Persistence**: nothing is written to disk in Phase 1 (as required), but
  because state is plain immutable data classes, adding a Room/DataStore
  layer later is additive — it doesn't require touching `domain/`.

---

## 3. Game engine design

`GameEngine` (interface) / `GameEngineImpl` (implementation) is the single
authoritative source of truth. Given a `GameState` and a `GameMove`, it
deterministically produces a `MoveResult` containing the new state, which
boxes were completed, whether an extra turn was granted, and the full list of
`GameEvent`s that occurred (for UI animation / logging / future replay).

Key design choices:

- **`BoardGeometry`** is a stateless object computing all topology (which
  lines exist for a grid size, which lines border a box, which boxes border a
  line, how many sides of a box are drawn) purely from `BoardConfig`. Nothing
  is hard-coded per board size — an N×M grid "just works" because geometry is
  derived, not enumerated by hand.
- **`GameRules`** is injected into the engine (`ClassicDottoRules` is the only
  implementation today). It answers "is this move legal", "what boxes does
  this move complete", "does completing N boxes grant an extra turn", and
  "is the game over" — so a future rule variant is a new `GameRules`, not a
  rewrite of `GameEngineImpl`'s control flow.
- **Turn management lives entirely in the engine.** A move that completes
  zero boxes passes the turn to the next player in the list. A move
  completing one or more boxes grants exactly one extra turn to the same
  player — per the spec, multiple simultaneous captures still only grant one
  extra turn, they just also score more.
- **Game completion** is decided by "have all playable lines been drawn",
  never by comparing scores, matching the spec's requirement.
- **Invariants enforced by construction**: `score[player] == boxes owned by
  player` (computed by summing captures in the same operation that draws the
  line, never separately), `drawnLines.size <= totalLines`,
  `completedBoxes.size <= totalBoxes`, and every completed box has exactly 4
  drawn boundary lines and exactly one owner (`Map<BoxCoordinate, PlayerId>`
  makes a second owner structurally impossible).

---

## 4. AI design

The AI is layered rather than one function per difficulty:

```
AiStrategy (interface: chooseLine(gameState, playerId, random) -> Line)
 ├── EasyAiStrategy     — uniform random among legal moves
 ├── MediumAiStrategy   — capture > safe move > any move
 └── HardAiStrategy     — capture > minimax-searched safe move > minimax-searched sacrifice
        uses:
        ├── BoardAnalyzer      — shared read-only queries (capturing lines, safe lines,
        │                         lines that would hand the opponent a box, boxes at 3 sides)
        ├── MoveEvaluator      — HeuristicMoveEvaluator scores a board position
        │                         (score differential, penalized by boxes left exposed at 3 sides)
        └── SearchStrategy     — MinimaxSearchStrategy: alpha-beta search that correctly
                                  understands Dotto's turn rule (completing a box means the
                                  SAME player searches one ply deeper before the mover flips)
```

- **Easy**: picks uniformly at random among all currently legal lines.
- **Medium**: takes an immediate capture if one exists; otherwise plays a
  "safe" line (one that doesn't leave any box at 3 drawn sides for the
  opponent to snap up); otherwise, if no safe line exists, plays any legal
  line.
- **Hard**: same capture-first priority, but when multiple captures or
  multiple safe lines are available it runs a depth-limited minimax search
  (with alpha-beta pruning) to pick the move that leads to the best board
  evaluation several plies out — including the possibility of "sacrificing"
  the smallest/least costly opening when no safe move exists at all. Search
  depth scales down as the board grows (`totalBoxes <= 9 → depth 4`, `<=16 →
  3`, `<=25 → 2`, else `1`) specifically so play never noticeably blocks the
  UI on larger boards.

**Difficulty is data, not branching UI code.** `AiDifficulty` is a plain enum
and `AiStrategyFactory` is the only place that maps difficulty → strategy;
nothing in the Compose layer knows what "Hard" means algorithmically.

**Determinism for testing.** Every `AiStrategy.chooseLine` takes a
`kotlin.random.Random` parameter. Production play uses `Random.Default`
inside `AiPlayerController`; tests pass a fixed seed and get fully
reproducible AI decisions.

**Off the UI thread & cancellable.** `AiPlayerController` is a suspend-based
`PlayerController`. It adds a randomized 300–800ms "thinking" delay (per the
spec) via `kotlinx.coroutines.delay`, then computes the move. Because the
whole turn loop runs inside `viewModelScope.launch` in `DottoViewModel`, and
that job is explicitly cancelled (`aiJob?.cancel()`) whenever a new game
starts, an in-flight AI computation from a previous game can never apply a
stale move to a new one.

---

## 5. Testing strategy

**Note on this build environment:** this project was written and reviewed
without access to a full Android/Gradle build (this sandbox's network
allowlist doesn't include Google's Maven repositories or the Gradle
distribution service, so `./gradlew` cannot actually be invoked here). What
*was* verified directly, using a real Kotlin 1.9.24 compiler fetched from
GitHub releases:

- The entire `domain/` and `application/` layer (37 files — board geometry,
  rules, engine, all three AI strategies including the minimax search, use
  cases) **compiles cleanly** against the actual Kotlin compiler.
- A standalone runtime harness (not shipped with the app) exercised the real
  engine and AI logic directly on the JVM: full randomized games across
  board sizes 3–7 with every invariant checked after every single move
  (`score == boxes owned`, lines never decrease, a completed box always has
  exactly 4 drawn sides, bounded totals), explicit turn-switching scenarios
  (normal move, single capture with a genuine extra turn, simultaneous
  two-box capture granting exactly one extra turn, a capture that also ends
  the game and correctly reports no further extra turn), move rejection
  (wrong player, duplicate line, out-of-range line), and all three AI
  difficulties playing complete games while always producing legal moves.
  **3,203 assertions passed.** This process caught and fixed two real bugs
  in test *assumptions* about turn order that would otherwise have shipped
  as incorrect tests.

**What is checked in to the repo** is a conventional JUnit 4 + Truth suite
under `app/src/test/java/com/dotto/app/domain/`, covering the same ground as
the runtime harness above (board geometry, engine turn/scoring/completion
rules, rejection paths, and per-difficulty AI behavior) in the form the
project would actually run under `./gradlew test` on a machine with normal
Android tooling access. Because that Gradle invocation itself could not be
performed in this environment, treat the checked-in suite as reviewed and
logic-equivalent to the independently-verified harness above, but not
independently re-executed via Gradle+JUnit here — please run
`./gradlew testDebugUnitTest` locally to get an authoritative pass/fail from
the real toolchain before relying on it in CI.

---

## 6. Where future multiplayer plugs in

Nothing in this phase implements networking, accounts, Firebase, or a
server, per the brief. The seams are already in place:

- **`PlayerController`** is the only thing the engine/ViewModel talk to for
  "give me a move". `HumanPlayerController` and `AiPlayerController` exist
  today; `NearbyPlayerController` and `OnlinePlayerController` are future
  implementations of the exact same interface.
- **`infrastructure/future/p2p` and `infrastructure/future/online`** are
  present as empty package placeholders marking where a future
  `GameTransport` abstraction and its implementations would live — nothing
  is implemented there now.
- **Moves, not UI state, are what would cross the wire.** Because
  `GameMove` is a tiny serializable-shaped data class and `GameEngine` is a
  pure function of `(GameState, GameMove) -> MoveResult`, a future
  networked game exchanges moves and lets each device's local engine
  validate and apply them — never syncing raw UI state.
- **`GameSession.moveHistory`** already accumulates every applied move,
  which is what a future replay feature or reconnect-and-resync flow would
  replay through the engine to reconstruct game state.

---

## 7. Development

This project targets:

- Kotlin 1.9.24, Android Gradle Plugin 8.5.2, Gradle 8.7
- Compose BOM 2024.06.00, Material 3
- `compileSdk`/`targetSdk` 34, `minSdk` 26
- JDK 17

To build locally (with normal Android SDK / network access):

```
cp local.properties.sample local.properties   # then point sdk.dir at your Android SDK
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Install and play:

```
./gradlew installDebug
```

The app requests no permissions and makes no network calls — it will run
correctly with the device in airplane mode.

---

## 8. Known limitations / recommended next steps

- **Not build-verified end-to-end in this environment.** The domain/AI/use-
  case layers were compiled and run directly with the Kotlin compiler and
  passed thousands of runtime assertions (see §5), but the Compose UI layer
  (screens, `DottoBoard` canvas renderer, touch hit-testing) has only been
  reviewed by hand, not compiled against the real Android/Compose SDK or run
  on a device/emulator, because this sandbox cannot reach Google's Maven
  repositories. Please run a full `./gradlew assembleDebug` and play a game
  on-device before considering Phase 1 fully verified.
- **Drawn-line color is currently player-agnostic.** Lines render in a
  single neutral color once drawn (ownership is communicated via box fill
  color and initials, and the score panel); per-line-owner coloring would
  need the engine to track which player drew each line, which `BoardState`
  doesn't do today. Worth adding if a future design wants "trail" coloring.
- **Hard AI's minimax evaluator is a simple heuristic** (score differential
  minus exposed-box count), not full chain-counting/parity analysis from
  combinatorial game theory. It plays soundly and never hands away obvious
  captures, but an expert Dots-and-Boxes player could still out-maneuver it
  on long chains. A natural "Expert"/"Master" tier later would swap in a
  chain-aware `MoveEvaluator` without touching `AiStrategy`/`SearchStrategy`
  contracts.
- **No persistence, stats, or replay UI** — intentionally out of scope for
  Phase 1, but the data shapes (`GameState`, `GameEvent`, `GameSession`) were
  designed so those features are additive later.
- **Accessibility** is partially addressed (score panel has content
  descriptions, current-turn state is announced, player identity doesn't
  rely on color alone since initials are always shown) but has not been
  audited with TalkBack on a device.
