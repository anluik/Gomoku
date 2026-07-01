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
  and sends both back. On disconnect it auto-fires `LEAVE_GAME`.
- **Connection is authoritative for `gameId`.** The `?gameId=` query param is the single source of
  truth; it was **removed from the message payload** so a client can't act on another game. One
  socket = one game (also = spectating by default).
- **Events are typed, polymorphic messages.** `GameEvent` is an abstract Jackson base keyed on
  `type`; subclasses: `JoinGameEvent`, `LeaveGameEvent`, `ResignEvent`, `StartGameEvent`,
  `MoveEvent(x,y,moveSeq)`, `ChatEvent(text)`. `SessionMessageProcessor` deserializes + bean-validates
  the right subtype automatically.
- **Handlers auto-discovered.** Each event has a `@Component` implementing `GameEventHandler` with
  opt-in default methods: `requiresActiveGame()`, `requiresParticipant()`, `retryOnConflict()`.
- **Concurrency = optimistic locking + retry.** `@Version` on `Game`; `GameSessionHandler` wraps the
  `findById → checks → handle` chain in a bounded retry (max 3) on `OptimisticLockingFailureException`
  when `handler.retryOnConflict()` is true. JOIN/MOVE/LEAVE/RESIGN opt in; CHAT/spectate do not.
- **Guard-save-first principle (mandatory for retryable handlers):** perform the version-guarded
  `Game.save` *before* any non-idempotent side effect (inserting a `GameResult`, broadcasting), so a
  retry can't double-apply. `ResignEventHandler` was reordered for this.
- **MOVE:** turn ownership (move-count parity × player order) is the primary guard; `moveSeq` dedupes
  stale/duplicate deliveries; bounds + occupancy validated; win via `GameUtils.isWinningMove`
  (4-axis line scan); on win, persist `GameResult(WIN)` and broadcast game-over.
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
      cleaned in some paths.
- [ ] **Reconnect/state resync:** the `directBestEffort` sink drops to absent subscribers and has no
      replay; a client reconnecting mid-game isn't re-synced. Fix: send a game-state snapshot on
      connect.
- [ ] **Disconnect auto-LEAVE** fires for connections that never joined and swallows errors
      (`.subscribe()` with no handler).

### Features
- [ ] `START_GAME`: the enum value exists but has **no handler** (routes to `unsupportedEvent`).
- [ ] Explicit `SPECTATE` presence event (currently implicit: connecting = read-only spectator).
- [ ] Draw detection (board full, no winner) and any move-timeout / abort rules.

### Testing
- [ ] No WebSocket integration tests yet. Priorities: turn enforcement, concurrent-join →
      "game full", win detection + persisted `GameResult`, chat fan-out to spectators.

## Guardrails when extending this layer

- Keep the **connection `gameId` authoritative**; never trust a game id from a message payload.
- Any new state-mutating handler that sets `retryOnConflict()=true` **must** follow
  **guard-save-first** (version-guarded `Game.save` before side effects), or retries will double-fire.
- Keep fan-out behind `GameSessionManager` — don't let handlers talk to a transport directly.
- Non-state events (chat, presence) need no lock/retry — don't add them.
- Before proposing a distributed/actor rewrite, confirm the user wants to move past the single-server
  MVP; it was intentionally deferred.
