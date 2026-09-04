# W108 reconstruction verification log

Date: 2026-09-05. Base commit: 4ba9a496000e9433fc1a4fefeb0e44a51fee491b.

This is the final implementation/verification record for the separately authorized W108 core prerequisite. The frozen source/test snapshot passed independent review and automated gates. It is not a Phase 2B networking completion or live-runtime acceptance record. This record was prepared before local commit from dirty main at the base above; it does not assert a clean or already-committed build input. The precise staged scope, preserved unstaged state, frozen input fingerprints and built artifact are recorded below. The owner performs local Git closeout only after this record and its audit are ready.

## Authorization and protected behavior

The user authorized continued ordinary project development and owner-decided local commits on 2026-09-05. The W108 exception covers one public Rotation3d.reconstructExact entry, its package-private helper, targeted tests and contract documentation. The old normalizer, fields, equality/hash and query arithmetic remain unchanged; the other 19 existing geometry production files remain frozen. Pre-existing topology edits and .codex/config.toml are preserved separately from the commit scope.

The original direct-normalizer re-entry failure remains a legacy negative regression with z bits 3fe8c97ef43f7248 / 3fe8c97ef43f7249 and fragment offsets 23 / 72. New-route equality/byte success is tested separately.

## Authentic development evidence

Worker command sequence used the target wrapper with Java 25.0.3+9, GRADLE_USER_HOME=C:/GradleCaches and process-local JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/GradleCaches/tmp:

| Stage | Observed result |
| --- | --- |
| Before implementation: clean compileGeometryIsolation test | Exit 1; 288 tests, only the two previously retained wire-prerequisite failures |
| First new reconstructExact test before production entry | Exit 1; authentic missing-method compile failure |
| First reconstruction green | Exit 0 |
| New wire path plus explicit legacy negative regressions and API closure | Exit 0 |
| Expanded raw/inverse/composition property tests | Exit 0 |
| First full clean gate | Exit 0; 296 tests, no failures/errors/skips |

One test-authoring compile error used nonexistent Vec3d.ZERO; it was corrected to an ordinary Vec3d construction before the green runs. It was not a production reconstruction defect.

An independent verifier reproduced the initial 296-test green snapshot and full build. Its checks found geometry 14 public types / 6 distance methods / 30 intersection overloads, unchanged Solid3d permits, Java 25 classfiles, private helper visibility, geometryIsolation -> java.base, and 19/19 unchanged non-Rotation3d geometry files. This snapshot predates review-driven test/Javadoc corrections; it is not the final gate for later edits.

Initial snapshot artifact, retained here as historical identification only:

- build/libs/crlhitbox-0.1.0-SNAPSHOT.jar, 73,466 bytes.
- SHA-256 0A5208548BA98442BCC9F534F1C1BF335AE358D4CB0A5E675F5C8BC42E5218BD.
- 44 entries, 33 class entries, zero duplicates, no tests or geometryIsolation output.

## Review findings and actions

The first independent code review found no P0/P1 implementation defect and one P2 validation gap: the initial test oracle duplicated the production integer decomposition and lower-bound search and did not independently compare reference acceptance with the public reconstruction result. The owner independently identified the same issue.

The worker replaced that oracle with a structurally separate exact-decimal reference and directed cell/pivot/parity/subnormal fixtures, adding reproducible seed/iteration context, broader strict-input tests and the complete public Javadoc failure/bound contract. The reference uses new BigDecimal(double), a raw-bit-order lower-bound binary search and exact upper-bound scan with cross-products, and independent witness verification compared against public reconstruction acceptance/raw bits. It does not use BigDecimal.valueOf or production integer decomposition.

An initial oracle attempt used exact decimal division by a general dyadic ratio and failed with ArithmeticException (focused command exit 1). It was corrected to division-free exact cross-product predicates; focused tests and full clean compileGeometryIsolation test then returned exit 0, with 297 tests / 0 failures / 0 errors / 0 skipped across 34 XML files. This is worker evidence for the corrected reference snapshot, not final independent verification of the later production optimization.

