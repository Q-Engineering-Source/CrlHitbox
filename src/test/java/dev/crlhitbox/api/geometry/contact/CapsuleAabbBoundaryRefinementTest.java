package dev.crlhitbox.api.geometry.contact;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import dev.crlhitbox.api.geometry.cast.CapsuleAabbCast3d;
import dev.crlhitbox.api.geometry.cast.CapsuleAabbCastResult3d;
import dev.crlhitbox.api.geometry.contact.CapsuleAabbContact3d.*;
import com.sun.management.ThreadMXBean;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class CapsuleAabbBoundaryRefinementTest {
    private static final Aabb BOX = new Aabb(new Vec3d(0,0,0),new Vec3d(1,1,1));
    private static final ContactAccuracy ACCURACY = new ContactAccuracy(1e-6,1e-6,1e-5);
    private static final QueryLimits LIMITS = new QueryLimits(8192,20000,128,32,2336,40000,64);

    @Test void initialOverlapNeverProducesInstallation() {
        Capsule c = point(0.5,0.5,0.5,0.25);
        BoundaryResult r = query(c,new Vec3d(1,0,0),candidate(c,0.5,new Vec3d(1,0,0)));
        assertEquals(BoundaryKind.INITIAL_OVERLAP,r.kind());
        assertTrue(r.installation().isEmpty());
    }

    @Test void unresolvedBoundaryDoesNotInventSafePrefixOrInstalledPose() {
        Capsule c = point(-2,0.5,0.5,0.25);
        BoundaryResult r = query(c,new Vec3d(2,0,0),candidate(c,1,new Vec3d(2,0,0)));
        assertEquals(BoundaryKind.UNRESOLVED,r.kind());
        assertTrue(r.prefix().isPresent());
        assertTrue(r.installation().isEmpty());
    }

    @Test void stationaryTouchHasNonpenetratingFullReceipt() {
        Capsule c=point(-0.25,0.5,0.5,0.25);
        BoundaryResult r=query(c,new Vec3d(0,0,0),candidate(c,1,new Vec3d(0,0,0)));
        assertEquals(BoundaryKind.NONPENETRATING_FULL,r.kind(),r::toString);
        assertEquals(PrefixKind.NONPENETRATING,r.prefix().orElseThrow().kind());
        assertEquals(Relation.TOUCHING,r.installation().orElseThrow().storedContact().relation());
        assertEquals(0,r.work().castQueries());
    }

    @Test void touchingAndMovingInwardBlocksAtStartWithoutPushout() {
        Capsule c=point(-0.25,0.5,0.5,0.25);
        Vec3d inward=new Vec3d(0.5,0,0);
        BoundaryResult r=query(c,inward,candidate(c,0,inward));
        assertEquals(BoundaryKind.BLOCKED_AT_START,r.kind(),r::toString);
        assertTrue(r.prefix().isEmpty());
        assertEquals(0,r.installation().orElseThrow().fraction());
        assertEquals(Relation.TOUCHING,r.installation().orElseThrow().storedContact().relation());
    }

    @Test void touchingOutwardAndTangentAreProvedNonpenetrating() {
        Capsule c=point(-0.25,0.5,0.5,0.25);
        for(Vec3d delta:new Vec3d[]{new Vec3d(-1,0,0),new Vec3d(0,0.1,0)}) {
            BoundaryResult r=query(c,delta,candidate(c,1,delta));
            assertEquals(BoundaryKind.NONPENETRATING_FULL,r.kind(),r::toString);
            assertEquals(PrefixKind.NONPENETRATING,r.prefix().orElseThrow().kind());
            assertTrue(r.installation().isPresent());
        }
    }

    @Test void separatedMotionEndingInExactTouchHasClosedNonpenetratingFullPath() {
        Capsule c=point(-2,0.5,0.5,0.25);
        Vec3d toward=new Vec3d(1.75,0,0);
        BoundaryResult r=query(c,toward,candidate(c,1,toward));
        assertEquals(BoundaryKind.NONPENETRATING_FULL,r.kind(),r::toString);
        assertEquals(PrefixKind.NONPENETRATING,r.prefix().orElseThrow().kind());
        assertEquals(Relation.TOUCHING,r.installation().orElseThrow().storedContact().relation());
    }

    @Test void separatedTangentPassageWithSeparatedEndpointsHasClosedNonpenetratingFullPath() {
        Capsule c=point(-2,-1,0.5,1);
        Vec3d across=new Vec3d(4,0,0);
        BoundaryResult r=query(c,across,candidate(c,1,across));
        assertEquals(BoundaryKind.NONPENETRATING_FULL,r.kind(),r::toString);
        assertEquals(PrefixKind.NONPENETRATING,r.prefix().orElseThrow().kind());
        assertEquals(1,r.prefix().orElseThrow().endFraction());
        assertEquals(Relation.SEPARATED,r.installation().orElseThrow().storedContact().relation());
        assertEquals(0,r.work().refinementSteps(),"point sweep uses finite exact segments");
    }

    @Test void zeroRadiusCrossingUsesContractualNonpenetratingPathProof() {
        Capsule c=point(-2,0.5,0.5,0);
        Vec3d across=new Vec3d(4,0,0);
        BoundaryResult r=query(c,across,candidate(c,1,across));
        assertEquals(BoundaryKind.NONPENETRATING_FULL,r.kind(),r::toString);
        assertEquals(PrefixKind.NONPENETRATING,r.prefix().orElseThrow().kind());
        assertEquals(Relation.SEPARATED,r.installation().orElseThrow().storedContact().relation());
        assertEquals(1,r.work().castQueries());
    }

    @Test void nonDyadicInteriorCornerTangencyHasClosedNonpenetratingFullPath() {
        Capsule c=point(-7.0/8,1.0/8,0.5,5.0/8);
        Vec3d acrossAndDown=new Vec3d(9.0/8,-1.5,0);
        BoundaryResult r=query(c,acrossAndDown,candidate(c,1,acrossAndDown));
        assertEquals(BoundaryKind.NONPENETRATING_FULL,r.kind(),r::toString);
        assertEquals(PrefixKind.NONPENETRATING,r.prefix().orElseThrow().kind());
        assertEquals(Relation.SEPARATED,r.installation().orElseThrow().storedContact().relation());
        assertEquals(0,r.work().refinementSteps(),"point sweep uses finite exact segments");
    }

    @Test void interiorTangencyOfPointCapsuleDoesNotNeedRefinementBudget() {
        Capsule c=point(-7.0/8,1.0/8,0.5,5.0/8);
        Vec3d acrossAndDown=new Vec3d(9.0/8,-1.5,0);
        QueryLimits oneStep=new QueryLimits(8192,20000,128,32,2336,40000,1);
        BoundaryResult r=CapsuleAabbContact3d.refineTranslationBoundary(c,acrossAndDown,BOX,
                new BoundarySearchHint(0,1),candidate(c,1,acrossAndDown),ACCURACY,oneStep);
        assertEquals(BoundaryKind.NONPENETRATING_FULL,r.kind(),r::toString);
        assertEquals(PrefixKind.NONPENETRATING,r.prefix().orElseThrow().kind());
        assertEquals(Relation.SEPARATED,r.installation().orElseThrow().storedContact().relation());
        assertEquals(0,r.work().refinementSteps());
    }

    @Test void recordingOnlyPointCapsuleTangencyAndOverlapUseFiniteExactSweep() {
        Capsule tangent=point(-7.0/8,1.0/8,0.5,5.0/8);
        Vec3d tangentDelta=new Vec3d(9.0/8,-1.5,0);
        BoundaryResult tangentResult=CapsuleAabbContact3d.refineTranslationBoundary(tangent,
                tangentDelta,BOX,new BoundarySearchHint(0,1),candidate(tangent,1,tangentDelta),
                ACCURACY,QueryLimits.recordingOnly());
        assertEquals(BoundaryKind.NONPENETRATING_FULL,tangentResult.kind(),tangentResult::toString);
        assertEquals(PrefixKind.NONPENETRATING,tangentResult.prefix().orElseThrow().kind());
        assertEquals(0,tangentResult.work().refinementSteps());
        assertTrue(tangentResult.work().staticAttempts()>0);
        assertEquals(tangentResult.work().staticCandidateUnits()+tangentResult.work().castCandidateUnits(),
                tangentResult.work().totalCandidateUnits());

        Capsule crossing=point(-2,0.5,0.5,0.25);
        Vec3d crossingDelta=new Vec3d(4,0,0);
        BoundaryResult crossingResult=CapsuleAabbContact3d.refineTranslationBoundary(crossing,
                crossingDelta,BOX,new BoundarySearchHint(0,1),candidate(crossing,1,crossingDelta),
                ACCURACY,QueryLimits.recordingOnly());
        assertEquals(BoundaryKind.UNRESOLVED,crossingResult.kind(),crossingResult::toString);
        assertEquals(Reason.NO_PREFIX_CERTIFICATE,crossingResult.reason().orElseThrow());
        assertTrue(crossingResult.installation().isEmpty());
        assertEquals(0,crossingResult.work().refinementSteps());
    }

    @Test void elongatedSweepFindsFullRankInteriorTangencyAndRejectsMicroPenetration() {
        Aabb thinBox=new Aabb(new Vec3d(-1.0/8,-1,-1.0/8),new Vec3d(0,0,0));
        Vec3d center=new Vec3d(0,0,9.0/16);
        Vec3d halfLine=new Vec3d(1.0/4,-1.0/8,0);
        Vec3d delta=new Vec3d(1.0/4,1.0/2,-5.0/8);
        Vec3d storedCenter=center.add(delta);
        double tangentRadius=3.0/8;
        double[] radii={Math.nextDown(tangentRadius),tangentRadius,Math.nextUp(tangentRadius)};
        BoundaryKind[] expected={BoundaryKind.SEPARATED_FULL,BoundaryKind.NONPENETRATING_FULL,
                BoundaryKind.UNRESOLVED};
        for(int index=0;index<radii.length;index++) {
            double radius=radii[index];
            Capsule start=capsuleAt(center,halfLine,radius);
            Capsule stored=capsuleAt(storedCenter,halfLine,radius);
            StoredInstallCandidate installation=new StoredInstallCandidate(1,center,storedCenter,
                    halfLine,stored);
            BoundaryResult result=CapsuleAabbContact3d.refineTranslationBoundary(start,delta,
                    thinBox,new BoundarySearchHint(0,1),installation,ACCURACY,
                    QueryLimits.recordingOnly());
            assertEquals(expected[index],result.kind(),result::toString);
            assertEquals(0,result.work().refinementSteps(),"finite full-capsule minimum");
            if(index<2) {
                assertEquals(Relation.SEPARATED,result.installation().orElseThrow()
                        .storedContact().relation());
                assertEquals(index==0?PrefixKind.CLEAR_SEPARATED:PrefixKind.NONPENETRATING,
                        result.prefix().orElseThrow().kind());
            } else {
                assertEquals(Reason.NO_PREFIX_CERTIFICATE,result.reason().orElseThrow());
                assertTrue(result.installation().isEmpty());
            }
        }
    }

    @Test void parallelCenterlineAndMotionStillRejectInteriorPenetration() {
        Vec3d center=new Vec3d(-7.0/4,0.5,0.5);
        Vec3d halfLine=new Vec3d(1.0/4,0,0);
        Vec3d delta=new Vec3d(4,0,0);
        Capsule start=capsuleAt(center,halfLine,0.25);
        Vec3d storedCenter=center.add(delta);
        Capsule stored=capsuleAt(storedCenter,halfLine,0.25);
        BoundaryResult result=CapsuleAabbContact3d.refineTranslationBoundary(start,delta,BOX,
                new BoundarySearchHint(0,1),new StoredInstallCandidate(1,center,storedCenter,
                        halfLine,stored),ACCURACY,QueryLimits.recordingOnly());
        assertEquals(BoundaryKind.UNRESOLVED,result.kind(),result::toString);
        assertEquals(Reason.NO_PREFIX_CERTIFICATE,result.reason().orElseThrow());
        assertTrue(result.installation().isEmpty());
        assertEquals(0,result.work().refinementSteps());
    }

    @Test void stationaryElongatedCapsuleKeepsItsExistingNoSweepPath() {
        Vec3d center=new Vec3d(-2,0.5,0.5);
        Vec3d halfLine=new Vec3d(0,0.25,0);
        Capsule start=capsuleAt(center,halfLine,0.25);
        Vec3d zero=new Vec3d(0,0,0);
        BoundaryResult result=CapsuleAabbContact3d.refineTranslationBoundary(start,zero,BOX,
                new BoundarySearchHint(0,1),new StoredInstallCandidate(1,center,center,
                        halfLine,start),ACCURACY,QueryLimits.recordingOnly());
        assertEquals(BoundaryKind.SEPARATED_FULL,result.kind(),result::toString);
        assertEquals(Relation.SEPARATED,result.installation().orElseThrow().storedContact().relation());
        assertEquals(0,result.work().refinementSteps());
        assertEquals(0,result.work().castQueries());
    }

    @Test void nonpointFaceEdgeAndCornerEndTouchUseWholePathProof() {
        assertNonpointEndTouch(new Vec3d(-2,0.5,0.5),new Vec3d(0,0.25,0),0.25,
                new Vec3d(1.75,0,0),java.util.Set.of(Face.X_MIN));
        assertNonpointEndTouch(new Vec3d(-8,-4,0.5),new Vec3d(0,0,0.25),5,
                new Vec3d(5,0,0),java.util.Set.of(Face.X_MIN,Face.Y_MIN));
        assertNonpointEndTouch(new Vec3d(-8,-4,-12.5),new Vec3d(0,0,0.5),13,
                new Vec3d(5,0,0),java.util.Set.of(Face.X_MIN,Face.Y_MIN,Face.Z_MIN));
    }

    private static void assertNonpointEndTouch(Vec3d center,Vec3d half,double radius,
                                                Vec3d delta,java.util.Set<Face> faces) {
        Capsule start=new Capsule(new Segment3d(center.subtract(half),center.add(half)),radius);
        Vec3d storedCenter=center.add(delta);
        Capsule stored=new Capsule(new Segment3d(storedCenter.subtract(half),storedCenter.add(half)),radius);
        var candidate=new StoredInstallCandidate(1,center,storedCenter,half,stored);
        BoundaryResult r=query(start,delta,candidate);
        assertEquals(BoundaryKind.NONPENETRATING_FULL,r.kind(),r::toString);
        assertEquals(Relation.TOUCHING,r.installation().orElseThrow().storedContact().relation());
        assertTrue(r.installation().orElseThrow().storedContact().witness().orElseThrow()
                .feature().possibilities().stream().anyMatch(feature->feature.activeFaces().containsAll(faces)));
    }

    @Test void thinWallFreeHitFreeIsNeverCertifiedFromEndpoints() {
        Capsule c=point(-2,0.5,0.5,0.25);
        Vec3d across=new Vec3d(4,0,0);
        BoundaryResult r=query(c,across,candidate(c,1,across));
        assertEquals(BoundaryKind.UNRESOLVED,r.kind(),r::toString);
        assertTrue(r.installation().isEmpty());
    }

    @Test void refinementLimitRetainsLastProvedPrefix() {
        Capsule c=point(-2,0.5,0.5,0.25);
        Vec3d toward=new Vec3d(3,0,0);
        QueryLimits oneStep=new QueryLimits(8192,20000,128,32,2336,40000,1);
        BoundaryResult r=CapsuleAabbContact3d.refineTranslationBoundary(c,toward,BOX,
                new BoundarySearchHint(0,1),candidate(c,0,toward),ACCURACY,oneStep);
        assertEquals(BoundaryKind.UNRESOLVED,r.kind(),r::toString);
        assertEquals(Reason.EXACT_WORK_LIMIT,r.reason().orElseThrow());
        assertEquals(0.5,r.prefix().orElseThrow().endFraction());
        assertEquals(PrefixKind.CLEAR_SEPARATED,r.prefix().orElseThrow().kind());
        assertEquals(2,r.work().refinementSteps());
    }

    @Test void staticAndCastSubbudgetsRejectWithVisibleAttempts(TestReporter reporter) {
        Capsule c=point(-2,0.5,0.5,0.25);
        Vec3d away=new Vec3d(-1,0,0);
        QueryLimits oneStatic=new QueryLimits(8192,20000,1,32,2336,40000,64);
        BoundaryResult staticLimited=CapsuleAabbContact3d.refineTranslationBoundary(c,away,BOX,
                new BoundarySearchHint(0,1),candidate(c,1,away),ACCURACY,oneStatic);
        reportBudgetWork(reporter,"static-evaluation-denied",staticLimited.work());
        assertEquals(Reason.EXACT_WORK_LIMIT,staticLimited.reason().orElseThrow());
        assertEquals(2,staticLimited.work().staticAttempts());
        assertEquals(oneStatic.maxStaticEvaluations()+1,staticLimited.work().staticEvaluations(),
                "Work includes the single denied evaluation attempt");
        assertEquals(oneStatic.maxStaticEvaluations(),
                staticLimited.work().staticEvaluations()-1,
                "completed static evaluations remain within the configured cap");
        assertEquals(staticLimited.work().staticCandidateUnits()+staticLimited.work().castCandidateUnits(),
                staticLimited.work().totalCandidateUnits());
        assertTrue(staticLimited.work().totalCandidateUnits()<=oneStatic.maxCandidateUnits());

        QueryLimits oneCastEvaluation=new QueryLimits(8192,20000,128,32,1,40000,64);
        BoundaryResult castLimited=CapsuleAabbContact3d.refineTranslationBoundary(c,away,BOX,
                new BoundarySearchHint(0,1),candidate(c,1,away),ACCURACY,oneCastEvaluation);
        reportBudgetWork(reporter,"cast-evaluation-reservation-denied",castLimited.work());
        assertEquals(Reason.EXACT_WORK_LIMIT,castLimited.reason().orElseThrow());
        assertEquals(1,castLimited.work().castAttempts());
        assertEquals(0,castLimited.work().castQueries());
        assertEquals(0,castLimited.work().castEvaluations());
        assertEquals(0,castLimited.work().castCandidateUnits());
        assertTrue(castLimited.work().castEvaluations()<=oneCastEvaluation.maxCastEvaluations());
        assertEquals(castLimited.work().staticCandidateUnits()+castLimited.work().castCandidateUnits(),
                castLimited.work().totalCandidateUnits());
        assertTrue(castLimited.work().totalCandidateUnits()<=oneCastEvaluation.maxCandidateUnits());

        QueryLimits oneExactOperation=new QueryLimits(8192,1,128,32,2336,40000,64);
        BoundaryResult exactLimited=CapsuleAabbContact3d.refineTranslationBoundary(c,away,BOX,
                new BoundarySearchHint(0,1),candidate(c,1,away),ACCURACY,oneExactOperation);
        reportBudgetWork(reporter,"exact-operation-denied",exactLimited.work());
        assertEquals(BoundaryKind.UNRESOLVED,exactLimited.kind(),exactLimited::toString);
        assertEquals(Reason.EXACT_WORK_LIMIT,exactLimited.reason().orElseThrow());
        assertTrue(exactLimited.work().exactAttempts()>0,"the denied exact operation attempt is observable");
        assertTrue(exactLimited.work().exactOps()<=oneExactOperation.maxExactOps());
        assertTrue(exactLimited.work().staticEvaluations()<=oneExactOperation.maxStaticEvaluations());
        assertEquals(exactLimited.work().staticCandidateUnits()+exactLimited.work().castCandidateUnits(),
                exactLimited.work().totalCandidateUnits());
        assertTrue(exactLimited.work().totalCandidateUnits()<=oneExactOperation.maxCandidateUnits());
    }

    @Test void legacyCastEvaluationCountMatchesBoundaryReservationAccounting(TestReporter reporter) {
        Capsule c=point(-2,0.5,0.5,0.25);
        Vec3d away=new Vec3d(-1,0,0);
        CapsuleAabbCastResult3d oldCast=CapsuleAabbCast3d.cast(c,away,BOX);
        reporter.publishEntry("UG_CONTACT_BUDGET_OLD_CAST",
                "kind="+oldCast.kind()+" evaluationCount="+oldCast.work().evaluationCount()
                        +" advancementAttempts="+oldCast.work().advancementAttempts()
                        +" refinementAttempts="+oldCast.work().refinementAttempts());
        System.out.println("UG_CONTACT_BUDGET_OLD_CAST kind="+oldCast.kind()
                +" work="+oldCast.work());

        BoundaryResult boundary=CapsuleAabbContact3d.refineTranslationBoundary(c,away,BOX,
                new BoundarySearchHint(0,1),candidate(c,1,away),ACCURACY,LIMITS);
        Work work=boundary.work();
        reportBudgetWork(reporter,"successful-boundary",work);

        assertEquals(CapsuleAabbCastResult3d.Kind.CLEAR_SEPARATED,oldCast.kind(),oldCast::toString);
        assertTrue(oldCast.work().evaluationCount()>0,oldCast::toString);
        assertEquals(BoundaryKind.SEPARATED_FULL,boundary.kind(),boundary::toString);
        assertEquals(1,work.castAttempts());
        assertEquals(1,work.castQueries());
        assertEquals(oldCast.work().evaluationCount(),work.castEvaluations());
        assertEquals(Math.multiplyExact(15,oldCast.work().evaluationCount()),work.castCandidateUnits());
        assertEquals(work.staticCandidateUnits()+work.castCandidateUnits(),work.totalCandidateUnits());
        assertTrue(work.totalCandidateUnits()<=LIMITS.maxCandidateUnits());
        assertCommonLimits("successful boundary with cast",work);
    }

    @Test void candidateBudgetRequiresStaticUnitsPlusWorstCaseCastReservation(TestReporter reporter) {
        Capsule c=point(-2,0.5,0.5,0.25);
        Vec3d away=new Vec3d(-1,0,0);
        QueryLimits belowStaticPlusCastReservation=new QueryLimits(8192,20000,128,32,2336,1095,64);
        BoundaryResult limited=CapsuleAabbContact3d.refineTranslationBoundary(c,away,BOX,
                new BoundarySearchHint(0,1),candidate(c,1,away),ACCURACY,
                belowStaticPlusCastReservation);
        Work work=limited.work();
        reportBudgetWork(reporter,"candidate-reservation-denied",work);

        assertEquals(BoundaryKind.UNRESOLVED,limited.kind(),limited::toString);
        assertEquals(Reason.EXACT_WORK_LIMIT,limited.reason().orElseThrow());
        assertTrue(work.staticCandidateUnits()>0,limited::toString);
        assertTrue(belowStaticPlusCastReservation.maxCandidateUnits()
                <1095+work.staticCandidateUnits(),limited::toString);
        assertEquals(1,work.castAttempts());
        assertEquals(0,work.castQueries(),"reservation failure must not invoke the legacy cast");
        assertEquals(0,work.castEvaluations());
        assertEquals(0,work.castCandidateUnits());
        assertEquals(work.staticCandidateUnits(),work.totalCandidateUnits());
        assertTrue(work.totalCandidateUnits()<=belowStaticPlusCastReservation.maxCandidateUnits());
        assertTrue(work.exactOps()<=belowStaticPlusCastReservation.maxExactOps());
        assertTrue(work.staticEvaluations()<=belowStaticPlusCastReservation.maxStaticEvaluations());
    }

    @Test void movingAwayCanUseCastFullPrefixWhenStoredPoseIsExact() {
        Capsule c=point(-2,0.5,0.5,0.25);
        Vec3d away=new Vec3d(-1,0,0);
        BoundaryResult r=query(c,away,candidate(c,1,away));
        assertEquals(BoundaryKind.SEPARATED_FULL,r.kind(),r::toString);
        assertEquals(1,r.prefix().orElseThrow().endFraction());
        assertEquals(1,r.work().castQueries());
        assertEquals(Relation.SEPARATED,r.installation().orElseThrow().storedContact().relation());
    }

    @Test void unrelatedStoredPoseCannotBeInstalled() {
        Capsule c=point(-2,0.5,0.5,0.25);
        StoredInstallCandidate falsePose=new StoredInstallCandidate(1,c.centerline().start(),
                new Vec3d(100,0.5,0.5),new Vec3d(0,0,0),point(100,0.5,0.5,0.25));
        BoundaryResult r=query(c,new Vec3d(-1,0,0),falsePose);
        assertEquals(BoundaryKind.UNRESOLVED,r.kind());
        assertEquals(Reason.ROUNDED_INSTALL_UNSAFE,r.reason().orElseThrow());
        assertTrue(r.installation().isEmpty());
    }

    @Test void outOfProfileStoredPoseIsUnsupportedDomain() {
        Capsule c=point(-2,0.5,0.5,0.25);
        Vec3d distant=new Vec3d(0x1.0p26,0.5,0.5);
        StoredInstallCandidate candidate=new StoredInstallCandidate(1,c.centerline().start(),
                distant,new Vec3d(0,0,0),point(distant.x(),distant.y(),distant.z(),0.25));
        BoundaryResult r=query(c,new Vec3d(-1,0,0),candidate);
        assertEquals(BoundaryKind.UNRESOLVED,r.kind());
        assertEquals(Reason.UNSUPPORTED_DOMAIN,r.reason().orElseThrow());
    }

    @Test void inwardFaceMotionBracketsFirstStrictOverlapAndKeepsStartPoseSafe() {
        Capsule c=point(-2,0.5,0.5,0.25);
        Vec3d inward=new Vec3d(2,0,0);
        BoundaryResult r=query(c,inward,candidate(c,0,inward));
        assertEquals(BoundaryKind.CERTIFIED_BOUNDARY,r.kind(),r::toString);
        Interval bracket=r.firstObstructionBracket().orElseThrow();
        assertTrue(bracket.lower()<=0.875 && bracket.upper()>0.875,r::toString);
        assertTrue(2*(bracket.upper()-bracket.lower())<=ACCURACY.maxPathWidthMeters());
        assertEquals(Relation.SEPARATED,r.installation().orElseThrow().storedContact().relation());
        assertTrue(r.work().refinementSteps()>0);
    }

    @Test void certifiedBoundaryInstallsCanonicalPositiveFractionBeforeFirstOverlap() {
        Capsule c=point(-2,0.5,0.5,0.25);
        Vec3d inward=new Vec3d(2,0,0);
        double safeFraction=0.87499;
        StoredInstallCandidate canonical=candidate(c,safeFraction,inward);
        BoundaryResult r=query(c,inward,canonical);

        assertEquals(BoundaryKind.CERTIFIED_BOUNDARY,r.kind(),r::toString);
        assertEquals(Relation.SEPARATED,r.startRelation());
        PrefixReceipt prefix=r.prefix().orElseThrow();
        assertEquals(PrefixKind.NONPENETRATING,prefix.kind());
        assertTrue(safeFraction>0 && prefix.endFraction()>=safeFraction,r::toString);

        Interval bracket=r.firstObstructionBracket().orElseThrow();
        assertTrue(bracket.lower()<=0.875 && bracket.upper()>0.875,r::toString);
        assertTrue(2*(bracket.upper()-bracket.lower())<=ACCURACY.maxPathWidthMeters(),r::toString);

        InstalledPoseReceipt installed=r.installation().orElseThrow();
        assertEquals(safeFraction,installed.fraction());
        assertEquals(canonical.acceptedStoredCenter(),installed.storedCenter());
        assertEquals(canonical.acceptedHalfLine(),installed.halfLine());
        assertSame(canonical.actualStoredCapsule(),installed.storedCapsule());
        ContactResult stored=installed.storedContact();
        assertEquals(Relation.SEPARATED,stored.relation(),stored::toString);
        assertTrue(stored.signedClearanceMeters().orElseThrow().lower()>0,stored::toString);
    }

    @Test void ugVerticalCapsuleFloorInwardBoundaryFitsCommonLimits(TestReporter reporter) {
        verifyUgInwardFixture(new InwardFixture("floor",
                new Vec3d(0.5,3,0.5),new Vec3d(0,0.5,0),0.25,
                new Vec3d(0,-2,0),0.25,0.625,PrefixKind.NONPENETRATING,
                Set.of(Face.Y_MAX)),reporter);
    }

    @Test void ugVerticalCapsuleWallInwardBoundaryFitsCommonLimits(TestReporter reporter) {
        verifyUgInwardFixture(new InwardFixture("wall",
                new Vec3d(-2,0.5,0.5),new Vec3d(0,0.25,0),0.25,
                new Vec3d(2,0,0),0.5,0.875,PrefixKind.NONPENETRATING,
                Set.of(Face.X_MIN)),reporter);
    }

    @Test void ugVerticalCapsuleEdgeInwardBoundaryFitsCommonLimits(TestReporter reporter) {
        verifyUgInwardFixture(new InwardFixture("edge",
                new Vec3d(-2,2,0.5),new Vec3d(0,0.5,0),0.25,
                new Vec3d(2,-0.5,0),0.5,1-1/(2*Math.sqrt(17)),PrefixKind.CLEAR_SEPARATED,
                Set.of(Face.X_MIN,Face.Y_MAX)),reporter);
    }

    @Test void ugVerticalCapsuleCornerInwardBoundaryFitsCommonLimits(TestReporter reporter) {
        verifyUgInwardFixture(new InwardFixture("corner",
                new Vec3d(-2,2,-2),new Vec3d(0,0.5,0),0.25,
                new Vec3d(2,-0.5,2),0.5,1-1/(2*Math.sqrt(33)),PrefixKind.CLEAR_SEPARATED,
                Set.of(Face.X_MIN,Face.Y_MAX,Face.Z_MIN)),reporter);
    }

    @Test void nearbyThirtyMillionCoordinateRoundingUsesBoundedBridge() {
        Vec3d center=new Vec3d(30_000_000.1,0.5,0.5);
        Vec3d halfLine=new Vec3d(0,0.3,0);
        Capsule start=new Capsule(new Segment3d(center.subtract(halfLine),center.add(halfLine)),0.25);
        Vec3d delta=new Vec3d(0.1,0,0);
        Vec3d storedCenter=center.add(delta);
        assertNotEquals(0,new java.math.BigDecimal(center.x()).add(new java.math.BigDecimal(delta.x()))
                .compareTo(new java.math.BigDecimal(storedCenter.x())),
                "fixture must exercise a nonzero binary64 installation correction");
        Capsule stored=new Capsule(new Segment3d(storedCenter.subtract(halfLine),
                storedCenter.add(halfLine)),0.25);
        StoredInstallCandidate candidate=new StoredInstallCandidate(1,center,storedCenter,halfLine,stored);
        Aabb remoteBox=new Aabb(new Vec3d(30_000_010,0,0),new Vec3d(30_000_011,1,1));
        BoundaryResult r=CapsuleAabbContact3d.refineTranslationBoundary(start,delta,remoteBox,
                new BoundarySearchHint(0,1),candidate,ACCURACY,LIMITS);
        assertEquals(BoundaryKind.SEPARATED_FULL,r.kind(),r::toString);
        assertEquals(stored,r.installation().orElseThrow().storedCapsule());
    }

    @Test void nearContactThirtyMillionCorrectionKeepsPositiveStoredGap() {
        double wall=30_000_000.0;
        Vec3d center=new Vec3d(wall-1,0.5,0.5),half=new Vec3d(0,0.3,0);
        Vec3d delta=new Vec3d(0.74999,0,0);
        Capsule start=new Capsule(new Segment3d(center.subtract(half),center.add(half)),0.25);
        Vec3d storedCenter=center.add(delta);
        Capsule stored=new Capsule(new Segment3d(storedCenter.subtract(half),
                storedCenter.add(half)),0.25);
        Aabb wallBox=new Aabb(new Vec3d(wall,0,0),new Vec3d(wall+1,1,1));
        BoundaryResult r=CapsuleAabbContact3d.refineTranslationBoundary(start,delta,wallBox,
                new BoundarySearchHint(0,1),new StoredInstallCandidate(1,center,storedCenter,half,stored),
                ACCURACY,LIMITS);
        assertEquals(BoundaryKind.SEPARATED_FULL,r.kind(),r::toString);
        assertEquals(Relation.SEPARATED,r.installation().orElseThrow().storedContact().relation());
        assertTrue(r.installation().orElseThrow().storedContact().signedClearanceMeters()
                .orElseThrow().lower()>0);
    }

    @Test void storedTouchBridgeDoesNotUpgradeStrictMovementPrefix(TestReporter reporter) {
        double wall=30_000_000.0, fraction=0.5;
        Aabb remoteBox=new Aabb(new Vec3d(wall,0,0),new Vec3d(wall+1,1,1));
        Vec3d center=new Vec3d(29_999_999.7,0.5,0.5),delta=new Vec3d(0.1,0,0);
        Capsule start=point(center.x(),center.y(),center.z(),0.25);
        Vec3d storedCenter=center.add(delta.multiply(fraction));
        Capsule stored=point(storedCenter.x(),storedCenter.y(),storedCenter.z(),0.25);
        StoredInstallCandidate candidate=new StoredInstallCandidate(fraction,center,storedCenter,
                new Vec3d(0,0,0),stored);

        java.math.BigDecimal exactIdealX=new java.math.BigDecimal(center.x()).add(
                new java.math.BigDecimal(delta.x()).multiply(new java.math.BigDecimal(fraction)));
        java.math.BigDecimal expandedFaceX=new java.math.BigDecimal(wall)
                .subtract(new java.math.BigDecimal(0.25));
        ContactResult startContact=CapsuleAabbContact3d.query(start,remoteBox,ACCURACY,LIMITS);
        ContactResult storedContact=CapsuleAabbContact3d.query(stored,remoteBox,ACCURACY,LIMITS);
        BoundaryResult result=CapsuleAabbContact3d.refineTranslationBoundary(start,delta,remoteBox,
                new BoundarySearchHint(0,1),candidate,ACCURACY,LIMITS);
        reporter.publishEntry("movement prefix and correction bridge",
                "exactIdealGap="+expandedFaceX.subtract(exactIdealX)
                        +" storedCenter="+storedCenter+" start="+startContact
                        +" stored="+storedContact+" boundary="+result);

        assertTrue(exactIdealX.compareTo(expandedFaceX)<0,
                "the exact translated pose at the accepted fraction is still strictly outside the expanded face");
        assertEquals(expandedFaceX.doubleValue(),storedCenter.x(),
                "binary64 stored pose rounds onto the closed contact face");
        assertEquals(Relation.SEPARATED,startContact.relation(),startContact::toString);
        assertEquals(Relation.TOUCHING,storedContact.relation(),storedContact::toString);
        assertEquals(BoundaryKind.CERTIFIED_BOUNDARY,result.kind(),result::toString);
        PrefixReceipt prefix=result.prefix().orElseThrow();
        assertEquals(PrefixKind.CLEAR_SEPARATED,prefix.kind(),result::toString);
        assertEquals(fraction,prefix.endFraction());
        Interval bracket=result.firstObstructionBracket().orElseThrow();
        assertEquals(fraction,bracket.lower());
        assertTrue(bracket.upper()>fraction && Math.nextUp(0.1)*(bracket.upper()-bracket.lower())
                <=ACCURACY.maxPathWidthMeters(),result::toString);
        InstalledPoseReceipt installed=result.installation().orElseThrow();
        assertEquals(fraction,installed.fraction());
        assertSame(stored,installed.storedCapsule());
        assertEquals(Relation.TOUCHING,installed.storedContact().relation());
        assertCommonLimits("strict prefix with stored touch bridge",result.work());
    }

    @Test void originalTranslationPrefixUsesExactParameterRatherThanRoundedSubcast(TestReporter reporter) {
        Aabb box=new Aabb(new Vec3d(7,66,8),new Vec3d(8,67,9));
        Vec3d center=new Vec3d(8.300000035762787,66.89999997615814,8.9);
        Vec3d originalDelta=new Vec3d(-0.24999999999999994,-5.551115123125783E-17,0);
        double radius=(double)0.3f;
        double rejected=9.53674330617105E-8, safe=Math.nextDown(rejected),
                later=Math.nextUp(rejected);
        Capsule start=point(center.x(),center.y(),center.z(),radius);
        ContactResult startContact=CapsuleAabbContact3d.query(start,box,ACCURACY,LIMITS);
        assertEquals(Relation.SEPARATED,startContact.relation());

        BigDecimal threshold=new BigDecimal(box.max().x()).add(new BigDecimal(radius));
        assertTrue(exactOriginalMargin(center.x(),originalDelta.x(),safe,threshold).signum()>0);
        assertTrue(exactOriginalMargin(center.x(),originalDelta.x(),rejected,threshold).signum()<0);
        assertTrue(exactOriginalMargin(center.x(),originalDelta.x(),later,threshold).signum()<0);
        assertTrue(new BigDecimal(safe).multiply(new BigDecimal(335544315))
                .compareTo(new BigDecimal(32))<0,
                "the exact contact parameter 32/335544315 is above nextDown(t)");
        assertTrue(new BigDecimal(rejected).multiply(new BigDecimal(335544315))
                .compareTo(new BigDecimal(32))>0,
                "the original proposed t is beyond exact contact");

        Vec3d sharedStoredCenter=center.add(originalDelta.multiply(safe));
        assertEquals(new Vec3d(8.300000011920929,center.y(),center.z()),sharedStoredCenter);
        assertEquals(sharedStoredCenter,center.add(originalDelta.multiply(rejected)));
        assertEquals(sharedStoredCenter,center.add(originalDelta.multiply(later)));
        Capsule stored=point(sharedStoredCenter.x(),sharedStoredCenter.y(),sharedStoredCenter.z(),radius);
        ContactResult storedContact=CapsuleAabbContact3d.query(stored,box,ACCURACY,LIMITS);
        assertEquals(Relation.TOUCHING,storedContact.relation());
        assertTrue(storedContact.witness().isPresent());

        for(double t:new double[]{rejected,later}) {
            StoredInstallCandidate candidate=new StoredInstallCandidate(t,center,sharedStoredCenter,
                    new Vec3d(0,0,0),stored);
            OriginalPrefixResult result=CapsuleAabbContact3d.proveOriginalTranslationPrefix(
                    start,originalDelta,t,box,candidate,ACCURACY,LIMITS);
            reporter.publishEntry("unsafe original t="+t,result.toString());
            assertTrue(result.proof().isEmpty(),result::toString);
            assertEquals(Reason.NO_PREFIX_CERTIFICATE,result.reason().orElseThrow(),result::toString);
        }

        StoredInstallCandidate candidate=new StoredInstallCandidate(safe,center,sharedStoredCenter,
                new Vec3d(0,0,0),stored);
        OriginalPrefixResult result=CapsuleAabbContact3d.proveOriginalTranslationPrefix(
                start,originalDelta,safe,box,candidate,ACCURACY,LIMITS);
        reporter.publishEntry("safe original nextDown(t)",result.toString());
        assertTrue(result.reason().isEmpty(),result::toString);
        OriginalPrefixReceipt proof=result.proof().orElseThrow();
        assertSame(start,proof.startCapsule());
        assertSame(originalDelta,proof.originalDelta());
        assertSame(box,proof.obstacle());
        assertSame(candidate,proof.candidate());
        assertEquals(safe,proof.endFraction());
        assertEquals(safe,proof.prefix().endFraction());
        assertEquals(PrefixKind.NONPENETRATING,proof.prefix().kind());
        assertEquals(safe,proof.installation().fraction());
        assertSame(stored,proof.installation().storedCapsule());
        assertEquals(Relation.TOUCHING,proof.installation().storedContact().relation());
        assertTrue(proof.installation().storedContact().witness().isPresent());
        assertCommonLimits("original prefix nextDown",result.work());
    }

    @Test void originalTranslationPrefixRejectsIdentityDomainAndBudgetFailures() {
        Aabb box=new Aabb(new Vec3d(7,66,8),new Vec3d(8,67,9));
        Vec3d center=new Vec3d(8.300000035762787,66.89999997615814,8.9);
        Vec3d delta=new Vec3d(-0.24999999999999994,-5.551115123125783E-17,0);
        double t=Math.nextDown(9.53674330617105E-8),radius=(double)0.3f;
        Capsule start=point(center.x(),center.y(),center.z(),radius);
        Vec3d actual=center.add(delta.multiply(t));
        Capsule stored=point(actual.x(),actual.y(),actual.z(),radius);
        StoredInstallCandidate candidate=new StoredInstallCandidate(t,center,actual,new Vec3d(0,0,0),stored);

        assertEquals(Reason.UNSUPPORTED_DOMAIN,CapsuleAabbContact3d.proveOriginalTranslationPrefix(
                start,delta,Math.nextUp(1.0),box,candidate,ACCURACY,LIMITS).reason().orElseThrow());
        assertEquals(Reason.ROUNDED_INSTALL_UNSAFE,CapsuleAabbContact3d.proveOriginalTranslationPrefix(
                start,delta,Math.nextUp(t),box,candidate,ACCURACY,LIMITS).reason().orElseThrow());
        StoredInstallCandidate wrongCenter=new StoredInstallCandidate(t,center,
                new Vec3d(Math.nextUp(actual.x()),actual.y(),actual.z()),new Vec3d(0,0,0),stored);
        assertEquals(Reason.ROUNDED_INSTALL_UNSAFE,CapsuleAabbContact3d.proveOriginalTranslationPrefix(
                start,delta,t,box,wrongCenter,ACCURACY,LIMITS).reason().orElseThrow());
        Capsule wrongRadius=point(actual.x(),actual.y(),actual.z(),Math.nextUp(radius));
        StoredInstallCandidate changedRadius=new StoredInstallCandidate(t,center,actual,
                new Vec3d(0,0,0),wrongRadius);
        assertEquals(Reason.ROUNDED_INSTALL_UNSAFE,CapsuleAabbContact3d.proveOriginalTranslationPrefix(
                start,delta,t,box,changedRadius,ACCURACY,LIMITS).reason().orElseThrow());
        QueryLimits tiny=new QueryLimits(8192,100,128,32,2336,40000,64);
        assertEquals(Reason.EXACT_WORK_LIMIT,CapsuleAabbContact3d.proveOriginalTranslationPrefix(
                start,delta,t,box,candidate,ACCURACY,tiny).reason().orElseThrow());
        Capsule line=new Capsule(new Segment3d(center,new Vec3d(center.x(),center.y()+0.5,center.z())),radius);
        assertEquals(Reason.UNSUPPORTED_DOMAIN,CapsuleAabbContact3d.proveOriginalTranslationPrefix(
                line,delta,t,box,candidate,ACCURACY,LIMITS).reason().orElseThrow());
    }

    @Test void originalTranslationPrefixSameFaceProofCoversAllSixFaces() {
        for(int axis=0;axis<3;axis++) for(boolean maximum:new boolean[]{false,true}) {
            double[] center={0.5,0.5,0.5},delta={0,0,0};
            center[axis]=maximum?1.5:-0.5;
            delta[axis]=maximum?-0.25:0.25;
            Vec3d startCenter=new Vec3d(center[0],center[1],center[2]);
            Vec3d originalDelta=new Vec3d(delta[0],delta[1],delta[2]);
            Vec3d storedCenter=startCenter.add(originalDelta);
            Capsule start=point(startCenter.x(),startCenter.y(),startCenter.z(),0.25);
            Capsule stored=point(storedCenter.x(),storedCenter.y(),storedCenter.z(),0.25);
            StoredInstallCandidate candidate=new StoredInstallCandidate(1,startCenter,storedCenter,
                    new Vec3d(0,0,0),stored);
            OriginalPrefixResult result=CapsuleAabbContact3d.proveOriginalTranslationPrefix(
                    start,originalDelta,1,BOX,candidate,ACCURACY,LIMITS);
            String face=new String[]{"X","Y","Z"}[axis]+(maximum?"_MAX":"_MIN");
            assertTrue(result.proof().isPresent(),face+": "+result);
            OriginalPrefixReceipt proof=result.proof().orElseThrow();
            assertEquals(1,proof.prefix().endFraction(),face);
            assertEquals(Relation.TOUCHING,proof.installation().storedContact().relation(),face);
            assertTrue(proof.installation().storedContact().witness().isPresent(),face);
            assertCommonLimits("original prefix "+face,result.work());
        }
    }

    @Test void elongatedOriginalCapsulePrefixProvesAxialExactTouchAtActualFraction(TestReporter reporter) {
        Vec3d center=new Vec3d(-1,0.5,0.5),halfLine=new Vec3d(0,0.5,0);
        Vec3d delta=new Vec3d(1.5,0,0);
        double t=0.5;
        Capsule start=capsuleAt(center,halfLine,0.25);
        Vec3d storedCenter=center.add(delta.multiply(t));
        Capsule stored=capsuleAt(storedCenter,halfLine,0.25);
        StoredInstallCandidate candidate=new StoredInstallCandidate(t,center,storedCenter,halfLine,stored);
        ContactResult startContact=CapsuleAabbContact3d.query(start,BOX,ACCURACY,LIMITS);
        ContactResult storedContact=CapsuleAabbContact3d.query(stored,BOX,ACCURACY,LIMITS);

        ElongatedOriginalPrefixResult result=CapsuleAabbContact3d.proveOriginalCapsuleTranslationPrefix(
                start,delta,t,BOX,candidate,ACCURACY,LIMITS);
        reporter.publishEntry("elongated axial exact-touch prefix",result.toString());

        assertEquals(Relation.SEPARATED,startContact.relation(),startContact::toString);
        assertTrue(startContact.signedClearanceMeters().orElseThrow().lower()>0,startContact::toString);
        assertEquals(new Vec3d(-0.25,0.5,0.5),storedCenter);
        assertEquals(Relation.TOUCHING,storedContact.relation(),storedContact::toString);
        assertTrue(storedContact.witness().isPresent(),storedContact::toString);
        assertTrue(result.reason().isEmpty(),result::toString);
        ElongatedOriginalPrefixReceipt proof=result.proof().orElseThrow();
        assertSame(start,proof.startCapsule());
        assertSame(delta,proof.originalDelta());
        assertSame(BOX,proof.obstacle());
        assertSame(candidate,proof.candidate());
        assertSame(ACCURACY,proof.accuracy());
        assertSame(LIMITS,proof.limits());
        assertEquals(t,proof.endFraction());
        assertEquals(t,proof.prefix().endFraction());
        assertEquals(PrefixKind.NONPENETRATING,proof.prefix().kind());
        assertEquals(t,proof.installation().fraction());
        assertSame(stored,proof.installation().storedCapsule());
        assertSame(storedCenter,proof.installation().storedCenter());
        assertSame(halfLine,proof.installation().halfLine());
        assertEquals(Relation.TOUCHING,proof.installation().storedContact().relation());
        assertTrue(proof.installation().storedContact().witness().isPresent());
        assertEquals(result.work(),proof.work());
        assertCommonLimits("elongated exact-touch prefix t=0.5",result.work());
    }

    @Test void elongatedOriginalCapsulePrefixKeepsIdealAndRoundedPathsDistinct(TestReporter reporter) {
        Aabb distantBox=new Aabb(new Vec3d(30_000_010,0,0),new Vec3d(30_000_011,1,1));
        Vec3d center=new Vec3d(30_000_000.1,0.5,0.5),halfLine=new Vec3d(0.2,0,0);
        Vec3d delta=new Vec3d(0.1,0,0);
        double t=0.5;
        Capsule start=capsuleAt(center,halfLine,0.25);
        Vec3d storedCenter=center.add(delta.multiply(t));
        Capsule stored=capsuleAt(storedCenter,halfLine,0.25);
        StoredInstallCandidate candidate=new StoredInstallCandidate(t,center,storedCenter,halfLine,stored);
        BigDecimal exactIdealX=new BigDecimal(center.x()).add(
                new BigDecimal(delta.x()).multiply(new BigDecimal(t)));
        BigDecimal roundedStoredX=new BigDecimal(storedCenter.x());
        BigDecimal exactIdealNegativeEndpointX=new BigDecimal(center.x())
                .subtract(new BigDecimal(halfLine.x()))
                .add(new BigDecimal(delta.x()).multiply(new BigDecimal(t)));
        BigDecimal roundedStoredNegativeEndpointX=new BigDecimal(stored.centerline().start().x());

        ElongatedOriginalPrefixResult result=CapsuleAabbContact3d.proveOriginalCapsuleTranslationPrefix(
                start,delta,t,distantBox,candidate,ACCURACY,LIMITS);
        ContactResult storedContact=CapsuleAabbContact3d.query(stored,distantBox,ACCURACY,LIMITS);
        reporter.publishEntry("elongated ideal versus canonical RN prefix",
                "t="+t+" exactIdealX="+exactIdealX+" storedRNX="+roundedStoredX
                        +" exactIdealNegativeEndpointX="+exactIdealNegativeEndpointX
                        +" storedNegativeEndpointRNX="+roundedStoredNegativeEndpointX
                        +" storedContact="+storedContact+" proof="+result);

        assertNotEquals(0,exactIdealX.compareTo(roundedStoredX),
                "fixture must distinguish exact originalDelta*t from the installed RN center");
        assertNotEquals(0,exactIdealNegativeEndpointX.compareTo(roundedStoredNegativeEndpointX),
                "fixture must exercise RN of center +/- fixedHalfLine after RN center placement");
        assertEquals(Relation.SEPARATED,storedContact.relation(),storedContact::toString);
        assertTrue(storedContact.signedClearanceMeters().orElseThrow().lower()>0,storedContact::toString);
        assertTrue(storedContact.witness().isPresent(),storedContact::toString);
        assertTrue(result.reason().isEmpty(),result::toString);
        ElongatedOriginalPrefixReceipt proof=result.proof().orElseThrow();
        assertEquals(t,proof.endFraction(),"the receipt must retain the supplied fraction");
        assertEquals(t,proof.candidate().fraction());
        assertEquals(t,proof.prefix().endFraction());
        assertEquals(PrefixKind.NONPENETRATING,proof.prefix().kind());
        assertEquals(Relation.SEPARATED,proof.installation().storedContact().relation());
        assertTrue(proof.installation().storedContact().witness().isPresent());
        assertEquals(result.work(),proof.work());
        assertCommonLimits("elongated exact/RN-distinct prefix t=0.5",result.work());
    }

    @Test void elongatedOriginalCapsulePrefixUsesUg084Run01ObservedGeometrySubcase(TestReporter reporter) {
        // The run01 evidence records these observed endpoint and radius values. It does not
        // uniquely bind the resolver's original delta/t, so this deliberately uses the
        // observed start-to-stored displacement as a numerical geometry subcase, not a replay.
        Aabb floor=new Aabb(new Vec3d(8,64,8),new Vec3d(9,65,9));
        Vec3d center=new Vec3d(8.5,66.39999997615814,8.5);
        Vec3d observedStoredCenter=new Vec3d(8.5,65.9000039434433,8.5);
        Vec3d halfLine=new Vec3d(0,0.5999999642372131,0);
        double radius=0.30000001192092896;
        Vec3d delta=observedStoredCenter.subtract(center);
        double t=1.0;
        Capsule start=capsuleAt(center,halfLine,radius);
        Vec3d storedCenter=center.add(delta.multiply(t));
        Capsule stored=capsuleAt(storedCenter,halfLine,radius);
        StoredInstallCandidate candidate=new StoredInstallCandidate(t,center,storedCenter,halfLine,stored);
        ContactResult storedContact=CapsuleAabbContact3d.query(stored,floor,ACCURACY,LIMITS);

        ElongatedOriginalPrefixResult result=CapsuleAabbContact3d.proveOriginalCapsuleTranslationPrefix(
                start,delta,t,floor,candidate,ACCURACY,LIMITS);
        reporter.publishEntry("UG084 run01 observed-geometry subcase; resolver delta/t not asserted",
                "start="+start.centerline()+" delta="+delta+" t="+t+" stored="+stored.centerline()
                        +" storedContact="+storedContact+" proof="+result);

        assertEquals(observedStoredCenter,storedCenter);
        assertEquals(new Vec3d(8.5,65.30000397920608,8.5),stored.centerline().start());
        assertEquals(new Vec3d(8.5,66.50000390768051,8.5),stored.centerline().end());
        assertEquals(Relation.SEPARATED,storedContact.relation(),storedContact::toString);
        Interval clearance=storedContact.signedClearanceMeters().orElseThrow();
        assertTrue(clearance.lower()>0,storedContact::toString);
        assertTrue(clearance.lower()<4e-6,storedContact::toString);
        assertTrue(storedContact.witness().isPresent(),storedContact::toString);
        assertTrue(result.reason().isEmpty(),result::toString);
        ElongatedOriginalPrefixReceipt proof=result.proof().orElseThrow();
        assertEquals(t,proof.endFraction());
        assertEquals(t,proof.prefix().endFraction());
        assertSame(candidate,proof.candidate());
        assertSame(stored,proof.installation().storedCapsule());
        assertEquals(Relation.SEPARATED,proof.installation().storedContact().relation());
        assertCommonLimits("UG084 run01 observed-geometry subcase",result.work());
    }

    @Test void ug084RoundedTouchDoesNotHideExactIdealMicroPenetration(TestReporter reporter) {
        Aabb floor=new Aabb(new Vec3d(8,64,8),new Vec3d(9,65,9));
        Vec3d center=new Vec3d(8.5,66.39999997615814,8.5);
        Vec3d halfLine=new Vec3d(0,0.5999999642372131,0);
        double radius=0.30000001192092896;
        Vec3d delta=new Vec3d(0,-1.08,0);
        double touchCandidate=0.4629629629629629;
        double[] fractions={Math.nextDown(touchCandidate),touchCandidate,Math.nextUp(touchCandidate)};
        String[] labels={"nextDown", "candidate", "nextUp"};
        Capsule start=capsuleAt(center,halfLine,radius);
        ContactResult startContact=CapsuleAabbContact3d.query(start,floor,ACCURACY,LIMITS);
        assertEquals(Relation.SEPARATED,startContact.relation(),startContact::toString);

        for(int index=0;index<fractions.length;index++) {
            double fraction=fractions[index];
            Vec3d storedCenter=center.add(delta.multiply(fraction));
            Capsule stored=capsuleAt(storedCenter,halfLine,radius);
            StoredInstallCandidate candidate=new StoredInstallCandidate(fraction,center,storedCenter,
                    halfLine,stored);
            ContactResult storedContact=CapsuleAabbContact3d.query(stored,floor,ACCURACY,LIMITS);

            // Independent exact dyadic arithmetic: this is the ideal support-point margin to
            // y=65 after translation, not the rounded stored pose queried above.
            BigDecimal idealFloorMargin=new BigDecimal(center.y())
                    .add(new BigDecimal(delta.y()).multiply(new BigDecimal(fraction)))
                    .subtract(new BigDecimal(halfLine.y()))
                    .subtract(new BigDecimal(radius))
                    .subtract(new BigDecimal(floor.max().y()));
            ElongatedOriginalPrefixResult result=CapsuleAabbContact3d.proveOriginalCapsuleTranslationPrefix(
                    start,delta,fraction,floor,candidate,ACCURACY,LIMITS);
            reporter.publishEntry("UG084 " + labels[index] + " RN-touch versus exact ideal",
                    "fraction="+fraction+" idealFloorMargin="+idealFloorMargin
                            +" storedCenter="+storedCenter+" storedContact="+storedContact
                            +" proof="+result);

            assertEquals(Relation.TOUCHING,storedContact.relation(),
                    labels[index]+" stored query: "+storedContact);
            assertTrue(storedContact.witness().isPresent(),storedContact::toString);
            if(index<2) {
                assertTrue(idealFloorMargin.signum()>0,labels[index]+" exact ideal support margin");
                assertTrue(result.reason().isEmpty(),labels[index]+" path proof: "+result);
                ElongatedOriginalPrefixReceipt proof=result.proof().orElseThrow();
                assertEquals(fraction,proof.endFraction());
                assertEquals(PrefixKind.NONPENETRATING,proof.prefix().kind());
            } else {
                assertTrue(idealFloorMargin.signum()<0,"nextUp must cross the exact ideal plane");
                assertTrue(result.proof().isEmpty(),result::toString);
                assertEquals(Reason.NO_PREFIX_CERTIFICATE,result.reason().orElseThrow(),result::toString);
            }
            assertCommonLimits("UG084 RN-touch candidate "+labels[index],result.work());
        }
    }

    @Test void elongatedOriginalCapsulePrefixRejectsIdentitySingleFaceAndBudgetFailures(TestReporter reporter) {
        Vec3d center=new Vec3d(-1,0.5,0.5),halfLine=new Vec3d(0,0.5,0);
        Vec3d delta=new Vec3d(1.5,0,0);
        double t=0.5;
        Capsule start=capsuleAt(center,halfLine,0.25);
        Vec3d storedCenter=center.add(delta.multiply(t));
        Capsule stored=capsuleAt(storedCenter,halfLine,0.25);
        StoredInstallCandidate good=new StoredInstallCandidate(t,center,storedCenter,halfLine,stored);

        StoredInstallCandidate wrongFraction=new StoredInstallCandidate(Math.nextDown(t),center,
                storedCenter,halfLine,stored);
        ElongatedOriginalPrefixResult fractionFailure=CapsuleAabbContact3d.proveOriginalCapsuleTranslationPrefix(
                start,delta,t,BOX,wrongFraction,ACCURACY,LIMITS);
        assertEquals(Reason.ROUNDED_INSTALL_UNSAFE,fractionFailure.reason().orElseThrow(),fractionFailure::toString);

        StoredInstallCandidate wrongHalfLine=new StoredInstallCandidate(t,center,storedCenter,
                new Vec3d(0,Math.nextUp(halfLine.y()),0),stored);
        ElongatedOriginalPrefixResult halfLineFailure=CapsuleAabbContact3d.proveOriginalCapsuleTranslationPrefix(
                start,delta,t,BOX,wrongHalfLine,ACCURACY,LIMITS);
        assertEquals(Reason.ROUNDED_INSTALL_UNSAFE,halfLineFailure.reason().orElseThrow(),halfLineFailure::toString);

        Capsule wrongRadius=capsuleAt(storedCenter,halfLine,Math.nextUp(0.25));
        StoredInstallCandidate wrongRadiusCandidate=new StoredInstallCandidate(t,center,storedCenter,
                halfLine,wrongRadius);
        ElongatedOriginalPrefixResult radiusFailure=CapsuleAabbContact3d.proveOriginalCapsuleTranslationPrefix(
                start,delta,t,BOX,wrongRadiusCandidate,ACCURACY,LIMITS);
        assertEquals(Reason.ROUNDED_INSTALL_UNSAFE,radiusFailure.reason().orElseThrow(),radiusFailure::toString);

        Vec3d wrongStoredCenter=new Vec3d(Math.nextUp(storedCenter.x()),storedCenter.y(),storedCenter.z());
        StoredInstallCandidate wrongCenter=new StoredInstallCandidate(t,center,wrongStoredCenter,halfLine,stored);
        ElongatedOriginalPrefixResult centerFailure=CapsuleAabbContact3d.proveOriginalCapsuleTranslationPrefix(
                start,delta,t,BOX,wrongCenter,ACCURACY,LIMITS);
        assertEquals(Reason.ROUNDED_INSTALL_UNSAFE,centerFailure.reason().orElseThrow(),centerFailure::toString);

        Vec3d cornerCenter=new Vec3d(-1,2,0.5),cornerHalf=new Vec3d(0,0,0.2);
        Vec3d crossCorner=new Vec3d(3,-3,0);
        Capsule cornerStart=capsuleAt(cornerCenter,cornerHalf,0.25);
        Vec3d cornerStoredCenter=cornerCenter.add(crossCorner);
        Capsule cornerStored=capsuleAt(cornerStoredCenter,cornerHalf,0.25);
        StoredInstallCandidate cornerCandidate=new StoredInstallCandidate(1,cornerCenter,
                cornerStoredCenter,cornerHalf,cornerStored);
        Capsule midpoint=capsuleAt(cornerCenter.add(crossCorner.multiply(0.5)),cornerHalf,0.25);
        assertEquals(Relation.OVERLAPPING,CapsuleAabbContact3d.query(midpoint,BOX,ACCURACY,LIMITS).relation(),
                "the segment crosses the box, while its endpoints do not share one exterior face");
        ElongatedOriginalPrefixResult faceFailure=CapsuleAabbContact3d.proveOriginalCapsuleTranslationPrefix(
                cornerStart,crossCorner,1,BOX,cornerCandidate,ACCURACY,LIMITS);
        assertTrue(faceFailure.proof().isEmpty(),faceFailure::toString);
        assertEquals(Reason.NO_PREFIX_CERTIFICATE,faceFailure.reason().orElseThrow(),faceFailure::toString);

        Vec3d longDelta=new Vec3d(65,0,0),farCenter=new Vec3d(-100,0.5,0.5);
        Capsule farStart=capsuleAt(farCenter,halfLine,0.25);
        Vec3d farStoredCenter=farCenter.add(longDelta.multiply(0.5));
        Capsule farStored=capsuleAt(farStoredCenter,halfLine,0.25);
        StoredInstallCandidate unsupportedCandidate=new StoredInstallCandidate(0.5,farCenter,
                farStoredCenter,halfLine,farStored);
        ElongatedOriginalPrefixResult unsupported=CapsuleAabbContact3d.proveOriginalCapsuleTranslationPrefix(
                farStart,longDelta,0.5,BOX,unsupportedCandidate,ACCURACY,LIMITS);
        assertEquals(Reason.UNSUPPORTED_DOMAIN,unsupported.reason().orElseThrow(),unsupported::toString);

        QueryLimits tiny=new QueryLimits(8192,100,128,32,2336,40000,64);
        ElongatedOriginalPrefixResult budgetFailure=CapsuleAabbContact3d.proveOriginalCapsuleTranslationPrefix(
                start,delta,t,BOX,good,ACCURACY,tiny);
        reporter.publishEntry("elongated-prefix rejects",
                "fraction="+fractionFailure+" halfLine="+halfLineFailure+" radius="+radiusFailure
                        +" storedCenter="+centerFailure+" noCommonFace="+faceFailure
                        +" unsupported="+unsupported+" budget="+budgetFailure);
        assertTrue(budgetFailure.proof().isEmpty(),budgetFailure::toString);
        assertEquals(Reason.EXACT_WORK_LIMIT,budgetFailure.reason().orElseThrow(),budgetFailure::toString);
        assertTrue(budgetFailure.work().exactAttempts()>0,budgetFailure::toString);
        assertTrue(budgetFailure.work().exactOps()<=tiny.maxExactOps(),budgetFailure::toString);
        assertTrue(budgetFailure.work().exactAttempts()>budgetFailure.work().exactOps(),budgetFailure::toString);
    }

    @Test void legacy062SpherePointPrefixAndBoundaryRemainAvailable() {
        Sphere sphere=new Sphere(new Vec3d(-2,0.5,0.5),0.25);
        Capsule pointCapsule=new Capsule(new Segment3d(sphere.center(),sphere.center()),sphere.radius());
        Vec3d delta=new Vec3d(1.75,0,0);
        Vec3d storedCenter=sphere.center().add(delta);
        Capsule stored=new Capsule(new Segment3d(storedCenter,storedCenter),sphere.radius());
        StoredInstallCandidate candidate=new StoredInstallCandidate(1,sphere.center(),storedCenter,
                new Vec3d(0,0,0),stored);

        OriginalPrefixResult legacyPoint=CapsuleAabbContact3d.proveOriginalTranslationPrefix(
                pointCapsule,delta,1,BOX,candidate,ACCURACY,LIMITS);
        BoundaryResult legacyBoundary=CapsuleAabbContact3d.refineTranslationBoundary(
                pointCapsule,delta,BOX,new BoundarySearchHint(0,1),candidate,ACCURACY,LIMITS);

        assertTrue(legacyPoint.reason().isEmpty(),legacyPoint::toString);
        assertEquals(Relation.TOUCHING,legacyPoint.proof().orElseThrow().installation()
                .storedContact().relation());
        assertEquals(BoundaryKind.NONPENETRATING_FULL,legacyBoundary.kind(),legacyBoundary::toString);
        assertEquals(PrefixKind.NONPENETRATING,legacyBoundary.prefix().orElseThrow().kind());
        assertEquals(Relation.TOUCHING,legacyBoundary.installation().orElseThrow().storedContact().relation());
    }

    private static BigDecimal exactOriginalMargin(double center,double delta,double fraction,
                                                  BigDecimal threshold) {
        return new BigDecimal(center).add(new BigDecimal(delta).multiply(new BigDecimal(fraction)))
                .subtract(threshold);
    }

    @Test void ug044SameFaceCorrectionProvesNonpenetratingStoredTouch(TestReporter reporter) {
        Aabb box=new Aabb(new Vec3d(8,68,8),new Vec3d(9,69,9));
        Vec3d center=new Vec3d(9.375,68.5,8.5),halfLine=new Vec3d(0,0.5,0);
        Vec3d delta=new Vec3d(-0.12499999999999997,-2.7755575615628914E-17,0);
        Capsule start=capsuleAt(center,halfLine,0.25);
        Vec3d storedCenter=center.add(delta);
        Capsule stored=capsuleAt(storedCenter,halfLine,0.25);
        StoredInstallCandidate candidate=new StoredInstallCandidate(1.0,center,storedCenter,halfLine,stored);
        ContactResult startContact=CapsuleAabbContact3d.query(start,box,ACCURACY,LIMITS);
        ContactResult storedContact=CapsuleAabbContact3d.query(stored,box,ACCURACY,LIMITS);
        BoundaryResult result=CapsuleAabbContact3d.refineTranslationBoundary(start,delta,box,
                new BoundarySearchHint(0,1),candidate,ACCURACY,LIMITS);
        reporter.publishEntry("UG-044 exact tuple",
                "start="+startContact+" stored="+storedContact+" boundary="+result);

        assertEquals(Relation.SEPARATED,startContact.relation(),startContact::toString);
        assertTrue(startContact.signedClearanceMeters().orElseThrow().lower()>0,startContact::toString);
        assertEquals(new Vec3d(9.25,68.5,8.5),storedCenter);
        assertEquals(new Vec3d(9.375,68,8.5),start.centerline().start());
        assertEquals(new Vec3d(9.375,69,8.5),start.centerline().end());
        assertEquals(new Vec3d(9.25,68,8.5),stored.centerline().start());
        assertEquals(new Vec3d(9.25,69,8.5),stored.centerline().end());
        assertEquals(Relation.TOUCHING,storedContact.relation(),storedContact::toString);
        assertEquals(BoundaryKind.NONPENETRATING_FULL,result.kind(),result::toString);
        assertEquals(Relation.SEPARATED,result.startRelation());
        PrefixReceipt prefix=result.prefix().orElseThrow();
        assertEquals(PrefixKind.NONPENETRATING,prefix.kind(),result::toString);
        assertEquals(1.0,prefix.endFraction());
        InstalledPoseReceipt installed=result.installation().orElseThrow();
        assertEquals(1.0,installed.fraction());
        assertEquals(storedCenter,installed.storedCenter());
        assertEquals(halfLine,installed.halfLine());
        assertSame(stored,installed.storedCapsule());
        assertEquals(storedContact.signedClearanceMeters(),installed.storedContact().signedClearanceMeters());
        assertEquals(storedContact.witness(),installed.storedContact().witness());
        assertEquals(Relation.TOUCHING,installed.storedContact().relation());
        assertTrue(result.reason().isEmpty());
        assertTrue(result.firstObstructionBracket().isEmpty());
        assertCommonLimits("UG-044 exact tuple",result.work());
    }

    @Test void sameFaceCorrectionProofCoversAllSixAabbFaces(TestReporter reporter) {
        for(int axis=0;axis<3;axis++) for(boolean maximum:new boolean[]{false,true}) {
            String face=new String[]{"X","Y","Z"}[axis]+(maximum?"_MAX":"_MIN");
            Vec3d center=faceFixtureCenter(axis,maximum);
            Vec3d halfLine=faceFixtureHalfLine(axis);
            Vec3d delta=faceFixtureDelta(axis,maximum);
            Capsule start=capsuleAt(center,halfLine,0.25);
            Vec3d storedCenter=center.add(delta);
            Capsule stored=capsuleAt(storedCenter,halfLine,0.25);
            StoredInstallCandidate candidate=new StoredInstallCandidate(1.0,center,storedCenter,halfLine,stored);
            ContactResult startContact=CapsuleAabbContact3d.query(start,BOX,ACCURACY,LIMITS);
            ContactResult storedContact=CapsuleAabbContact3d.query(stored,BOX,ACCURACY,LIMITS);
            BoundaryResult result=CapsuleAabbContact3d.refineTranslationBoundary(start,delta,BOX,
                    new BoundarySearchHint(0,1),candidate,ACCURACY,LIMITS);
            reporter.publishEntry("same-face "+face,
                    "start="+startContact.relation()+" stored="+storedContact.relation()+" boundary="+result);
            assertEquals(Relation.SEPARATED,startContact.relation(),face+" start: "+startContact);
            assertEquals(Relation.TOUCHING,storedContact.relation(),face+" stored: "+storedContact);
            assertEquals(BoundaryKind.NONPENETRATING_FULL,result.kind(),face+": "+result);
            assertEquals(PrefixKind.NONPENETRATING,result.prefix().orElseThrow().kind(),face+": "+result);
            InstalledPoseReceipt installed=result.installation().orElseThrow();
            assertEquals(1.0,installed.fraction(),face+": "+result);
            assertEquals(storedCenter,installed.storedCenter(),face+": "+result);
            assertEquals(halfLine,installed.halfLine(),face+": "+result);
            assertSame(stored,installed.storedCapsule(),face+": "+result);
            assertEquals(storedContact.signedClearanceMeters(),installed.storedContact().signedClearanceMeters(),
                    face+": "+result);
            assertEquals(storedContact.witness(),installed.storedContact().witness(),face+": "+result);
            assertCommonLimits("same-face "+face,result.work());
        }
    }

    @Test void sameFaceProofRejectsOneEndpointInsideRadiusSlabAndOppositeFaceCrossing(TestReporter reporter) {
        double origin=30_000_000.0;
        double nearCornerYGap=29_058_991.0/134_217_728.0;
        Aabb remoteBox=new Aabb(new Vec3d(origin,origin,origin),
                new Vec3d(origin+1,origin+1,origin+1));
        Vec3d center=new Vec3d(origin-0.25,origin-(nearCornerYGap+0.125)/2,origin+0.5);
        Vec3d halfLine=new Vec3d(-0.125,(nearCornerYGap-0.125)/2,0);
        Vec3d delta=new Vec3d(0,-0x1.0p-30,0);
        Capsule start=capsuleAt(center,halfLine,0.25);
        Vec3d storedCenter=center.add(delta);
        Capsule stored=capsuleAt(storedCenter,halfLine,0.25);
        StoredInstallCandidate install=new StoredInstallCandidate(1,center,storedCenter,halfLine,stored);
        ContactResult startContact=CapsuleAabbContact3d.query(start,remoteBox,ACCURACY,LIMITS);
        ContactResult storedContact=CapsuleAabbContact3d.query(stored,remoteBox,ACCURACY,LIMITS);
        BoundaryResult result=CapsuleAabbContact3d.refineTranslationBoundary(start,delta,remoteBox,
                new BoundarySearchHint(0,1),install,ACCURACY,LIMITS);
        reporter.publishEntry("one endpoint in expanded face slabs",
                "firstEndpoint="+start.centerline().start()+" lastEndpoint="+start.centerline().end()
                        +" clearance="+startContact.signedClearanceMeters()+" stored="+storedContact
                        +" boundary="+result);
        assertEquals(Relation.SEPARATED,startContact.relation(),startContact::toString);
        assertEquals(Relation.SEPARATED,storedContact.relation(),storedContact::toString);
        assertTrue(startContact.signedClearanceMeters().orElseThrow().lower()>0,startContact::toString);
        assertTrue(startContact.signedClearanceMeters().orElseThrow().lower()<4e-7,
                "generic clearance proof must be smaller than the conservative 30-million-coordinate correction bound");
        Vec3d first=start.centerline().start(),second=start.centerline().end();
        assertNotEquals(first.x()>origin-0.25,second.x()>origin-0.25,
                "exactly one endpoint is inside the X_MIN radius-expanded half-space");
        assertTrue(first.y()>origin-0.25 && second.y()>origin-0.25,
                "both endpoints lie inside the Y_MIN radius-expanded half-space");
        assertEquals(BoundaryKind.UNRESOLVED,result.kind(),result::toString);
        assertEquals(Reason.CORRECTION_PATH_UNPROVED,result.reason().orElseThrow(),result::toString);
        assertTrue(result.installation().isEmpty(),result::toString);

        Capsule crossing=capsuleAt(new Vec3d(0.5,0.5,0.5),new Vec3d(0.75,0,0),0.25);
        StoredInstallCandidate crossingInstall=new StoredInstallCandidate(1,new Vec3d(0.5,0.5,0.5),
                new Vec3d(0.5,0.5,0.5),new Vec3d(0.75,0,0),crossing);
        ContactResult crossingContact=CapsuleAabbContact3d.query(crossing,BOX,ACCURACY,LIMITS);
        BoundaryResult crossingResult=CapsuleAabbContact3d.refineTranslationBoundary(crossing,
                new Vec3d(0,0,0),BOX,new BoundarySearchHint(0,1),crossingInstall,ACCURACY,LIMITS);
        reporter.publishEntry("opposite-face crossing",
                "endpoints="+crossing.centerline()+" static="+crossingContact+" boundary="+crossingResult);
        assertEquals(Relation.OVERLAPPING,crossingContact.relation(),crossingContact::toString);
        assertEquals(BoundaryKind.INITIAL_OVERLAP,crossingResult.kind(),crossingResult::toString);
        assertTrue(crossingResult.installation().isEmpty(),crossingResult::toString);
    }

    @Test void sameFaceProofFailsClosedAtExactOperationLimit(TestReporter reporter) {
        Aabb box=new Aabb(new Vec3d(8,68,8),new Vec3d(9,69,9));
        Vec3d center=new Vec3d(9.375,68.5,8.5),halfLine=new Vec3d(0,0.5,0);
        Vec3d delta=new Vec3d(-0.12499999999999997,-2.7755575615628914E-17,0);
        Capsule start=capsuleAt(center,halfLine,0.25);
        Vec3d storedCenter=center.add(delta);
        Capsule stored=capsuleAt(storedCenter,halfLine,0.25);
        StoredInstallCandidate candidate=new StoredInstallCandidate(1,center,storedCenter,halfLine,stored);
        QueryLimits limitAtOldRedBoundary=new QueryLimits(8192,3515,128,32,2336,40000,64);
        BoundaryResult result=CapsuleAabbContact3d.refineTranslationBoundary(start,delta,box,
                new BoundarySearchHint(0,1),candidate,ACCURACY,limitAtOldRedBoundary);
        reporter.publishEntry("same-face exact-op exhaustion",
                "maxExactOps=3515 result="+result);
        assertEquals(BoundaryKind.UNRESOLVED,result.kind(),result::toString);
        assertEquals(Reason.EXACT_WORK_LIMIT,result.reason().orElseThrow(),result::toString);
        assertEquals(3515,result.work().exactOps(),result::toString);
        assertTrue(result.work().exactAttempts()>result.work().exactOps(),result::toString);
        assertTrue(result.installation().isEmpty(),result::toString);
    }

    @Test void sameFaceProofRejectsIdentityRadiusAndStoredOverlap(TestReporter reporter) {
        Aabb box=new Aabb(new Vec3d(8,68,8),new Vec3d(9,69,9));
        Vec3d center=new Vec3d(9.375,68.5,8.5),halfLine=new Vec3d(0,0.5,0);
        Vec3d delta=new Vec3d(-0.12499999999999997,-2.7755575615628914E-17,0);
        Capsule start=capsuleAt(center,halfLine,0.25);
        Vec3d storedCenter=center.add(delta);
        Capsule stored=capsuleAt(storedCenter,halfLine,0.25);
        Vec3d wrongAcceptedCenter=new Vec3d(storedCenter.x(),Math.nextUp(storedCenter.y()),storedCenter.z());
        StoredInstallCandidate wrongIdentity=new StoredInstallCandidate(1,center,wrongAcceptedCenter,halfLine,stored);
        BoundaryResult identityResult=CapsuleAabbContact3d.refineTranslationBoundary(start,delta,box,
                new BoundarySearchHint(0,1),wrongIdentity,ACCURACY,LIMITS);
        reporter.publishEntry("same-face install identity rejection","result="+identityResult);
        assertEquals(BoundaryKind.UNRESOLVED,identityResult.kind(),identityResult::toString);
        assertEquals(Reason.ROUNDED_INSTALL_UNSAFE,identityResult.reason().orElseThrow(),identityResult::toString);
        assertTrue(identityResult.installation().isEmpty(),identityResult::toString);

        Capsule wrongRadiusStored=capsuleAt(storedCenter,halfLine,Math.nextUp(0.25));
        StoredInstallCandidate wrongRadius=new StoredInstallCandidate(1,center,storedCenter,halfLine,wrongRadiusStored);
        BoundaryResult radiusResult=CapsuleAabbContact3d.refineTranslationBoundary(start,delta,box,
                new BoundarySearchHint(0,1),wrongRadius,ACCURACY,LIMITS);
        reporter.publishEntry("same-face raw radius rejection","result="+radiusResult);
        assertEquals(BoundaryKind.UNRESOLVED,radiusResult.kind(),radiusResult::toString);
        assertEquals(Reason.ROUNDED_INSTALL_UNSAFE,radiusResult.reason().orElseThrow(),radiusResult::toString);
        assertTrue(radiusResult.installation().isEmpty(),radiusResult::toString);

        Vec3d overlapCenter=new Vec3d(Math.nextDown(9.25),68.5,8.5);
        Vec3d overlapDelta=overlapCenter.subtract(center);
        Vec3d roundedOverlapCenter=center.add(overlapDelta);
        Capsule overlapStored=capsuleAt(roundedOverlapCenter,halfLine,0.25);
        StoredInstallCandidate overlapCandidate=new StoredInstallCandidate(1,center,
                roundedOverlapCenter,halfLine,overlapStored);
        ContactResult overlapContact=CapsuleAabbContact3d.query(overlapStored,box,ACCURACY,LIMITS);
        BoundaryResult overlapResult=CapsuleAabbContact3d.refineTranslationBoundary(start,overlapDelta,box,
                new BoundarySearchHint(0,1),overlapCandidate,ACCURACY,LIMITS);
        reporter.publishEntry("same-face stored overlap rejection",
                "center="+roundedOverlapCenter+" contact="+overlapContact+" result="+overlapResult);
        assertEquals(Relation.OVERLAPPING,overlapContact.relation(),overlapContact::toString);
        assertEquals(BoundaryKind.UNRESOLVED,overlapResult.kind(),overlapResult::toString);
        assertTrue(overlapResult.installation().isEmpty(),overlapResult::toString);
    }

    @Test void nearContactMillionCorrectionKeepsPositiveStoredGap() {
        double wall=1_000_000.0;
        Vec3d center=new Vec3d(wall-1,0.5,0.5),half=new Vec3d(0,0.3,0);
        Vec3d delta=new Vec3d(0.74999,0,0);
        Capsule start=new Capsule(new Segment3d(center.subtract(half),center.add(half)),0.25);
        Vec3d storedCenter=center.add(delta);
        Capsule stored=new Capsule(new Segment3d(storedCenter.subtract(half),storedCenter.add(half)),0.25);
        Aabb wallBox=new Aabb(new Vec3d(wall,0,0),new Vec3d(wall+1,1,1));
        BoundaryResult result=CapsuleAabbContact3d.refineTranslationBoundary(start,delta,wallBox,
                new BoundarySearchHint(0,1),new StoredInstallCandidate(1,center,storedCenter,half,stored),
                ACCURACY,LIMITS);
        assertEquals(BoundaryKind.SEPARATED_FULL,result.kind(),result::toString);
        assertEquals(Relation.SEPARATED,result.installation().orElseThrow().storedContact().relation());
        assertTrue(result.installation().orElseThrow().storedContact().signedClearanceMeters()
                .orElseThrow().lower()>0);
    }

    private static BoundaryResult query(Capsule c,Vec3d delta,StoredInstallCandidate install) {
        return CapsuleAabbContact3d.refineTranslationBoundary(c,delta,BOX,
                new BoundarySearchHint(0,1),install,ACCURACY,LIMITS);
    }

    private static Vec3d faceFixtureCenter(int axis,boolean maximum) {
        double[] center={0.5,0.5,0.5};
        center[axis]=maximum?1.375:-0.375;
        return new Vec3d(center[0],center[1],center[2]);
    }

    private static Vec3d faceFixtureHalfLine(int axis) {
        double[] half={0,0,0};
        half[(axis+1)%3]=0.5;
        return new Vec3d(half[0],half[1],half[2]);
    }

    private static Vec3d faceFixtureDelta(int axis,boolean maximum) {
        double[] delta={0,0,0};
        delta[axis]=maximum?-0.125:0.125;
        delta[(axis+1)%3]=-2.7755575615628914E-17;
        return new Vec3d(delta[0],delta[1],delta[2]);
    }

    private static StoredInstallCandidate candidate(Capsule c,double t,Vec3d delta) {
        Vec3d start = c.centerline().start(), stored = start.add(delta.multiply(t));
        return new StoredInstallCandidate(t,start,stored,new Vec3d(0,0,0),point(stored.x(),stored.y(),stored.z(),c.radius()));
    }

    private record InwardFixture(String name,Vec3d center,Vec3d halfLine,double radius,
                                 Vec3d delta,double safeFraction,double firstTouchFraction,
                                 PrefixKind expectedPrefixKind,
                                 Set<Face> expectedStartFaces) {
        private Capsule startCapsule() { return capsuleAt(center,halfLine,radius); }
        private StoredInstallCandidate installCandidate() {
            Vec3d storedCenter=center.add(delta.multiply(safeFraction));
            return new StoredInstallCandidate(safeFraction,center,storedCenter,halfLine,
                    capsuleAt(storedCenter,halfLine,radius));
        }
        private Capsule capsuleAtFraction(double fraction) {
            return capsuleAt(center.add(delta.multiply(fraction)),halfLine,radius);
        }
    }

    private record Measurement<T>(T result,long elapsedNanos,Long threadAllocatedBytes) { }

    private static void verifyUgInwardFixture(InwardFixture fixture,TestReporter reporter) {
        Capsule start=fixture.startCapsule();
        StoredInstallCandidate candidate=fixture.installCandidate();
        BoundarySearchHint hint=new BoundarySearchHint(0,1);
        Measurement<ContactResult> startQuery=measure(() -> CapsuleAabbContact3d.query(
                start,BOX,ACCURACY,LIMITS));
        Measurement<BoundaryResult> boundary=measure(() -> CapsuleAabbContact3d.refineTranslationBoundary(
                start,fixture.delta(),BOX,hint,candidate,
                ACCURACY,LIMITS));
        Measurement<ContactResult> overlapProbe=null;
        if(boundary.result().firstObstructionBracket().isPresent()) {
            double high=boundary.result().firstObstructionBracket().orElseThrow().upper();
            Capsule highPose=fixture.capsuleAtFraction(high);
            overlapProbe=measure(() -> CapsuleAabbContact3d.query(
                    highPose,BOX,ACCURACY,LIMITS));
        }

        String startReport="relation="+startQuery.result().relation()
                +" clearance="+startQuery.result().signedClearanceMeters()
                +" witness="+startQuery.result().witness();
        String boundaryReport="kind="+boundary.result().kind()
                +" startRelation="+boundary.result().startRelation()
                +" prefix="+boundary.result().prefix()
                +" bracket="+boundary.result().firstObstructionBracket()
                +" installation="+boundary.result().installation()
                +" reason="+boundary.result().reason();
        printMeasurement(fixture.name(),"startStatic",startReport,startQuery,reporter);
        printMeasurement(fixture.name(),"inwardBoundary",boundaryReport,boundary,reporter);
        if(overlapProbe!=null) {
            printMeasurement(fixture.name(),"bracketHighStatic",
                    "relation="+overlapProbe.result().relation()
                            +" clearance="+overlapProbe.result().signedClearanceMeters(),
                    overlapProbe,reporter);
        }

        ContactResult startResult=startQuery.result();
        assertEquals(Relation.SEPARATED,startResult.relation(),startResult::toString);
        assertTrue(startResult.signedClearanceMeters().orElseThrow().lower()>0,startResult::toString);
        assertTrue(startResult.witness().orElseThrow().feature().complete(),startResult::toString);
        assertTrue(startResult.witness().orElseThrow().feature().possibilities().stream()
                .anyMatch(feature->feature.activeFaces().equals(fixture.expectedStartFaces())),startResult::toString);

        BoundaryResult result=boundary.result();
        assertEquals(BoundaryKind.CERTIFIED_BOUNDARY,result.kind(),result::toString);
        assertEquals(Relation.SEPARATED,result.startRelation(),result::toString);
        PrefixReceipt prefix=result.prefix().orElseThrow();
        assertEquals(fixture.expectedPrefixKind(),prefix.kind(),result::toString);
        assertTrue(fixture.safeFraction()>0 && fixture.safeFraction()<fixture.firstTouchFraction());
        assertTrue(prefix.endFraction()>=fixture.safeFraction(),result::toString);
        Interval bracket=result.firstObstructionBracket().orElseThrow();
        assertTrue(bracket.lower()<=fixture.firstTouchFraction()
                && bracket.upper()>fixture.firstTouchFraction(),result::toString);
        double deltaNormUpper=Math.nextUp(Math.hypot(Math.hypot(fixture.delta().x(),fixture.delta().y()),
                fixture.delta().z()));
        assertTrue(Math.nextUp(deltaNormUpper*(bracket.upper()-bracket.lower()))
                <=ACCURACY.maxPathWidthMeters(),result::toString);

        InstalledPoseReceipt installed=result.installation().orElseThrow();
        assertEquals(candidate.fraction(),installed.fraction(),result::toString);
        assertEquals(candidate.acceptedStartCenter(),fixture.center(),result::toString);
        assertEquals(candidate.acceptedStoredCenter(),installed.storedCenter(),result::toString);
        assertEquals(candidate.acceptedHalfLine(),installed.halfLine(),result::toString);
        assertSame(candidate.actualStoredCapsule(),installed.storedCapsule(),result::toString);
        ContactResult stored=installed.storedContact();
        assertEquals(Relation.SEPARATED,stored.relation(),stored::toString);
        assertTrue(stored.signedClearanceMeters().orElseThrow().lower()>0,stored::toString);

        assertNotNull(overlapProbe,"CERTIFIED_BOUNDARY must expose its strict-overlap high endpoint");
        assertEquals(Relation.OVERLAPPING,overlapProbe.result().relation(),overlapProbe.result()::toString);
        assertTrue(overlapProbe.result().signedClearanceMeters().orElseThrow().upper()<0,
                overlapProbe.result()::toString);

        assertCommonLimits(""+fixture.name()+" startStatic",startResult.work());
        assertCommonLimits(""+fixture.name()+" inwardBoundary",result.work());
        assertCommonLimits(""+fixture.name()+" bracketHighStatic",overlapProbe.result().work());
    }

    private static void assertCommonLimits(String label,Work work) {
        assertAll(label,
                ()->assertTrue(work.exactAttempts()>=0),
                ()->assertTrue(work.exactOps()>=0 && work.exactOps()<=LIMITS.maxExactOps()),
                ()->assertTrue(work.peakExactBits()>=0 && work.peakExactBits()<=LIMITS.maxExactBits()),
                ()->assertTrue(work.staticAttempts()>=0 && work.staticAttempts()<=LIMITS.maxStaticEvaluations()),
                ()->assertTrue(work.staticEvaluations()>=0 && work.staticEvaluations()<=LIMITS.maxStaticEvaluations()),
                ()->assertTrue(work.staticEvaluations()<=work.staticAttempts()),
                ()->assertTrue(work.staticCandidateUnits()>=0),
                ()->assertTrue(work.castAttempts()>=0 && work.castAttempts()<=LIMITS.maxCastQueries()),
                ()->assertTrue(work.castQueries()>=0 && work.castQueries()<=LIMITS.maxCastQueries()),
                ()->assertTrue(work.castQueries()<=work.castAttempts()),
                ()->assertTrue(work.castEvaluations()>=0 && work.castEvaluations()<=LIMITS.maxCastEvaluations()),
                ()->assertTrue(work.castCandidateUnits()>=0),
                ()->assertEquals(Math.addExact(work.staticCandidateUnits(),work.castCandidateUnits()),
                        work.totalCandidateUnits()),
                ()->assertTrue(work.totalCandidateUnits()>=0
                        && work.totalCandidateUnits()<=LIMITS.maxCandidateUnits()),
                ()->assertTrue(work.refinementSteps()>=0
                        && work.refinementSteps()<=LIMITS.maxRefinementSteps()));
    }

    private static <T> Measurement<T> measure(Supplier<T> query) {
        ThreadMXBean allocationBean=allocationBean();
        long threadId=Thread.currentThread().threadId();
        long allocationBefore=allocationBean==null?-1:allocationBean.getThreadAllocatedBytes(threadId);
        long start=System.nanoTime();
        T result=query.get();
        long elapsed=System.nanoTime()-start;
        long allocationAfter=allocationBean==null?-1:allocationBean.getThreadAllocatedBytes(threadId);
        Long allocated=allocationBefore>=0 && allocationAfter>=allocationBefore
                ? allocationAfter-allocationBefore:null;
        return new Measurement<>(result,elapsed,allocated);
    }

    private static ThreadMXBean allocationBean() {
        java.lang.management.ThreadMXBean bean=ManagementFactory.getThreadMXBean();
        return bean instanceof ThreadMXBean allocation
                && allocation.isThreadAllocatedMemorySupported()
                && allocation.isThreadAllocatedMemoryEnabled()?allocation:null;
    }

    private static void printMeasurement(String fixture,String phase,String result,
                                         Measurement<?> measurement,TestReporter reporter) {
        Work work=measurement.result() instanceof ContactResult contact?contact.work():
                ((BoundaryResult)measurement.result()).work();
        String line="UG_CONTACT_MEASURE case="+fixture+" phase="+phase
                +" result="+result+" wallNanos="+measurement.elapsedNanos()
                +" threadAllocatedBytes="+(measurement.threadAllocatedBytes()==null
                        ?"unavailable":measurement.threadAllocatedBytes())
                +" work="+workFields(work);
        System.out.println(line);
        reporter.publishEntry("UG_CONTACT_MEASURE",line);
    }

    private static String workFields(Work w) {
        return "{exactAttempts="+w.exactAttempts()+", exactOps="+w.exactOps()
                +", peakExactBits="+w.peakExactBits()+", staticAttempts="+w.staticAttempts()
                +", staticEvaluations="+w.staticEvaluations()+", staticCandidateUnits="+w.staticCandidateUnits()
                +", castAttempts="+w.castAttempts()+", castQueries="+w.castQueries()
                +", castEvaluations="+w.castEvaluations()+", castCandidateUnits="+w.castCandidateUnits()
                +", totalCandidateUnits="+w.totalCandidateUnits()+", refinementSteps="+w.refinementSteps()+"}";
    }

    private static void reportBudgetWork(TestReporter reporter,String scenario,Work work) {
        String line="UG_CONTACT_BUDGET case="+scenario+" work="+workFields(work);
        System.out.println(line);
        reporter.publishEntry("UG_CONTACT_BUDGET",line);
    }

    private static Capsule capsuleAt(Vec3d center,Vec3d halfLine,double radius) {
        return new Capsule(new Segment3d(center.subtract(halfLine),center.add(halfLine)),radius);
    }

    private static Capsule point(double x,double y,double z,double r) {
        Vec3d p = new Vec3d(x,y,z); return new Capsule(new Segment3d(p,p),r);
    }
}
