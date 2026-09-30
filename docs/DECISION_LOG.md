# Decision log

Decisions taken by the owner for the collider stage, recorded so they are not re-derived from memory.
Each entry states the decision, its scope, and what it does **not** authorize.

## D1 — P03/P05: scoped freeze exceptions are allowed (option A)

Decision: allow narrow, explicitly bounded freeze exceptions for the two rotated items, so the six
throughput targets can keep being pursued rather than being parked as NG.

Permitted scope — each change proposed and verified separately, one variable at a time:

- `api/geometry/Segment3d.java`: the construction path, and construction call sites that diagnostics
  prove do duplicated work.
- `api/geometry/Obb.java`: the `bounds()` computation path and its directly related repeated work.

Semantics that must not change: endpoint canonicalisation, finiteness checks, valid degenerate inputs,
immutability, value equality and hashing, the returned bounds value, and "touching counts as
intersecting". No speed-up may come from reduced precision, a narrowed legal input domain, or relaxed
bounds. Changing any observable semantics is a separate discussion, not an ordinary optimisation.

Not authorized by this entry: touching any other geometry file, changing public signatures, or adding
new geometry behaviour. The geometry package stays frozen for everything else, and
`compileGeometryIsolation` still governs the collider layer.

## D2 — P01: one operation is one full AABB-pair query

Definition: in one coordinate frame, one judgement over two valid, already-constructed AABB colliders
through the public query entry, producing and consuming one `boolean`, counts as one operation.

Consequences: shape construction stays outside the measured region; the six axis comparisons are not
six operations; a batch query is not an arbitrary multiple; multi-thread aggregate throughput does not
substitute for the single-thread target; the input set must contain fixed, verified intersecting and
separated pairs. Target unchanged: 3,561,837,865.930 ops/s.

Also recorded: the reference benchmark is unavailable, so this project defines its own operation and
**cannot** claim to have reproduced the original measurement method. The earlier "not comparable"
claim is withdrawn — the gap is a gap to investigate, not a proven definition mismatch.

## D3 — Runtime acceptance: build an isolated on-host CHB environment

Decision: plan a dedicated, isolated CHB server and client environment on the acceptance host instead
of leaving runtime validation permanently `UNKNOWN`. Scope to build: separate server and client
directories; a test world, ports and logs; Cleanroom `0.6.8-alpha` with compatible Fugue and scalar;
test artifacts built from the same candidate source with recorded hashes; a real client OpenGL
renderer check; and real-scenario checks for the entity capability, event-driven shape changes, F3+B
and entity lifecycle.

Explicitly **not** authorized: accepting an EULA, using the existing production Q-Eng server or world
as the test environment, or changing production configuration. Building the environment is not runtime
acceptance: no gate becomes `PASS` because the environment exists.

**Blocking fact:** the acceptance host is unconfirmed. The roadmap names a Ryzen 5 5950X with 63.9 GiB;
the host behind every measurement in this repository reports an AMD Ryzen 9 8945HX with 31.2 GiB. Name
the acceptance host before the isolated environment is built, and do not silently treat either machine
as the other.

## D4 — License: remains unselected

Decision: do not add or change LICENSE or SPDX headers, keep the existing third-party notices, and do
not adopt HitboxAPI's license merely because its feature set and performance targets are referenced.
Resolve licensing before public release or before actually incorporating third-party code. The
authoritative statement is the current `AGENTS.md`, not a historical commit title.