The next review closed oracle independence but identified a remaining P2 directed-coverage gap: no explicitly asserted unique y pivot or independently verified midpoint tie parity. The final corpus adds N(1,4,-2,.5) with a unique-y assertion, exact 1/2/3-candidate cells, odd/even subnormal midpoint ties, Composite/PlacedSolid3d consumer equality/hash/bounds/query checks, and imported inverse/composition/reimport closure. The owner also caught and corrected an initially tautological endpoint helper: it now derives the lower/upper endpoint from target plus its actual predecessor/successor, rather than comparing against a caller-supplied endpoint. The final production implementation has a fail-fast fourth-candidate guard and an actual 108-constructor-call guard; no test-only production entry or reflection was introduced.

The optimized implementation passed the independent numerical delta review with P0/P1/P2 = 0/0/0. Its source chain preserves the exact lower cell, common-power cancellation, lattice ceiling/carry, raw-bit encoding, bounded upper scan, tied pivots and actual-normalizer-result acceptance. The worker's optimized 300-test green run preceded the last test-only endpoint/producer-closure correction. The independent code reviewer subsequently reviewed the corrected frozen source/test hashes and closed both earlier P2 findings, returning P0/P1/P2 = 0/0/0. The final verifier matched and tested that corrected hash snapshot, as recorded below; earlier green epochs are not substituted for it.

## Conservative aggregate workload bounds

Let E be entries and O be OBB primitive leaves. Every entry contributes one transform rotation and every OBB leaf one orientation, so reconstruction calls R<=E+O. The protocol limits give E<=4096 and O<=16384, hence the count-only bound R<=20,480.

The 1 MiB payload cap gives a tighter conservative bound because each OBB leaf needs at least 81 bytes (tag plus ten doubles) and each entry transform 56 bytes:

    81*O + 56*E <= 1,048,576
    E <= 4,096
    R <= 4,096 + floor((1,048,576 - 56*4,096)/81)
      <= 14,209.

This ignores header, ResourceLocation, Composite and count overhead, so 14,209 is a relaxed upper bound, not a claimed encodable fixture. A real decoder benchmark must build a valid payload with the actual encoder. W108's 108 constructor-verification bound is per rotation, not a measured average or a latency guarantee.

## Initial reference implementation resource measurement

The owner ran an ignored, temporary standalone Java source harness at build/w108-audit/W108ResourceProbe.java, against build/classes/java/geometryIsolation. This pure-core measurement ran no packet decoder, Netty lifecycle, server or client. Process exit was 0.

Environment: Temurin 25.0.3+9-LTS; JVM -Xms128m -Xmx512m. Each profile received 2,048 warm-up calls, then three measured rounds of 14,209 calls. Allocation totals were observed using the supported ThreadMXBean per-thread allocated-bytes counter; they are cumulative allocation, not retained heap or peak memory. No performance SLA is asserted.

| Reference profile | Min ms | Median ms | Max ms | Median allocated bytes |
| --- | ---: | ---: | ---: | ---: |
| Ordinary valid constructor outputs | 1508.797 | 1553.983 | 1592.452 | 1,278,060,632 |
| Extreme/subnormal valid outputs | 1414.717 | 1416.582 | 1426.105 | 1,530,091,520 |
| Canonical off-image inputs, expected rejection | 1516.294 | 1651.169 | 1786.381 | 1,722,291,560 |

The harness verifies every successful tuple's raw bits and every expected rejection. Ordinary fixtures include the recorded counterexample, identity, quarter turn, tied maxima and mixed-sign rotations. Extreme fixtures include minimum subnormal components with nontrivial inverse length, maximum finite raw inputs and binade-adjacent values. Off-image fixtures include (1,2^-26,2^-26,0), (.5,.5,0,0), (.6,.6,.6,.6), and (.75,.25,.25,.25).

