# Exact Rotation Reconstruction Contract — W108

Status: REVIEWED DESIGN / IMPLEMENTED AND CORE-VERIFIED 2026-09-05. Phase 2B networking/resource/runtime gates remain separate and incomplete.

The user subsequently authorized continued normal project development and owner-decided local commits. That authorization covers the reviewed W108 core repair, its narrow source/API/decode-rule and test-contract exceptions. The design-turn restrictions and review history below describe the earlier stage. Final implementation, 300-test/build/isolation verification, independent review and pure-core measurement are recorded in [the verification log](W108_RECONSTRUCTION_VERIFICATION.md). The current scope is recorded in AGENTS.md and the W108 addendum in docs/input.md. Networking and runtime acceptance remain separate gates.

This CrlHitbox-specific design was formulated and reviewed on 2026-09-04 under a document-only authorization. The user authorized its implementation and ordinary project development on 2026-09-05 as recorded above. The earlier design-only restrictions are historical; they do not change the locked HIT-RQ consumer requirements or authorize unrelated integration. Implementation results are recorded separately from the analytic review.

## 1. Recommendation and design evolution

Recommend a single geometry-owned checked reconstruction Interface using **bounded legacy-constructor preimage verification**, designated **W108** in this document.

The success rule is deliberately stronger than a norm check:

> Return an actual result of the unchanged public Rotation3d constructor, and only when its four stored raw component bits exactly equal the supplied tuple.

The proposed algorithm enumerates every relevant inverse-rounding-cell candidate, not an empirical ULP neighborhood. Its analytic bound is at most three candidates per non-pivot coordinate, at most four pivots, and at most 108 public-constructor verifications. No additional provenance fields, wire witness, opaque byte sidecar, approximate equality, direct private-field storage or input-domain expansion is required.

The first draft examined a checked near-unit envelope importer. Independent numerical review validated its legacy-output coverage, but further source-level analysis found a concrete placed-sphere bounds counterexample admitted even by a tighter envelope. That direct-import route is **rejected as a drop-in remedy**, not merely postponed.

Independent arithmetic and whole-contract reviews accepted the W108 design after wording corrections, each with residual P0/P1/P2 counts of 0/0/0. The dedicated Pro follow-up also completed and accepted its soundness, completeness and 108-call bound under the supplied source assumptions. Root cross-checked the complete reply against the source and independent reviews. These are analytic design results, not implementation or performance evidence. The source/API/test-contract authorization was subsequently received on 2026-09-05; implementation still requires its own verification.

## 2. Current evidence and protected facts

Implementation starting point: D:/WI - Dev Workspace/CrlHitbox-src, main at 4ba9a496000e9433fc1a4fefeb0e44a51fee491b. This is the historical base, not a moving HEAD assertion.

The [historical blocker record](PHASE2B_REPRESENTATION_BLOCKER.md) and [retained prerequisite regressions](../src/test/java/dev/crlhitbox/internal/network/GeometryWireRepresentationPhase2BTest.java) establish:

- q=N(1,1,3,2) has stored z bits 3fe8c97ef43f7248.
- Direct public-constructor re-entry N(q) gives z bits 3fe8c97ef43f7249.
- The 32-byte fragment first differs at offset 23; the 137-byte OBB/placement fragment first differs at offset 72.
- Previously executed results: original 285 tests passed; adding three prerequisites produced a 288-test aggregate with two new failures and a passing quarter-turn control.
- Those tests were not changed or rerun during the original design-only turn. The later authorized implementation retains the legacy mismatch as explicitly named negative regressions and tests the new exact route separately.

Owning sources:

