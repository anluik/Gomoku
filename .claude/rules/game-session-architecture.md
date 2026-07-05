# Rule: Game Session Architecture — Decisions & Backlog

> **For future Claude sessions:** Read this before continuing or expanding the real-time
> game layer (WebSocket sessions, game events, concurrency, persistence, scaling). It records
> *why* things are built this way and what was **deliberately deferred**, so you don't
> re-litigate settled decisions or undo an intentional trade-off. When the user asks to "continue"
> or "expand the app," treat the Backlog section as the working to-do list.

## TL;DR of the philosophy we agreed on

The app is a reactive Spring WebFlux + WebSocket Gomoku backend. We are building a **single-server
MVP that is architected to scale later**, not a distributed system now. The guiding invariant for
all future scaling is **single-writer-per-game** (serialize every mutation of one game through one
owner). For the MVP we approximate safety with **optimistic locking**, and we keep clean seams so
the bigger machinery can slot in without rewriting handler logic. Minimise races where it's cheap;
document the rest.

---

## Current architecture (what exists today)

- **WebSocket entry:** `GameSessionHandler` (`session/handlers/`) handles one connection. It merges
  an **action stream** (this client's messages) and a **broadcast stream** (fan-out for the game),
  and sends both back. On connect it registers presence + cancels any pending forfeit; on disconnect
  it decrements presence and (if the game is active) arms a forfeit timer — it does **not** fire a
  leave.
- **Connection is authoritative for `gameId`.** The `?gameId=` query param is the single source of
  truth; it was **removed from the message payload** so a client can't act on another game. One
  socket = one game (also = spectating by default).
- **Events are typed, polymorphic messages.** `GameEvent` is an abstract Jackson base keyed on
  `type`; subclasses: `JoinGameEvent`, `ResignEvent`, `AbortEvent`, `MoveEvent(x,y,moveSeq)`,
  `ChatEvent(text)`. `SessionMessageProcessor` deserializes + bean-validates the right subtype
  automatically. (There is **no `LEAVE_GAME` event** — see disconnect handling below — and **no
  incoming `START_GAME`**: the game auto-starts, see game start below.)
- **Membership vs presence.** `Game.players` is *durable* membership (set on JOIN, survives a
  disconnect — nobody can take your seat). Live connections are *ephemeral* presence, ref-counted
  per `(gameId, userId)` in `GamePresenceTracker` (in-memory) so multi-tab / refresh doesn't look
  like a leave.
- **Disconnect = abandonment, not leave.** A transport close never mutates `players`. When a
  player's **last** connection drops during an active game (`players.size()==2 && !isOver`),
  `AbandonmentService` broadcasts `PLAYER_DISCONNECTED` and arms a `Mono.delay(grace)` forfeit timer
  (`game.abandonment.grace-seconds`, default 30). Reconnect cancels it and broadcasts
  `PLAYER_RECONNECTED`. On expiry (still absent) the outcome depends on whether **both players have
  made at least one move**: until both have moved the game **aborts** (`GameResult(ABORT)`, no
  winner — it never really started); once both have moved the opponent **wins** (`GameResult(WIN)`)
  — **even if the opponent has also since disconnected**. A committed game (both moved) can never end
  in an abort. Guard-save-first with the same optimistic retry. Timers are in-memory/single-server.
- **Game start (auto).** The creator is seated as `player[0]` at REST creation, so only the opponent
  sends `JOIN_GAME`; when that join brings the game to 2 players, `JoinGameEventHandler` broadcasts
  `GAME_STARTED` (with player order / first mover). There is no client start command.
- **Ending a game as a player.** `MOVE` can end it (win, or `DRAW` when the board fills). Otherwise
  the leave/concede action depends on whether the caller has moved: **before their first move →
  `ABORT`** (`GameResult(ABORT)`, no winner; `AbortEventHandler`); **after their first move →
  `RESIGN`** (opponent wins). The two are complementary and mutually gated — the wrong one is
  rejected (`ABORT_NOT_ALLOWED` / `RESIGN_NOT_ALLOWED`). `ABORT` also works for a lone creator to
  cancel an un-joined game.
- **Handlers auto-discovered.** Each event has a `@Component` implementing `GameEventHandler` with
  opt-in default methods: `requiresActiveGame()`, `requiresParticipant()`, `retryOnConflict()`.
- **Concurrency = optimistic locking + retry.** `@Version` on `Game`; `GameSessionHandler` wraps the
  `findById → checks → handle` chain in a bounded retry (max 3) on `OptimisticLockingFailureException`
  when `handler.retryOnConflict()` is true. JOIN/MOVE/RESIGN/ABORT opt in; CHAT/spectate do not. The
  `AbandonmentService` forfeit path uses the same retry independently.
- **Guard-save-first principle (mandatory for retryable handlers):** perform the version-guarded
  `Game.save` *before* any non-idempotent side effect (inserting a `GameResult`, broadcasting), so a
  retry can't double-apply. `ResignEventHandler` was reordered for this.
- **MOVE:** turn ownership (move-count parity × player order) is the primary guard; `moveSeq` dedupes
  stale/duplicate deliveries; bounds + occupancy validated; win via `GameUtils.isWinningMove`
  (4-axis line scan); on win, persist `GameResult(WIN)` and broadcast `GAME_WON`. If the move fills
  the board with no winner it's a draw → `GameResult(DRAW)` + `GAME_DRAW`.
- **CHAT:** pure broadcast, no DB, no lock; allowed for players and spectators.
- **Broadcast seam:** `GameSessionManager` hides fan-out behind an in-memory `Sinks.Many` per game —
  the one class to swap for a distributed pub/sub later.

## Decisions & rationale (the "why")

| Decision | Why |
|---|---|
| Optimistic locking + retry, **not** a single-writer actor, for the MVP | Ships fast, keeps the existing request/response handler shape, no new distributed concepts. |
| `@Version` on **`Game` only**, not `BaseEntity` | Lowest blast radius. `@Version` makes a null-version doc save as an *insert* (duplicate-key risk) and makes `save()` able to throw. Only `Game` has the race; `User`/`Role`/`GameResult` write paths aren't conflict-aware and their data isn't disposable. |
| Typed polymorphic `GameEvent` subclasses | Clean per-type payloads/validation now; makes a future external transport a serialization concern, not a redesign. |
| Connection `gameId` authoritative, dropped from payload | Structurally removes the cross-game action vulnerability. |
| `moveSeq` sequence number added now | Cheap stale/duplicate rejection today; makes at-least-once delivery safe when an external queue is added later. |

## Agreed scaling direction (target end-state, NOT built yet)

Reachable behind today's seams without changing handler logic:
1. **Single-writer-per-game actor** — funnel all commands for a game through one ordered processor;
   eliminates races structurally (no locks needed).
2. **In-memory-authoritative game state + write-behind persistence** — active games live in memory,
   synced to Mongo on a policy (snapshot/terminal flush / event log), not per-command.
3. **Multi-server** — swap `GameSessionManager`'s in-memory `Sinks.Many` for **Redis Pub/Sub**, and
   route all connections for a game to one owning node via **game-affinity / consistent-hash
   sharding** (single-writer across the cluster).

---

## BACKLOG (deferred deliberately — pick up here when extending)

### Architecture / scale
- [ ] Single-writer-per-game command processor (actor) — the target invariant.
- [ ] In-memory-authoritative state + write-behind persistence (+ crash recovery / event log).
- [ ] Multi-server: Redis Pub/Sub in `GameSessionManager`; game-affinity routing/sharding.
- [ ] Cross-document atomicity: standalone Mongo has no transactions, so `GameResult` + `Game`
      updates aren't atomic. Mitigated today by guard-save-first + recompute-on-read. True atomicity
      needs a Mongo **replica set**. (Note: `@Transactional` on handlers is currently a no-op.)
- [ ] Promote `@Version` to `BaseEntity` **only** once other entities get concurrent-update paths,
      their write sites handle/retry conflicts, and existing docs are migrated (backfill `version:0`).

### Correctness / known flaws (from the original review)
- [ ] **Broadcast eavesdropping:** any authenticated user can subscribe to any game's broadcast by
      guessing its `gameId` (no participant gate on the subscription). Decide spectator policy.
- [ ] **Sink lifecycle:** `GameSessionManager.removeSink()` calls `tryEmitComplete()`, which completes
      the shared broadcast for *all* subscribers (can disconnect survivors); sinks are also never
      cleaned in some paths. *Partially mitigated:* `GameSessionHandler` now only calls `removeSink`
      when `GamePresenceTracker.isGameEmpty(gameId)` (no survivors to disconnect). The general
      `removeSink` contract is still footgun-y if called elsewhere.
- [ ] **Reconnect/state resync:** the `directBestEffort` sink drops to absent subscribers and has no
      replay; a client reconnecting mid-game isn't re-synced. Fix: send a game-state snapshot on
      connect.
- [x] **Disconnect auto-LEAVE** — DONE. Transport close no longer fires `LEAVE_GAME` (event removed
      entirely). Disconnect now goes through `GamePresenceTracker` + `AbandonmentService` (see
      "Disconnect = abandonment" above); the disconnect subscribe has a real error handler. Known
      residual: forfeit timers/presence are in-memory (single-server); a restart mid-grace strands
      the game, and pre-start games left by a sole player linger in Mongo (no auto-cleanup).

### Features
- [x] **Game start** — DONE via auto-start: `JoinGameEventHandler` broadcasts `GAME_STARTED` when the
      2nd player joins. Incoming `START_GAME` event removed (was unhandled). No explicit game-status
      field was added — lifecycle state stays derived from `players` / `moves` / `isOver`.
- [x] **Manual `ABORT` event** — DONE: `AbortEventHandler` ends the game with no winner, allowed only
      before the caller's first move; `RESIGN` is gated to *after* the first move (complementary,
      mutually exclusive). Also lets a lone creator cancel an un-joined game.
