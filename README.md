# Dotto

A native Android implementation of Dots-and-Boxes, built as a modern, extensible game platform. Dotto combines pure-logic game rules with a visually striking neon aesthetic and robust technical foundations.

Phase 1 includes **Human vs. Local AI** play, a **Room-based persistence** layer for session resumption, and **AdMob integration**.

---

## 1. Core Features

- **Classic Gameplay**: Draw lines, capture boxes, and win the grid.
- **Progressive Levels**: Start at Level 1 (3x3) and climb as high as you can. Your total score and highest level are persisted.
- **Intelligent AI**: Three difficulty levels (Easy, Medium, Hard) powered by Minimax search with alpha-beta pruning.
- **Session Resumption**: Close the app mid-game? Dotto remembers your level, the board state, and whose turn it was.
- **Atmospheric UI**: High-fidelity neon graphics, glassmorphism effects, and real-time starfield animations.
- **Tactile Feedback**: Integrated sound effects and haptics (vibration) for an immersive experience.
- **Monetized**: Clean AdMob banner integration that respects the game layout.

---

## 2. Technical Architecture

Dotto follows a strict Clean Architecture approach, ensuring the game rules are decoupled from the Android platform.

```
app
├── domain            — Pure Kotlin game rules. No Android imports.
│   ├── model          (DotCoordinate, Line, BoxCoordinate, Player, GameState, GameMove)
│   ├── board           (BoardConfig, BoardState, BoardGeometry)
│   ├── rules            (GameRules, ClassicDottoRules — the authoritative rulebook)
│   ├── engine             (GameEngine, GameEngineImpl — state machine orchestration)
│   ├── events               (GameEvent — MoveMade, BoxCompleted, TurnChanged)
│   └── ai                       (AiStrategy, SearchStrategy, BoardAnalyzer, MoveEvaluator)
│
├── application        — Use cases and UI-facing state.
│   ├── usecase          (StartNewGame, MakeMoveUseCase, RestartGame)
│   └── state              (SetupConfig, DottoUiState)
│
├── infrastructure     — Data persistence and external integrations.
│   ├── persistence      (Room Database: PlayerProgress and SavedMove history)
│   └── future            (Placeholders for p2p/online multiplayer transports)
│
└── presentation        — Jetpack Compose UI.
    ├── game               (GameScreen, DottoViewModel)
    ├── setup              (SetupScreen)
    ├── components          (DottoBoard canvas renderer, StarField, DottoAdView)
    ├── sound                (SoundManager: Sound effects and haptics)
    └── theme                (Material 3, Neon palette, PlayerPresentation)
```

### Key Architectural Benefits
- **Testable Rules**: The entire `domain/` layer is unit-testable on a JVM without an emulator.
- **Resilient State**: The `DottoViewModel` restores the game state by replaying `SavedMove` entities through the engine, ensuring consistency.
- **Flexible Payouts**: AdMob is integrated as a custom Compose component (`DottoAdView`) that handles loading states gracefully.

---

## 3. Game Engine & AI

### Authority
The `GameEngineImpl` is the single source of truth. It takes the current `GameState` and a `GameMove`, validating it against `ClassicDottoRules` and producing a `MoveResult`. This result includes the new state, completed boxes, and a list of structured `GameEvent`s for animations.

### AI Strategies
- **Easy**: Uniform random selection.
- **Medium**: Prioritizes immediate captures and avoids "bad" moves that hand boxes to the opponent.
- **Hard**: Uses depth-limited **Minimax search** with alpha-beta pruning. It evaluates the board based on score differential and potential exposure of boxes, allowing for strategic sacrifices.

---

## 4. Persistence (Room)

Dotto uses a local SQLite database via Room to ensure your progress is never lost:
- **PlayerProgress**: Tracks your name, total cumulative score, current level, and the player whose turn it is.
- **MoveHistory**: Every line drawn is saved. If the app is killed, the ViewModel re-runs these moves through the engine to reconstruct the exact board state upon return.

---

## 5. Development & Build

### Environment
- **Kotlin 1.9.24**, **Compose BOM 2024.06.00**, **Material 3**
- **Room Persistence Library 2.6.1**
- **Google Mobile Ads SDK 23.3.0**
- **Target SDK 35**, **Min SDK 26**

### Build Commands
To build and install the debug version:
```bash
./gradlew assembleDebug
./gradlew installDebug
```

To run the unit test suite:
```bash
./gradlew testDebugUnitTest
```

---

## 6. Permissions & Privacy
- **Internet & Network State**: Used exclusively for AdMob advertising.
- **Vibrate**: Used for tactile feedback (can be toggled off in settings).
- **Offline Play**: The core gameplay, progression, and AI are fully functional without a network connection.

---

## 7. Roadmap
- **Human-vs-Human**: Local pass-and-play support.
- **Nearby Multiplayer**: Playing against friends via Bluetooth/Wi-Fi Direct.
- **Cloud Stats**: Syncing total scores across devices.
- **Expert Difficulty**: Enhancing the AI with chain-counting and parity analysis.