| Evidence | Source / meaning |
| --- | --- |
| Normalization N | [Rotation3d constructor](<D:/WI - Dev Workspace/CrlHitbox-src/src/main/java/dev/crlhitbox/api/geometry/Rotation3d.java:31>): max-component division, rounded squares/sum, sqrt, reciprocal, final rounded products |
| Canonical sign/zero | [Rotation3d helpers](<D:/WI - Dev Workspace/CrlHitbox-src/src/main/java/dev/crlhitbox/api/geometry/Rotation3d.java:136>): first nonzero in (w,x,y,z) positive; signed zero canonicalized |
| Exact equality | [Rotation3d.equals](<D:/WI - Dev Workspace/CrlHitbox-src/src/main/java/dev/crlhitbox/api/geometry/Rotation3d.java:145>): stored component bits, not angular equivalence |
| Other producers | [identity/inverse](<D:/WI - Dev Workspace/CrlHitbox-src/src/main/java/dev/crlhitbox/api/geometry/Rotation3d.java:61>); [RigidTransform3d composition](<D:/WI - Dev Workspace/CrlHitbox-src/src/main/java/dev/crlhitbox/api/geometry/RigidTransform3d.java:75>) routes products through N |
| Rotation formula | [rotateComponents](<D:/WI - Dev Workspace/CrlHitbox-src/src/main/java/dev/crlhitbox/api/geometry/Rotation3d.java:125>): v + w*t + q_xyz cross t, t=2*(q_xyz cross v) |
| Downstream representation | [Obb](<D:/WI - Dev Workspace/CrlHitbox-src/src/main/java/dev/crlhitbox/api/geometry/Obb.java:13>), [PlacedSolid3d](<D:/WI - Dev Workspace/CrlHitbox-src/src/main/java/dev/crlhitbox/api/geometry/PlacedSolid3d.java:5>), [RigidIntervalBox](<D:/WI - Dev Workspace/CrlHitbox-src/src/main/java/dev/crlhitbox/api/geometry/RigidIntervalBox.java:12>) |
| Rigid sphere placement | [PlacedSolid3d.primitiveBounds](<D:/WI - Dev Workspace/CrlHitbox-src/src/main/java/dev/crlhitbox/api/geometry/PlacedSolid3d.java:50>): transforms center, preserves radius |
| Pointwise scope | [GEOMETRY_SEMANTICS](<D:/WI - Dev Workspace/CrlHitbox-src/docs/GEOMETRY_SEMANTICS.md:3>): finite closed-set, non-certified, no global collision epsilon |
| Frozen reconstruction rule | [input.md](<D:/WI - Dev Workspace/CrlHitbox-src/docs/input.md:1039>) and its [stop clause](<D:/WI - Dev Workspace/CrlHitbox-src/docs/input.md:1079>) remain active until explicitly amended |