These data identify substantial allocation/comparison cost in the auditable binary-search reference enumerator. They do not establish real-message latency, all worst-case inputs, another node's performance or a runtime acceptance failure. The equivalent exact integer lower-bound optimization has now passed independent numerical and Pro review and is authorized for implementation.

## Exact optimization advisory decision

The designated in-app-browser Pro conversation completed its focused optimization review on 2026-09-05 (displayed thinking duration 10m 32s; normal reply actions and Pro selector visible). It accepted exact quotient ceiling followed by binary64 scaled-integer lattice ceiling as strictly equivalent to the reference lower-bound search, including subnormal/normal boundaries, quotient remainder zero, binade carry and the 1.0 endpoint. Optional cancellation of a common power of two is also exact. Root independently checked the proof and the implementation guards requested by the numerical reviewer: BigInteger-sized shifts, long exponent encoding, bounded range and exact lattice reconstruction.

Pro accepted the corrected exact BigDecimal oracle approach and a separately reviewed core commit. It did not run the repository or approve a packet SLA. The reference's all-off-image profile is kernel stress, not a model of one fail-fast packet: an actual decoder must stop at the first invalid rotation. Future adversarial packet measurements need expensive valid prefixes and an invalid tail as well as all-valid maximum payloads. Maximum legal layout, full ByteBuf decode, CPU/GC/peak-heap, network/main-thread costs and a preselected or independently chosen budget remain Phase 2B gates.

## Final independent automated gate and artifact

The verifier matched the frozen reconstruction source/tests and reran clean/test/build after the endpoint/imported-producer test correction. The table identifies the final snapshot, including the later wire-control strengthening whose separate full-test rerun is detailed below (the wire test at the clean epoch had SHA-256 D22AB1528C57DAC7E1299FA257F4D4162016F41A5F639E6C6ABBA220E4DDC977):

| Frozen input | SHA-256 |
| --- | --- |
| Rotation3d.java | 115618244608EF240EC546AA3E11069846C22BBE7890CA038E0CDFF5A79735B2 |
| ExactRotationReconstruction.java | 086579FEB34F13B5610398BD0F77129C8B4062A8F9EAE41D47D2333C71767A13 |
| ExactRotationReconstructionTest.java | 5FEAEC7D17106B018B02352003447B7C78CFC43E32E71A712DD3EDCDA948B369 |
| GeometryApiSurfacePhase1BTest.java | 0A01B21DF7197A17A2CEA5396F41396956B3B668E3D0FF0D65013B9CF3B9023A |
| GeometryWireRepresentationPhase2BTest.java | F78CEED42F75681D428116BBD7FDCE83478C6A7B16DEE8A981F4DA6D5790FDC7 |

Environment remained Temurin 25.0.3+9-LTS, Gradle wrapper 9.6.1, Unimined 1.4.27-kappa, Minecraft 1.12.2 / Cleanroom Loader 0.6.8-alpha, with the same process-only paths given above. Both Gradle commands completed on their initial tool calls, with no live session left to poll:

| Final command | Captured exit | Result |
| --- | ---: | --- |
| .\gradlew.bat clean compileGeometryIsolation test --stacktrace --console=plain | 0 | BUILD SUCCESSFUL in 16s; 8 tasks executed; tool wall time 17.0347s |
| .\gradlew.bat build --stacktrace --console=plain | 0 | BUILD SUCCESSFUL in 13s; 3 tasks executed / 7 up-to-date; tool wall time 14.5182s |
| jdeps -s build/classes/java/geometryIsolation | 0 | geometryIsolation -> java.base |
| javap / JAR / hash audit | 0 | API, package visibility, packaging and frozen-source checks passed |