- [x] **Draw detection** — DONE: `MoveEventHandler` ends a full board (`moves == boardSize²`) with no
      winner as `GameResult(DRAW)` + `GAME_DRAW`.
- [ ] Explicit `SPECTATE` presence event (currently implicit: connecting = read-only spectator).
- [ ] Move-timeout / clock rules (e.g. forfeit on running out of time). Not started.

### Testing
- [x] **`GameSessionHandler` unit tests** — DONE (`GameSessionHandlerTest`, Surefire): handshake
      gameId validation, presence/forfeit wiring, event dispatch, precondition gates (not-found /
      already-over / non-participant + their opt-outs), error containment (session survives bad
      payloads and handler crashes), optimistic-lock retry (success / exhaustion / opt-out), and
      disconnect cleanup. Uses a mocked `WebSocketSession` + scripted fake `GameEventHandler`.
- [x] **Per-handler unit tests** — DONE for all five event handlers (`MoveEventHandlerTest`,
      `JoinGameEventHandlerTest`, `ResignEventHandlerTest`, `AbortEventHandlerTest`,
      `ChatEventHandlerTest`): dispatch contracts (supports/gates/retry opt-in), every validation
      rejection (with no-side-effect assertions), win/draw/win-on-last-cell outcomes, auto-start on
      second join, abort/resign complementarity, lone-creator abort, guard-save-first ordering via
      `InOrder`, and save-failure → no side effects.
- [ ] No WebSocket integration tests yet (real socket + real handlers + Mongo). Priorities: turn
      enforcement, concurrent-join → "game full", win detection + persisted `GameResult`, chat
      fan-out to spectators.

## Guardrails when extending this layer

- Keep the **connection `gameId` authoritative**; never trust a game id from a message payload.
- **A transport disconnect is not a leave.** Never remove a player from `players` on socket close —
  membership is durable; a drop only arms the `AbandonmentService` forfeit timer. Keep presence
  (ephemeral) and membership (durable) separate.
- Any new state-mutating handler that sets `retryOnConflict()=true` **must** follow
  **guard-save-first** (version-guarded `Game.save` before side effects), or retries will double-fire.
- Keep fan-out behind `GameSessionManager` — don't let handlers talk to a transport directly.
- Non-state events (chat, presence) need no lock/retry — don't add them.
- Before proposing a distributed/actor rewrite, confirm the user wants to move past the single-server
  MVP; it was intentionally deferred.