Source-line links in this historical evidence section refer to the pre-entry base layout; the later method insertion shifts subsequent Rotation3d line numbers. The arithmetic reasoning assumes Java SE 25 binary64 nearest/ties-to-even operations, gradual underflow and the prescribed evaluation sequence. It does not silently contract separate operations into FMA or reassociate them. These properties and correctly rounded sqrt are supported by [JLS 15.4 / 15.7.3](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.4) and [Math.sqrt](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Math.html#sqrt(double)). The proofs below are project-specific deductions, not claims that the Java documentation proves this reconstruction algorithm.

Protected starting hashes:

| File | SHA-256 |
| --- | --- |
| AGENTS.md | 17A1A8B268F67EEE420CF83F63B488210052B49FD35C48D1FB5000FFB0FEF5EF |
| .codex/config.toml | 47599E564E50DAE50B0875FB551A27012F43FC85B2A13CFA16B6A0D6632A040D |
| docs/input.md | 58854CF66C0E63D49FB55ED0E317C9D186A7B6BB5362A0C39A2EE4035C4BB31B |
| docs/PHASE2B_REPRESENTATION_BLOCKER.md | 478715C8989E9DB11DE003EDAA1212F463BA13D0CC48DD1AF7870EC776774B01 |
| GeometryWireRepresentationPhase2BTest.java | 406EEBC8981DB79159DC861C9B9115D533D7B0C91C996E8F7790F27CAADFF758 |

Existing dirty AGENTS.md and untracked .codex/config.toml are unrelated user state. The blocker record and prerequisite test predate this design turn. They are preserved.

## 3. Alternative designs and rejection reasons

### A. Checked reconstruction in the value-owning Module

One public static entry is enough. Validation, exact candidate generation and constructor verification are hidden behind that Interface. The external Netty Adapter reads/writes four doubles; it does not own normalization or enter geometry.

Two profiles were examined:

- **Envelope-only direct storage:** a necessary numerical condition is used as sufficient permission to create a value. Rejected by Section 4.
- **W108 image-witness reconstruction:** acceptance is verified by the unchanged constructor itself, with a bounded complete candidate set. This is the recommended profile.

The eventual Phase 2B decoder is a separate integration consumer. No networking production code is included in the prospective geometry-only allowlist; wiring the approved call requires separate Phase 2B scope.

### B. Globally idempotent canonicalization

If N_new preserves every output of N_old, then for the recorded q=N_old(r):

    N_new(r)=q
    N_new(q)=N_old(q)!=q.

That contradicts idempotence N_new(N_new(r))=N_new(r). Thus a global idempotent replacement must change some old constructor results. A new orientation type would likewise need real consumer migration rather than an Adapter that ultimately invokes old N(q).

This is a larger semantic change and is rejected as the minimal remedy. Repeated normalization, quantization or a near-unit raw-constructor fast path would need independent totality, bounded-work, proportional-scale and migration decisions.

### C. Opaque wire bytes plus a normalizing runtime view

Byte forwarding can remain exact while the runtime object is N(q), but existing Rotation3d/Obb/transform/placement equality still differs from the server value. This changes holder no-op/delta reasoning and creates wire/runtime dual representation. Reject it while strong exact object equality remains required.

Carrying a raw witness on the wire is another alternative, but original raw tuples are not retained today. It changes payload/provenance contracts. W108 instead reconstructs and verifies a witness locally without adding wire data.

| Concern | W108 checked reconstruction | Global idempotence | Opaque wire/runtime split |
| --- | --- | --- | --- |
| Old constructor output | Unchanged | Some must change | Unchanged |
| Accepted orientation domain | Old constructor image | Redefined | Runtime materialization differs |
| Exact server/client object equality | Preserved by verified witness | Depends on new canonical contract | Not preserved by byte retention |
| Stored four-double wire layout | Structurally unchanged | May stay, semantics change | Equivalence/provenance semantics change |
| Locality | One geometry-owned reconstruction Seam | Global producer behavior | Network state and runtime view |
| Main remaining work | Implement/prove enumerator equivalence, test and measure | Design/migration of new representation | Revise authority/equality lifecycle |
| Recommendation | Proceed to explicit implementation approval only after design review | Reject as minimal repair | Reject under retained strong goal |

This is an in-process, JDK-only pure-computation Module. No new public SPI, injected port, builder, opaque state, public error enum or serializer type is justified.

## 4. Why a near-unit envelope is insufficient

Independent numerical review accepted a conservative legacy-output bound:

    u=2^-53
    |sum_exact(q_i^2)-1| < 64u = 2^-47.

The first Pro design reply supplied a tighter 14u candidate. Neither bound proves membership in the old constructor image. More importantly, necessary norm bounds are not sufficient direct-import predicates for current rigid placement semantics.

Consider a hypothetical directly stored tuple:

    a=2^-26
    q=(1,a,a,0)
    sum_exact(q_i^2)=1+2^-51=1+4u.

It has finite components, positive zero, the existing canonical sign, max absolute component 1, and lies inside both the 14u and 64u envelopes.

For the local unit-sphere point p=(0,1,0), the current rotation formula gives:

    t=(-2a,0,2)
    rotate(p)=(2a, -1-2a^2, 2a^2)
             =(2^-25, -1-2^-51, 2^-51).

All relevant products and intermediate additions/subtractions here are exactly representable binary64 dyadics in the source evaluation order. In particular the intermediate y term is 1-2^-51, and the final y is -1-2^-51.

The current placed-sphere bounds for center zero, radius one and zero translation remain [-1,1] on each axis. The mapped local point is outside that box. Consequently a norm-only importer can admit a representation for which the stored bounds are not conservative for the placement's own represented-set definition.

This is a source-level analytic counterexample under hypothetical direct storage, **not a new runtime test**, not a claim that the old constructor emits this tuple, and not permission to change the query algorithm.

The tuple is not in the old image: an old output with max component 1 would require rounded inverseLength 1. The final products then require the scaled input to equal q. Its exact left-associated squared sum is 1+2^-51; the specified sqrt/reciprocal sequence yields a reciprocal below 1, a contradiction.

Therefore the envelope route is rejected for this proposal. The valid norm derivation can remain useful as mathematical background, but no norm tolerance is an acceptance condition of W108. No geometry-domain expansion, Gram-matrix tolerance, shape inflation or query epsilon is introduced.

## 5. Proposed W108 Interface and accepted domain

Approved Interface, added by the separately authorized implementation:

    public static Rotation3d reconstructExact(
            double x, double y, double z, double w);

Let I be the set of tuples obtainable from the unchanged legacy public constructor N. The proposed contract is:

- Accept exactly canonical supplied tuples in I.
- On success return an actual result of N whose four raw stored bits equal the supplied bits.
- Reject non-finite values, all-zero tuples, negative-zero encodings and noncanonical quaternion-sign representatives.
- Reject tuples outside the necessary max-magnitude range [1/2,1].
- For other tuples, use the complete bounded witness procedure below. Exhaustion means no legacy preimage exists under the proved algorithm, not an arbitrary search-budget failure.
- Invalid input throws IllegalArgumentException before any returned/published object.
- No partial value, fallback normalization of q, identity substitution, payload cache or ordinary-path logging is permitted.

Canonical sign means the first nonzero component in (w,x,y,z) is positive, exactly matching current shouldNegate behavior. Negative zero must be recognized from raw bits; numeric comparison with zero does not distinguish it.

The old raw constructor still accepts raw -0 and q/-q forms and canonicalizes them. Reconstruction instead rejects noncanonical encodings to retain byte identity. The two Interfaces have different input meanings; no old behavior is changed.

Success guarantees:

- Exact accessor bits, equals/hashCode and subsequent encoding.
- Immutable ordinary Rotation3d values with no extra witness/provenance fields.
- Re-import of an accepted tuple preserves its bits.
- Identity, inverse and successful composition outputs remain covered as described in Section 8.
- No claim that direct N(q) becomes idempotent.
- No unchecked/private direct-storage path: return the verified public-constructor result itself.

## 6. Bounded witness algorithm

This section specifies the algorithmic contract. Implementation and executed evidence are recorded separately in [the verification log](W108_RECONSTRUCTION_VERIFICATION.md).

### 6.1 Necessary producer facts

For finite raw input not all zero, let M=max|raw_i| and let s_i be the actual rounded divisions raw_i/M. Then |s_i|<=1 and at least one equals +1 or -1.

The rounded squared sum is in [1,4], its rounded sqrt in [1,2], and the rounded reciprocal c in [1/2,1]. Final components before canonical sign/zero handling are RN(c*s_i).

For a scaled pivot s_j=+1 or -1, multiplication is exact: |q_j|=c. Other magnitudes cannot round above c. Thus c is recoverable as max|q_i|.

If final canonicalization flipped all signs, absorb that flip into s. Zero signs can be represented by positive zero without changing the final canonical result. The resulting scaled tuple still has max magnitude one, and N(s)=q because the second scaling divides by exactly one and reproduces the same subsequent operation sequence.

### 6.2 Enumerate all possible pivots

Compute c=max|q_i|. In deterministic component order (x,y,z,w), visit every index j with |q_j|=c. There are at most four.

For that branch, fix s_j to +1 or -1 with the sign of q_j. Enumerating all equal maxima is necessary: a final-component tie does not identify which raw scaled component was the original unit pivot.

### 6.3 Nonzero-coordinate candidates

For each non-pivot coordinate with nonzero q_i, let t=|q_i|>0. Let prev(t) and next(t) be the adjacent binary64 values, and define exact real gaps:

    deltaMinus=t-prev(t)
    deltaPlus=next(t)-t.

Every magnitude b=|s_i| capable of rounding c*b to t is inside the closed outer inverse-rounding cell:

    (t+prev(t))/(2c) <= b <= (t+next(t))/(2c),
    b is a binary64 value in [0,1].

Generate **all** such representable b values, then attach the sign of q_i. The closed endpoints intentionally overinclude some tie cases; final verification by N handles ties-to-even. No endpoint may be rounded inward using ordinary floating-point midpoint arithmetic.

### 6.4 Zero-coordinate candidates

For each non-pivot coordinate with supplied positive-zero q_i, use:

    {-2^-1074, +0.0, +2^-1074}.

These are a complete safe outer set, because RN(c*s_i) can round to signed zero only when |c*s_i|<=2^-1075, and c>=1/2 gives |s_i|<=2^-1074. An exact zero-cell filter may remove impossible candidates but is not necessary.

There is no need to enumerate a separate -0 preimage: replacing its sign with +0 does not change the squared sum or final canonical stored zeros. Conversely, tiny negative preimages must not be dropped merely because the output zero is positive.

### 6.5 Verify the Cartesian candidates

For each pivot, enumerate the Cartesian product of the other three candidate sets, in deterministic order.

For each s, call the unchanged public constructor:

    actual = new Rotation3d(s_x,s_y,s_z,s_w).

Return actual only when all four Double.doubleToRawLongBits values equal the supplied tuple. Otherwise continue, then reject after exhaustion. A norm check or geometric/ULP equality must never replace this final comparison.

Every candidate tuple has a fixed unit pivot and finite components in [-1,1], so it is in the current raw constructor's valid input domain. No speculative unrepresentable quaternion needs to be constructed.

## 7. Proof obligations and analytic result

### 7.1 At most three candidates per nonzero coordinate

For positive t, deltaMinus<=deltaPlus. Since c>=1/2, the inverse-cell width is:

    (deltaMinus+deltaPlus)/(2c) <= 2*deltaPlus.

Since c<=1 and its lower midpoint is positive, the lower endpoint is at least (t+prev(t))/2, strictly greater than prev(t). Any representable candidate b must therefore be at least t.

Positive binary64 spacing on or above t never decreases; successive candidate values are separated by at least deltaPlus. A closed interval of width at most 2*deltaPlus contains at most three such values.

This reasoning covers subnormals, the smallest normal value, ordinary binades, powers of two and t=1. For t=1, nextUp(1) is used as a cell endpoint even though candidate b remains restricted to at most 1.

The zero-coordinate outer set also has at most three values.

### 7.2 Constructor-call bound

At most four pivot branches and three non-pivot coordinates yield:

    4 * 3^3 = 108

public-constructor verifications. This is a proof-derived fixed bound, not a heuristic ULP radius, retry count or normalize-until-stable loop.

### 7.3 Completeness

For every q=N(raw), the scaled tuple s established in Section 6.1 exists.

- Its original unit pivot appears among the enumerated maxima.
- Every nonzero coordinate lies in the corresponding inverse rounding cell.
- Any coordinate whose final result was zero is represented by the zero outer set; replacing a signed-zero s coordinate with +0 is harmless.
- The candidate product therefore contains that s, with any needed global canonical sign absorbed.
- Its max magnitude is one, so N(s) reproduces exactly the old post-scaling arithmetic and canonicalization.
- Final raw-bit verification accepts it.

Thus this scheme does not reject a legitimate old output merely because N(q)!=q.

### 7.4 Soundness

Every returned result was produced by actual N and compared component-bit-for-component-bit with q. Therefore it is in I. No necessary norm envelope is being mistaken for image membership, and no untrusted data is directly installed into private fields.

Together with complete enumeration, exhaustion gives rejection of tuples outside I.

### 7.5 Limits

These are analytic conclusions tied to the inspected legacy arithmetic and exact candidate-generation specification. They do not prove that a future implementation correctly realizes the enumeration, that its throughput is acceptable, or that every unrelated geometry query is mathematically certified.

Changing N's operation order or normalization contract requires re-auditing W108. The proposal does not promise that all old constructor inputs normalize idempotently, that a witness is unique, or that a particular witness is transmitted.

## 8. Exact enumeration and producer closure

### 8.1 A bounded reference enumeration plan

All finite binary64 magnitudes involved can be represented as integer multiples of 2^-1074:

    t=T*2^-1074
    c=C*2^-1074
    b=B*2^-1074.

With P and U denoting prev(t) and next(t) on the same integer scale, the cell predicates are exactly:

    (T+P)*2^1074 <= 2*C*B
    2*C*B <= (T+U)*2^1074.

Use raw IEEE bit decomposition, not decimal text conversion or rounded double midpoint/product evaluation. For a normal value with biased exponent E and significand M, its positive scaled integer is M shifted left by E-1; for a subnormal, it is the fraction integer. Zero is handled explicitly.

The positive raw-bit ordering from +0 through 1 is monotone. One reference design can use a lower-bound binary search over that fixed range, at most 64 predicate comparisons per nonzero coordinate search, to find the first representable candidate meeting the lower predicate. Then enumerate adjacent values while the upper predicate holds and b<=1. The proof bounds the collected set by three; an implementation must not silently truncate a larger set. Candidate sets depend only on q_i and c, so they may be computed once per tuple and reused across pivots: at most four such searches (256 lower-bound comparisons), plus bounded successor checks. This is local temporary work, not a persistent cache.

The t=1 upper endpoint nextUp(1) must remain representable in the integer comparison; the candidate range restriction must not incorrectly clamp the rounding cell itself.

Input and adjacent-endpoint exponents are bounded after validation. Products and comparison integers fit within a conservative 2151-bit upper bound. This is fixed-size arithmetic on four components, not allocation or iteration controlled by a peer's declared length. No square-norm computation or tolerance is required.

This reference strategy prioritizes auditability. A faster rational-division or directed-rounding enumerator may later replace it only with a proved equivalent set and independent tests. Per-tuple bounded work does not establish maximum-packet CPU/allocation safety; that remains an implementation acceptance gate.

### 8.1a Exact quotient/lattice lower bound (implementation-stage optimization review)

The first implemented binary-search reference was expensive in the measured aggregate core workload; see W108_RECONSTRUCTION_VERIFICATION.md. The following equivalent lower-bound strategy passed independent numeric review and the designated Pro's completed 2026-09-05 review. Root independently accepted the proof and authorized this optimization. It changes no W108 candidate, pivot, acceptance or constructor-call contract; implementation results still require independent verification.

Write the lower condition as `D*B >= A`, with positive integers `D=2*C` and `A=(T+P)*2^1074`. Compute `n=ceil(A/D)` by exact BigInteger quotient/remainder. For integer B and D>0, this condition is exactly `B>=n`. Validated t<=c implies `0<n<=2^1074`.

The least representable scaled coefficient at or above n is obtained as follows:

- If bitLength(n)<=53, every coefficient in that subnormal/lowest-normal-binade range is representable, so B=n.
- Otherwise let d=bitLength(n)-53 and take B=ceil(n/2^d)*2^d using BigInteger arithmetic only. This is the current binade's lattice ceiling. If the result carries into the next binade, it is an exact power of two and needs no second rounding.
- Decode the exact B to positive raw bits: zero is positive zero; B<2^52 is a subnormal fraction; otherwise e=bitLength(B)-52, M=B>>(e-1), fraction=M-2^52, bits=((long)e<<52)|fraction. The exponent must be promoted to long before the shift; an int shift would mask the distance incorrectly in Java.
- Check the internal range `0<=B<=2^1074`, exponent `1<=e<=1023` for normal values, and exact lattice reconstruction `B == M<<(e-1)` before encoding. Do not silently truncate an unexpected low-bit remainder.

Optional cancellation divides A and D by the same exact power of two, chosen with the minimum of their lowest-set-bit indices. Both operands are positive, and only a common factor may be removed. The rational value, integer quotient/remainder ceiling and candidate set are unchanged.

The result is the same first raw-bit candidate as the reference binary search. The original exact upper-bound comparison and adjacent-value enumeration remain unchanged. Directed tests must cover 2^52 and 2^53 coefficient transitions, quotient remainders, large-shift binade carry, the 2^1074 endpoint, smallest-subnormal cells and nextUp(1) as a cell endpoint. An independently represented reference oracle must compare public acceptance/results, rather than duplicating the new integer extraction/ceiling logic.

This algebraic equivalence is not a measured speedup or an implementation pass. Runtime measurements and independent implementation review remain required. No long-sized shift/mask may stand in for arbitrary BigInteger shifts, whose size can exceed 64 bits.

### 8.2 Existing and future producer coverage

| Route | Coverage argument |
| --- | --- |
| Public raw constructor | Completeness directly covers every successful N(raw) |
| identity() | (0,0,0,1) is N(0,0,0,1) |
| inverse() | Conjugating a preimage and applying existing canonical sign/zero rules yields the existing inverse's bits; no new magnitude/normalization state |
| RigidTransform3d.inverse() | Uses the existing Rotation3d inverse route |
| Successful andThen() / composition | Its quaternion product is passed through N, so returned rotation lies in I |
| W108 result | Returned actual N result lies in I; re-import is covered again |
| OBB and placed solids | Consumers of ordinary immutable rotations, not new orientation producers |

Existing finite-result/overflow failure behavior remains unchanged. Coverage of successful outputs does not add a promise that every possible transform operation succeeds.

## 9. Error, security and lifecycle contract

- Validate exactly four components; retain no buffer, Entity, World, raw witness, cache or mutable caller object.
- Reject non-finite, all-zero, negative-zero and noncanonical-sign inputs before candidate work.
- Reject necessary magnitude violations before integer decomposition; bound every shift/comparison size from validated IEEE fields.
- Enumerate exact cells with deterministic finite loops; no peer-selected precision, search window, retries or resource allocation count.
- Return only the verified public-constructor result. Failed candidates cannot be published.
- Exhaustion throws IllegalArgumentException with a useful category. No fallback to N(q), nearest representable value, identity, angular equality or an envelope-only profile.
- The later network Adapter translates invalid input to its protocol failure before message publication/holder mutation. Its thread/lifecycle obligations are separate from this pure Module.
- Successful results use existing immutable value semantics and can follow the existing immutable handoff rules.
- No logging on ordinary query/reconstruction success paths, time, randomness, ThreadLocal, platform linkage, JOML, native dependency or reflection is introduced.

For values already in I, downstream code sees exactly the same bits. This avoids a new numerical-domain admissibility claim; it does not replace regression testing or confer certified geometric authority.

## 10. Authorized W108 exceptions and version decision

The 2026-09-05 user authorization covers these reviewed W108 exceptions:

1. A narrow geometry source/API-freeze exception for one reconstruction entry and its private JDK-only exact-enumeration helpers.
2. An Interface documenting exact old-image acceptance and the fixed W108 work bound.
3. A Phase 2B decode-rule amendment: read the same stored doubles and call the approved reconstruction entry, which internally verifies a legacy-constructor witness. Exact candidate generation and verification belong to the geometry Module, not the network Adapter. This is no longer the original direct N(transmitted-components) call.
4. Retention of exact object equality, unchanged raw encoding field order, insertion order and source/wire revision separation.
5. Strict invalid/noncanonical rejection before publication and holder installation.
6. An explicit test-contract amendment that preserves the old N(q)!=q fact and separately tests the new route.

The four-double rotation layout, shape tags, transform order and other protocol limits need not change structurally. Decoder acceptance/reconstruction semantics nevertheless change and must be documented/versioned. No Phase 2B codec is implemented in this repository. Confirm any actual external distribution before deciding whether an unpublished v1 specification may be amended in place or a version change is required.

The corresponding W108 authorization addendum in input.md records those exceptions. All unrelated Phase 2B protocol, thread, limit and runtime-acceptance requirements remain unchanged.

## 11. Validation matrix and evidence boundaries

| Area | Required evidence (executed status belongs in the verification log) |
| --- | --- |
| Legacy constructor | Preserve body/operation order and old input/output expectations; original 285 tests remain enabled |
| Historical failure | Preserve (1,1,3,2), both z-bit patterns and offsets 23/72; direct N(q) remains demonstrably non-idempotent |
| New exact route | W108 returns q bits, exact object equality and identical re-encoding through Rotation3d, Obb, RigidTransform3d and PlacedSolid3d |
| Composite consumers | A PlacedSolid3d whose local solid is canonical-flat Composite; representative independent placed-solid pairs, not a placement graph |
| Candidate cells | Exact independent rational oracle; normal/subnormal cells, power-of-two boundaries, t=1 nextUp endpoint, signed-zero output cells and both tie parities |
| Pivot completeness | Every possible maximum index, tied maxima, canonical global sign flips, zero and tiny non-pivot coordinates |
| Sound rejection | Non-finite/-0/noncanonical forms, magnitude violations, and canonical near-unit non-image tuples including the analytic sphere counterexample |
| Search bound | At most three generated coordinate candidates, four pivots and 108 verifications, including adversarial non-image inputs; no silent truncation |
| Producer closure | Raw outputs across exponent/sign/tie partitions; identity, inverse, successful composition, reconstruction and repeated inverse/reconstruction |
| Reference independence | Candidate enumerator checked independently rather than using the same helper as its oracle; final N equality is soundness verification, not a substitute for testing enumeration completeness |
| Resource limits | Valid/invalid maximum-size Phase 2B workloads, whole-packet CPU/allocation budget and failure-before-publication |
| Immutability/purity | No retained mutable state/buffer/provenance; deterministic results and no normal-path effects |
| API/isolation | One approved static entry, unchanged 14 public geometry types, 6/30 queries and sealed permits; Java 25 and empty-classpath geometry isolation |
| Integration | Separately scoped Phase 2B codec/lifecycle tests, dedicated server and independent real-GPU evidence when required |

No random test campaign substitutes for the cardinality/completeness proof. Conversely, the proof does not substitute for implementation tests or a measured packet budget.

The historical direct-constructor exact-equality expectations cannot honestly be made to pass while that constructor remains unchanged. The authorized test-contract amendment replaces them only with explicitly named legacy regressions that assert and retain the N(q)!=q fact, raw inputs, bits and offsets, alongside separate exact-success assertions for W108. Those new legacy regression tests may legitimately pass under their explicitly amended expectations; they do not mean N was fixed. Do not silently replace the subject, delete/disable the fixture or weaken equality.

## 12. W108 implementation allowlist

The authorized W108 change set is limited to:

- src/main/java/dev/crlhitbox/api/geometry/Rotation3d.java: one static reconstruction entry and private helper plumbing, keeping old constructor/query/equality logic unchanged.
- A specifically listed package-private exact-enumeration helper if justified; no additional public geometry type or public unsafe entry.
- New reconstruction tests, the explicitly approved transition of GeometryWireRepresentationPhase2BTest.java, and central API-closure assertions.
- docs/GEOMETRY_SEMANTICS.md and narrowly applicable AGENTS.md constraints, preserving unrelated topology edits.
- docs/input.md only for the approved reconstruction/freeze/test-contract exceptions.
- Updates to prior blocker status after actual implementation and verification, not retroactively before them.

No change to RigidTransform3d, Obb, PlacedSolid3d, Vec3d, collision kernels, Gradle, dependencies, loader, networking, holders, rendering, persistence or gameplay production is presumed. No speculative helper should widen that list. If one becomes necessary, return for scope review.

This geometry-only allowlist excludes implementing/wiring a production packet decoder. That is later Phase 2B integration work, not implicit permission in this contract.

Local commits are authorized when the owner considers the reviewed change ready; this does not authorize release, push, CrlOzzAPI integration or Phase 2C continuation. A passing W108 core commit is not a Phase 2B networking-completion claim.

## 13. Review record and readiness

- Design explorations compared a minimal checked entry, a global idempotent canonicalizer and wire/runtime alternatives.
- Independent numerical review accepted the conservative legacy norm envelope, then separately confirmed that direct envelope admission can violate placed-sphere bounds.
- The same independent reviewer analytically accepted the W108 inverse-cell cardinality, soundness/completeness, zero/sign/tie treatment, fixed constructor bound and exact integer comparison scale. Its findings concern the specification, not an executed Implementation.
- The earlier whole-contract review identified two wording issues: separate later network wiring from the geometry-only allowlist, and avoid implying a Composite placement graph. Both are corrected here.
- A second independent numeric reading of the W108 document confirmed the proof and integer comparison plan. Its single clarification request to say non-pivot coordinates explicitly has been incorporated. Whole-contract review requested two wording clarifications: geometry-owned witness verification, and the precise distinction between a failing old equality expectation and a passing explicitly amended legacy regression. Both are incorporated. Limited re-reviews by exact_numeric_review and exact_contract_review each returned residual P0/P1/P2 = 0/0/0. These counts concern design defects, not completed implementation/resource/runtime gates.
- The first completed Pro design reply, shown as 16m 36s, recommended a conditional 14u-envelope approach and explicitly withheld geometry-safety/implementation approval. It did not know the new bounded inverse-cell proof. Root did not adopt its domain expansion; it supplied the tighter-envelope counterexample, exact sign/formula facts and W108 argument in the same [user-designated in-app-browser conversation](https://chatgpt.com/c/6a94036c-e798-83e8-b921-198beb49698b).
- That focused follow-up completed with a displayed 14m 59s thinking duration and normal reply actions after generation ended. The actual UI showed Pro. It explicitly revised the earlier recommendation: reject R-ENV as a drop-in remedy and conditionally recommend W108 for explicit authorization. It found no remaining mathematical soundness/completeness/cardinality gap under the supplied source facts; independently confirmed the sphere counterexample, tied-pivot handling, closed-cell/tie overinclusion, signed-zero/subnormal cases, exact integer scale, image membership and producer closure; and retained implementation-conformance, producer-inventory, normalization-pinning, packet-resource and user-authorization gates.
- Root confirmed the local producer inventory again: Rotation3d is final; its public raw constructor, private identity construction and private canonicalNormalized calls from inverse are the orientation-producing routes, with RigidTransform3d composition calling the public constructor. No local production reflection/Unsafe/deserialization path was found. This is a statement about supported project producers, not arbitrary external reflective mutation of an object.
- Root recommendation: adopt W108 as the reviewed design to request implementation approval for, with proposed name Rotation3d.reconstructExact. Do not approve or execute production changes solely because Pro or reviewers accepted the proof. The prior blocker record remains historical evidence for the unchanged direct N(q) route; this design establishes a new proposed route, not a retroactive green result for that route.
- No source, test, build, numerical prototype or game runtime was run/changed in this design turn. Existing red tests remain unchanged.

The preceding review bullets describe the original design-only turn. On 2026-09-05, implementation was authorized and began; they do not describe the current worktree. A further completed Pro reply (displayed thinking duration 10m 32s, Pro selector and normal reply actions visible) accepted the exact quotient/lattice optimization in Section 8.1a. It confirmed subnormal/normal transitions, lattice carry, the 1.0 endpoint, exact quotient handling, zero filtering, optional common-power cancellation and unchanged 108-call bound. Root cross-checked these conclusions with the independent numerical review rather than treating advisory text as executable authority. The advisor explicitly kept actual maximum-payload, full-decoder, thread/GC and cross-node resource gates OPEN / NOT YET EVALUABLE, and accepted a separately verified core commit without claiming Phase 2B completion.

Readiness is separated into: analytic contract review; explicit source/API/decode/test-contract authorization; implementation equivalence and regression checks; whole-packet resource validation; later server/GPU integration acceptance. A passed design review does not mean any later gate passed.

Real GPU acceptance: not executed