Final JUnit XML: 34 suites, 300 tests, 0 failures, 0 errors, 0 skipped. All original 285 tests remain enabled. The public geometry inventory remains 14 types; distance methods remain 6, intersects overloads 30, Solid3d permits the same five types. Classfile major is 69. The sole new public member is reconstructExact; the helper has no public API. All 19 non-Rotation3d existing geometry source hashes match the frozen base. Rotation3d's only change is 19 inserted Javadoc/delegation lines; its prior constructor/query/equality lines are unchanged.

One final test-only strengthening followed that clean epoch: the quarter-turn control now asserts both the new exact entry and the legacy direct constructor preserve value/bytes. The wire-test hash in the table above is this final file. The verifier matched it and reran the entire test task, not just the control class: .\gradlew.bat test --stacktrace --console=plain returned exit 0 (14.4994s tool wall time, BUILD SUCCESSFUL in 13s, compileTestJava and all tests actually re-executed), again 34 suites / 300 tests / 0 failures/errors/skips. A following build returned exit 0 (7.3870s tool wall time, BUILD SUCCESSFUL in 6s, all 10 tasks up-to-date). Production inputs, isolation classes and the JAR remained unchanged from the clean epoch. This later full-test evidence, together with the unchanged production clean/build/isolation evidence, is the final tested snapshot; it is not mislabeled as another clean run.

Final primary remapped artifact:

- build/libs/crlhitbox-0.1.0-SNAPSHOT.jar, 74,454 bytes.
- SHA-256 F7BB3A1CECAF1CBA70E389CE3DDE99B0C45CCF433B923531C9B5B6905D7816EE.
- 44 JAR entries / 33 class entries / 0 duplicates; no tests or geometryIsolation output bundled.

These file hashes identify the actual development-node build input, not a deployed server/client artifact. No deployment or external-node equality is claimed.

### Pre-commit worktree and staged identity

At record preparation HEAD remained 4ba9a496000e9433fc1a4fefeb0e44a51fee491b; the W108 changes were staged but not yet committed. The staged delta excluding this self-referential verification record has SHA-256 93E38D701D132C21AB6022C5F6DF9A78CE7945C612A77E6C5552DED709CEF6B8. It is computed from `git diff --cached --binary --no-ext-diff --no-color 4ba9a496000e9433fc1a4fefeb0e44a51fee491b -- . ':(exclude)docs/W108_RECONSTRUCTION_VERIFICATION.md'`, encoded as UTF-8 without BOM with LF lines and one terminal LF. Excluding the record avoids a circular self-hash; the source/test/artifact fingerprints independently bind the verified build.

The preserved unstaged AGENTS.md topology delta has SHA-256 F9CE89D2E7A8533581454075A63CA13E48BE2A37B1A68FC68B31BFDDC873BBF9 using the same text encoding over `git diff --binary --no-ext-diff --no-color -- AGENTS.md`. It is exactly the pre-existing 35-line addition, not part of the staged W108 scope. Untracked .codex/config.toml retains SHA-256 47599E564E50DAE50B0875FB551A27012F43FC85B2A13CFA16B6A0D6632A040D. The local task packet docs/input.md remains excluded and is not a build input.

For an additional independent source binding, the owner hashed sorted `relative/path|SHA256` lines for each tree, slash-normalized, UTF-8 without BOM, LF-separated with one terminal LF. The src/main manifest is 2912BC9416BBA8178E320777264B5CCB92562DF8453516A76614D9BED88849C6; the final src/test manifest is AA46DD0ADF7EB2AE84C79E03DB75185FDD0994DA4C53D82E0F7F42C8EEB27D7C. Build configuration and dependencies remain at the recorded base.

## Optimized pure-core measurement

After the independent verifier released build outputs, the owner reran the identical retained source, JVM, heap, warmup, rounds, call count and profiles. Captured process exit: 0. Every success/rejection and raw-bit check passed. The source file SHA-256 was 5C13231A2378F933342AD8C98C1F56E41E3924BA6F250539B95296A1D3305AF8; its content is retained below for repeatability.

| Optimized profile | Min ms | Median ms | Max ms | Median allocated bytes | Median time ratio, reference / optimized | Allocation reduction |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Ordinary valid constructor outputs | 216.512 | 217.775 | 224.081 | 252,914,576 | 7.14x | 80.21% |
| Extreme/subnormal valid outputs | 164.149 | 165.615 | 235.384 | 253,296,144 | 8.55x | 83.45% |
| Canonical off-image inputs, expected rejection | 232.945 | 239.413 | 256.795 | 298,539,376 | 6.90x | 82.67% |

Ratios compare observed three-round medians, not a universal speedup guarantee. Cumulative allocation remains material and is not peak/live heap; neither a packet decoder nor network/game thread was measured. No post-hoc SLA is invented. These measurements close the requested before/after pure-core observation only.

## Git boundary and deferred Phase 2B gates

- The planned local core commit is limited to the reviewed W108-owned source, tests, contract/evidence documents and the narrow AGENTS.md authorization section, as identified by the staged snapshot above. The pre-existing 35-line topology hunk, .codex/config.toml, excluded docs/input.md, logs and build outputs stay outside that scope. Commit creation is a subsequent owner action, not an assertion made by this pre-commit record.
- The new exact geometry/ByteBuf fragment route passes; the old direct N(q) route remains a preserved negative regression. No production network codec/message, tracking sender, holder-generation state, client installer or pending-snapshot lifecycle is implemented by this change.
- Actual maximum legal payload construction, full ByteBuf decoder CPU/allocation/GC/peak-heap, network/main-thread costs and an independently selected budget remain OPEN / NOT YET EVALUABLE for Phase 2B.
- Dedicated-server, independent real-GPU client and correlated cross-node artifact/evidence gates remain unexecuted. This pure geometry core change does not require launching a client; later client/server integration cannot use it as runtime acceptance evidence.
- No push, tag, release, license change, EULA acceptance or Minecraft client/server launch was performed.


## Reproducing the pure-core measurement

The exact source used for both measurements is retained below. The Java block content excludes the opening fence's line separator and includes one terminal LF; in UTF-8 without BOM it is 4,798 bytes and has the recorded 5C13231A... source hash. Place it at the ignored build/w108-audit/W108ResourceProbe.java after the clean automated gates, and run it from the development repository using the same Java 25 executable and heap. This is a manual measurement helper, not production code, a JUnit replacement or a game launcher. Repeated measurements can vary with JVM compilation, GC and host load.

```powershell
& 'C:\GradleCaches\jdks\eclipse_adoptium-25-amd64-windows.2\bin\java.exe' -Xms128m -Xmx512m --class-path 'build/classes/java/geometryIsolation' 'build/w108-audit/W108ResourceProbe.java'
```

```java
import com.sun.management.ThreadMXBean;
import dev.crlhitbox.api.geometry.Rotation3d;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

public class W108ResourceProbe {
    private static volatile long sink;

    public static void main(String[] args) {
        var bean = ManagementFactory.getThreadMXBean();
        ThreadMXBean allocations = bean instanceof ThreadMXBean supported
                && supported.isThreadAllocatedMemorySupported() ? supported : null;
        if (allocations != null && !allocations.isThreadAllocatedMemoryEnabled()) {
            allocations.setThreadAllocatedMemoryEnabled(true);
        }
        double[][] ordinary = stored(new double[][]{
                {1,1,3,2}, {1,1,1,1}, {1,2,3,4}, {.1,-.2,.3,.9},
                {0,0,0,1}, {0,0,1,1}, {1,0,0,0}, {4,-2,1,3}});
        double[][] extreme = stored(new double[][]{
                {Double.MIN_VALUE,1,0,1}, {Double.MIN_VALUE,2*Double.MIN_VALUE,Double.MIN_NORMAL,-1},
                {Double.MAX_VALUE,1,Double.MIN_VALUE,Double.MAX_VALUE},
                {Math.nextDown(1.0),Math.nextUp(.5),Math.nextDown(.5),1},
                {0,Double.MIN_VALUE,-2*Double.MIN_VALUE,Double.MIN_NORMAL}});
        double a = Math.scalb(1.0,-26);
        double[][] nonImage = {{1,a,a,0}, {.5,.5,0,0}, {.6,.6,.6,.6}, {.75,.25,.25,.25}};
        System.out.println("java=" + System.getProperty("java.runtime.version")
                + "; count=14209 conservative byte-aware orientation ceiling; no packet decoder executed");
        run("ordinary", ordinary, false, allocations);
        run("extreme", extreme, false, allocations);
        run("off_image", nonImage, true, allocations);
        System.out.println("checksum=" + sink);
    }

    private static double[][] stored(double[][] raw) {
        double[][] result = new double[raw.length][];
        for (int i=0;i<raw.length;i++) {
            Rotation3d q = new Rotation3d(raw[i][0],raw[i][1],raw[i][2],raw[i][3]);
            result[i] = new double[]{q.x(),q.y(),q.z(),q.w()};
        }
        return result;
    }

    private static void run(String name, double[][] input, boolean reject, ThreadMXBean allocations) {
        measure(input, 2048, reject, allocations);
        long[] elapsed = new long[3];
        long[] allocated = new long[3];
        for (int round=0;round<3;round++) {
            Sample sample=measure(input,14209,reject,allocations);
            elapsed[round]=sample.nanos();
            allocated[round]=sample.allocated();
            sink ^= sample.hash();
            System.out.printf("profile=%s round=%d calls=14209 ms=%.3f allocatedBytes=%d%n",
                    name,round,sample.nanos()/1_000_000.0,sample.allocated());
        }
        Arrays.sort(elapsed);
        Arrays.sort(allocated);
        System.out.printf("SUMMARY profile=%s minMs=%.3f medianMs=%.3f maxMs=%.3f medianAllocatedBytes=%d%n",
                name,elapsed[0]/1_000_000.0,elapsed[1]/1_000_000.0,elapsed[2]/1_000_000.0,allocated[1]);
    }

    private static Sample measure(double[][] input,int count,boolean reject,ThreadMXBean allocations) {
        long thread = Thread.currentThread().threadId();
        long beforeBytes=allocations == null ? -1 : allocations.getThreadAllocatedBytes(thread);
        long before=System.nanoTime();
        long hash=0;
        int failures=0;
        for (int i=0;i<count;i++) {
            double[] q=input[i%input.length];
            try {
                Rotation3d decoded=Rotation3d.reconstructExact(q[0],q[1],q[2],q[3]);
                if (reject) throw new AssertionError("off-image profile accepted");
                if (Double.doubleToRawLongBits(decoded.x()) != Double.doubleToRawLongBits(q[0])
                        || Double.doubleToRawLongBits(decoded.y()) != Double.doubleToRawLongBits(q[1])
                        || Double.doubleToRawLongBits(decoded.z()) != Double.doubleToRawLongBits(q[2])
                        || Double.doubleToRawLongBits(decoded.w()) != Double.doubleToRawLongBits(q[3])) {
                    throw new AssertionError("bit preservation failed");
                }
                hash = Long.rotateLeft(hash,1) ^ Double.doubleToRawLongBits(decoded.z());
            } catch (IllegalArgumentException expected) {
                if (!reject) throw expected;
                failures++;
            }
        }
        long nanos=System.nanoTime()-before;
        long afterBytes=allocations == null ? -1 : allocations.getThreadAllocatedBytes(thread);
        if (reject && failures != count) throw new AssertionError("unexpected rejection count");
        return new Sample(nanos,beforeBytes < 0 || afterBytes < 0 ? -1 : afterBytes-beforeBytes,hash ^ failures);
    }

    private record Sample(long nanos,long allocated,long hash) {}
}
```

Real GPU acceptance: not executed
