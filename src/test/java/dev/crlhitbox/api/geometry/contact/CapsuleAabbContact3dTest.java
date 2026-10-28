package dev.crlhitbox.api.geometry.contact;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Vec3d;
import dev.crlhitbox.api.geometry.contact.CapsuleAabbContact3d.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

import java.math.BigInteger;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CapsuleAabbContact3dTest {
    private static final ContactAccuracy ACCURACY = new ContactAccuracy(1e-6,1e-6,1e-5);
    private static final QueryLimits LIMITS = new QueryLimits(8192,20000,128,32,2336,40000,64);
    private static final Aabb BOX = new Aabb(new Vec3d(0,0,0),new Vec3d(1,1,1));

    @Test void axisExitUsesCompleteCapsuleAndRejectsCrossWallDirection() {
        Capsule nearMinimum=point(0.25,0.5,0.5,0.2);
        AxisExitResult suggestions=CapsuleAabbContact3d.axisExitCandidates(
                nearMinimum,BOX,ACCURACY,LIMITS);
        assertEquals(AxisExitKind.CANDIDATES,suggestions.kind());
        assertTrue(suggestions.candidates().stream().anyMatch(c->c.exitFace()==Face.X_MIN));
        assertTrue(suggestions.candidates().stream().noneMatch(c->c.exitFace()==Face.X_MAX));
        assertEquals(AxisExitCheckKind.CERTIFIED,CapsuleAabbContact3d.checkAxisExit(
                nearMinimum,BOX,Face.X_MIN,point(-0.2,0.5,0.5,0.2),ACCURACY,LIMITS).kind());
        assertEquals(AxisExitCheckKind.INVALID_PATH,CapsuleAabbContact3d.checkAxisExit(
                nearMinimum,BOX,Face.X_MAX,point(1.2,0.5,0.5,0.2),ACCURACY,LIMITS).kind());
    }

    @Test void axisExitMayHaveNoConservativeCandidateForSpanningCenterline() {
        Capsule diagonal=new Capsule(new Segment3d(new Vec3d(0.25,0.25,0.25),
                new Vec3d(0.75,0.75,0.75)),0.1);
        AxisExitResult result=CapsuleAabbContact3d.axisExitCandidates(diagonal,BOX,ACCURACY,LIMITS);
        assertEquals(AxisExitKind.NO_CONSERVATIVE_EXIT,result.kind());
        assertTrue(result.candidates().isEmpty());
        assertEquals(AxisExitCheckKind.INVALID_PATH,CapsuleAabbContact3d.checkAxisExit(
                diagonal,BOX,Face.X_MAX,new Capsule(new Segment3d(
                        new Vec3d(1.1,0.25,0.25),new Vec3d(1.6,0.75,0.75)),0.1),
                ACCURACY,LIMITS).kind());
    }

    @Test void axisExitChecksStoredRoundingAndStartRelation() {
        Capsule start=point(0.5,0.5,0.5,0.25);
        assertEquals(AxisExitCheckKind.END_NOT_CLEAR,CapsuleAabbContact3d.checkAxisExit(
                start,BOX,Face.X_MAX,point(Math.nextDown(1.25),0.5,0.5,0.25),
                ACCURACY,LIMITS).kind());
        assertEquals(AxisExitCheckKind.CERTIFIED,CapsuleAabbContact3d.checkAxisExit(
                start,BOX,Face.X_MAX,point(1.25,0.5,0.5,0.25),ACCURACY,LIMITS).kind());
        assertEquals(AxisExitKind.START_NOT_OVERLAPPING,
                CapsuleAabbContact3d.axisExitCandidates(point(1.25,0.5,0.5,0.25),
                        BOX,ACCURACY,LIMITS).kind());
    }

    @Test void axisExitCertifiesCompleteNondegenerateCenterline() {
        Capsule start=new Capsule(new Segment3d(new Vec3d(0.6,0.3,0.5),
                new Vec3d(0.8,0.7,0.5)),0.25);
        Capsule stored=new Capsule(new Segment3d(new Vec3d(1.25,0.3,0.5),
                new Vec3d(1.45,0.7,0.5)),0.25);
        AxisExitResult result=CapsuleAabbContact3d.axisExitCandidates(start,BOX,ACCURACY,LIMITS);
        assertTrue(result.candidates().stream().anyMatch(c->c.exitFace()==Face.X_MAX));
        assertEquals(AxisExitCheckKind.CERTIFIED,CapsuleAabbContact3d.checkAxisExit(
                start,BOX,Face.X_MAX,stored,ACCURACY,LIMITS).kind());
    }

    @Test void faceEdgeCornerAndExactTouch() {
        assertContact(point(-1,0.5,0.5,0.5),Relation.SEPARATED,0.5);
        assertContact(point(-0.5,0.5,0.5,0.5),Relation.TOUCHING,0);
        assertContact(point(-0.25,0.5,0.5,0.5),Relation.OVERLAPPING,-0.25);
        assertContact(point(-0.5,-0.5,0.5,0.5),Relation.SEPARATED,
                Math.sqrt(0.5)-0.5);
        assertContact(point(-0.5,-0.5,-0.5,0),Relation.SEPARATED,
                Math.sqrt(0.75));
        ContactResult corner=CapsuleAabbContact3d.query(point(-0.5,-0.5,-0.5,0),BOX,ACCURACY,LIMITS);
        FeatureAttribution attribution=corner.witness().orElseThrow().feature();
        assertEquals(java.util.Set.of(Face.X_MIN,Face.Y_MIN,Face.Z_MIN),
                attribution.possibilities().iterator().next().activeFaces());
        assertTrue(attribution.complete());
    }

    @Test void segmentInteriorWinsOverBothEndpoints() {
        Capsule spanning = new Capsule(new Segment3d(new Vec3d(-2,0.5,0.5),
                new Vec3d(2,0.5,0.5)),0);
        ContactResult r = CapsuleAabbContact3d.query(spanning,BOX,ACCURACY,LIMITS);
        assertEquals(Relation.TOUCHING,r.relation());
        assertEquals(0,r.signedClearanceMeters().orElseThrow().lower());
        assertEquals(0,r.signedClearanceMeters().orElseThrow().upper());
        assertTrue(r.work().staticCandidateUnits()>0);
    }

    @Test void nonpointUniqueAndTiedFeaturesAreComplete() {
        Capsule unique=new Capsule(new Segment3d(new Vec3d(-2,-2,0.5),
                new Vec3d(-1,-1,0.5)),0);
        ContactResult u=CapsuleAabbContact3d.query(unique,BOX,ACCURACY,LIMITS);
        FeatureAttribution one=u.witness().orElseThrow().feature();
        assertTrue(one.complete());
        assertEquals(java.util.Set.of(new Feature(java.util.Set.of(Face.X_MIN,Face.Y_MIN))),
                one.possibilities());

        Capsule parallel=new Capsule(new Segment3d(new Vec3d(-1,-1,0.5),
                new Vec3d(-1,2,0.5)),0.25);
        ContactResult p=CapsuleAabbContact3d.query(parallel,BOX,ACCURACY,LIMITS);
        FeatureAttribution tied=p.witness().orElseThrow().feature();
        assertTrue(tied.complete());
        assertEquals(java.util.Set.of(
                new Feature(java.util.Set.of(Face.X_MIN)),
                new Feature(java.util.Set.of(Face.X_MIN,Face.Y_MIN)),
                new Feature(java.util.Set.of(Face.X_MIN,Face.Y_MAX))),tied.possibilities());
    }

    @Test void inputDomainAndWorkLimitFailClosed() {
        ContactResult far = CapsuleAabbContact3d.query(point(0x1.0p26,0,0,1),BOX,ACCURACY,LIMITS);
        assertEquals(Relation.UNRESOLVED,far.relation());
        assertEquals(Reason.UNSUPPORTED_DOMAIN,far.reason().orElseThrow());
        QueryLimits tiny = new QueryLimits(8192,1,128,32,2336,40000,64);
        ContactResult limited = CapsuleAabbContact3d.query(point(-1,0,0,0.5),BOX,ACCURACY,tiny);
        assertEquals(Relation.UNRESOLVED,limited.relation());
        assertEquals(Reason.EXACT_WORK_LIMIT,limited.reason().orElseThrow());
        assertTrue(limited.work().exactAttempts()>limited.work().exactOps());
    }

    @Test void exactShiftAndCandidateBudgetRejectBeforeUnboundedWork() {
        Aabb origin=new Aabb(new Vec3d(0,0,0),new Vec3d(0,0,0));
        QueryLimits bits=new QueryLimits(8,20000,128,32,2336,40000,64);
        ContactResult shifted=CapsuleAabbContact3d.query(
                point(Double.MIN_VALUE,0,0,0),origin,ACCURACY,bits);
        assertEquals(Reason.EXACT_WORK_LIMIT,shifted.reason().orElseThrow());
        assertTrue(shifted.work().exactAttempts()>shifted.work().exactOps());
        assertTrue(shifted.work().peakExactBits()<=bits.maxExactBits());

        QueryLimits candidates=new QueryLimits(8192,20000,128,32,2336,1,64);
        ContactResult exhausted=CapsuleAabbContact3d.query(point(-1,0.5,0.5,0.25),
                BOX,ACCURACY,candidates);
        assertEquals(Reason.EXACT_WORK_LIMIT,exhausted.reason().orElseThrow());
        assertEquals(2,exhausted.work().staticCandidateUnits());
    }

    @Test void impossiblyNarrowClearanceIsUnresolvedRatherThanMisclassified() {
        ContactAccuracy narrow=new ContactAccuracy(1e-20,1e-6,1e-5);
        ContactResult r=CapsuleAabbContact3d.query(point(-1,-1,0.5,0),BOX,narrow,LIMITS);
        assertEquals(Relation.UNRESOLVED,r.relation());
        assertEquals(Reason.ACCURACY_UNREACHABLE,r.reason().orElseThrow());
    }

    @Test void pointOnDegenerateBoxAndZeroRadiusAreExact() {
        Aabb point = new Aabb(new Vec3d(2,2,2),new Vec3d(2,2,2));
        ContactResult r = CapsuleAabbContact3d.query(point(2,2,2,0),point,ACCURACY,LIMITS);
        assertEquals(Relation.TOUCHING,r.relation());
    }

    @Test void degeneratePlaneAndLineBoxesKeepExactRelation() {
        Aabb plane=new Aabb(new Vec3d(0,0,0),new Vec3d(0,1,1));
        Aabb line=new Aabb(new Vec3d(0,0,0),new Vec3d(0,0,1));
        ContactResult face=CapsuleAabbContact3d.query(point(-0.5,0.5,0.5,0.5),
                plane,ACCURACY,LIMITS);
        assertEquals(Relation.TOUCHING,face.relation());
        ContactResult edge=CapsuleAabbContact3d.query(point(-0.5,-0.5,0.5,0.5),
                line,ACCURACY,LIMITS);
        assertEquals(Relation.SEPARATED,edge.relation());
        assertTrue(edge.witness().orElseThrow().feature().possibilities().iterator().next()
                .activeFaces().containsAll(java.util.Set.of(Face.X_MIN,Face.X_MAX,Face.Y_MIN,Face.Y_MAX)));
    }

    @Test void staticQueryRejectsEndpointsOutsideRecenteredProfile() {
        ContactResult r=CapsuleAabbContact3d.query(point(1000,0,0,0.25),BOX,ACCURACY,LIMITS);
        assertEquals(Relation.UNRESOLVED,r.relation());
        assertEquals(Reason.UNSUPPORTED_DOMAIN,r.reason().orElseThrow());
    }

    @Test void independentAxisAlignedIntegerOracle() {
        Random random = new Random(0xC0A7AC7L);
        for (int i=0;i<300;i++) {
            int x0=random.nextInt(97)-48, x1=random.nextInt(97)-48;
            int y=random.nextInt(97)-48, z=random.nextInt(97)-48;
            int radius=random.nextInt(33);
            Capsule c=new Capsule(new Segment3d(new Vec3d(x0/16.0,y/16.0,z/16.0),
                    new Vec3d(x1/16.0,y/16.0,z/16.0)),radius/16.0);
            int dx=gap(Math.min(x0,x1),Math.max(x0,x1),0,16);
            int dy=gap(y,y,0,16), dz=gap(z,z,0,16);
            int compare=Integer.compare(dx*dx+dy*dy+dz*dz,radius*radius);
            Relation expected=compare>0?Relation.SEPARATED:compare<0?Relation.OVERLAPPING:Relation.TOUCHING;
            ContactResult actual=CapsuleAabbContact3d.query(c,BOX,ACCURACY,LIMITS);
            assertEquals(expected,actual.relation(),"case "+i+" "+c+" "+actual);
            assertTrue(actual.work().exactOps()<=LIMITS.maxExactOps());
        }
    }

    @Test void independentArbitraryDirectionRationalCornerOracle() {
        Random random=new Random(0xC0A7D1ECL);
        for(int i=0;i<240;i++) {
            int gx=1+random.nextInt(24),gy=1+random.nextInt(24),gz=1+random.nextInt(24);
            boolean xp=random.nextBoolean(),yp=random.nextBoolean(),zp=random.nextBoolean();
            int ex=xp?16+gx:-gx,ey=yp?16+gy:-gy,ez=zp?16+gz:-gz;
            int sx=ex+(xp?1:-1)*(1+random.nextInt(32));
            int sy=ey+(yp?1:-1)*(1+random.nextInt(32));
            int sz=ez+(zp?1:-1)*(1+random.nextInt(32));
            int radius=random.nextInt(40);
            Capsule c=new Capsule(new Segment3d(new Vec3d(sx/16.0,sy/16.0,sz/16.0),
                    new Vec3d(ex/16.0,ey/16.0,ez/16.0)),radius/16.0);
            // Each coordinate remains outside its chosen side of the box and
            // approaches it monotonically, so the exact nearest pair is the
            // second endpoint and the corresponding box corner.
            int squared=gx*gx+gy*gy+gz*gz;
            Relation expected=squared>radius*radius?Relation.SEPARATED:
                    squared<radius*radius?Relation.OVERLAPPING:Relation.TOUCHING;
            ContactResult result=CapsuleAabbContact3d.query(c,BOX,ACCURACY,LIMITS);
            assertEquals(expected,result.relation(),"direction case "+i+" "+result);
            assertTrue(result.witness().orElseThrow().feature().complete());
        }
    }

    @Test void independentExactRationalOracleCoversSlabCrossingAndInteriorStationaryPoint() {
        // At u=1/3 this oblique segment is (0,5/12,1/2), inside the box, so d=0.
        Segment3d crossingLine=new Segment3d(new Vec3d(-1,0.25,0.5),
                new Vec3d(2,0.75,0.5));
        Capsule crossing=new Capsule(crossingLine,0);
        ContactResult crossed=assertExactReference(crossing,BOX,rational(0,1));
        assertWitnessInSlab(crossed.witness().orElseThrow());

        // In the corner feature region, squared distance is x(u)^2+y(u)^2.
        // Its unique stationary point is u=1/3 at (-1/2,-3/8,1/2), giving d=5/8.
        Segment3d cornerMinimum=new Segment3d(new Vec3d(-7.0/8,1.0/8,0.5),
                new Vec3d(1.0/4,-11.0/8,0.5));
        assertRadiusNeighbors(cornerMinimum,BOX,rational(5,8),5.0/8,witness ->
                assertWitnessAt(witness,rational(1,3),
                        new Rational[]{rational(-1,2),rational(-3,8),rational(1,2)},
                        new Rational[]{rational(0,1),rational(0,1),rational(1,2)}));
    }

    @Test void independentExactRationalOracleCoversBothCapsAndCylinderSideAtAdjacentRadii() {
        // The nearest endpoint of each segment is (±3/4 outside, y=2), at distance 5/4.
        Segment3d endCap=new Segment3d(new Vec3d(-2,2,0.5),new Vec3d(-3.0/4,2,0.5));
        assertRadiusNeighbors(endCap,BOX,rational(5,4),5.0/4,witness ->
                assertWitnessAt(witness,rational(1,1),
                        new Rational[]{rational(-3,4),rational(2,1),rational(1,2)},
                        new Rational[]{rational(0,1),rational(1,1),rational(1,2)}));

        Segment3d startCap=new Segment3d(new Vec3d(7.0/4,2,0.5),new Vec3d(3,2,0.5));
        assertRadiusNeighbors(startCap,BOX,rational(5,4),5.0/4,witness ->
                assertWitnessAt(witness,rational(0,1),
                        new Rational[]{rational(7,4),rational(2,1),rational(1,2)},
                        new Rational[]{rational(1,1),rational(1,1),rational(1,2)}));

        // For 1/3<=u<=1/2, x,z lie in the box footprint and y=2; the side distance is 1.
        Segment3d cylinderSide=new Segment3d(new Vec3d(-1,2,-1),new Vec3d(2,2,3));
        assertRadiusNeighbors(cylinderSide,BOX,rational(1,1),1,witness ->
                assertWitnessOnSlabMinimumSet(witness,rational(1,3),rational(1,2)));
    }

    @Test void subnormalAndAdjacentWorldCoordinatePredicatesStayExact() {
        Aabb origin=new Aabb(new Vec3d(0,0,0),new Vec3d(0,0,0));
        ContactResult subnormal=CapsuleAabbContact3d.query(
                point(Double.MIN_VALUE,0,0,0),origin,ACCURACY,LIMITS);
        assertEquals(Relation.SEPARATED,subnormal.relation(),subnormal::toString);
        double world=30_000_000.0;
        double adjacent=Math.nextUp(world);
        Aabb remote=new Aabb(new Vec3d(world+1,0,0),new Vec3d(world+2,1,1));
        ContactResult close=CapsuleAabbContact3d.query(point(adjacent,0.5,0.5,1),
                remote,ACCURACY,LIMITS);
        assertEquals(Relation.OVERLAPPING,close.relation(),close::toString);
        ContactResult touching=CapsuleAabbContact3d.query(point(world,0.5,0.5,1),
                remote,ACCURACY,LIMITS);
        assertEquals(Relation.TOUCHING,touching.relation(),touching::toString);
    }

    @Test void normalizedDyadicChargingPreservesPublicResultsAndExactBudgetThresholds(TestReporter reporter) {
        Aabb origin=new Aabb(new Vec3d(0,0,0),new Vec3d(0,0,0));
        assertNormalizedBudgetCase("zero",point(0,0,0,0),BOX,rational(0,1),Relation.TOUCHING,
                legacyWork(752,53,5),false,reporter);
        assertNormalizedBudgetCase("d-one-negative-integer",point(-1,0.5,0.5,1),BOX,rational(1,1),
                Relation.TOUCHING,legacyWork(1034,53,5),true,reporter);
        assertNormalizedBudgetCase("negative-odd-over-power-of-two",
                point(-0.75,0.5,0.5,0.25),BOX,rational(3,4),Relation.SEPARATED,
                legacyWork(1044,53,5),true,reporter);
        Capsule negativeEvenShift=new Capsule(new Segment3d(new Vec3d(-0.5,0.5,0.5),
                new Vec3d(-1.5,1.5,0.5)),0.5);
        assertNormalizedBudgetCase("negative-even-over-power-of-two",negativeEvenShift,BOX,
                rational(1,2),Relation.TOUCHING,legacyWork(1325,53,5),false,reporter);
        Capsule positiveEvenShift=new Capsule(new Segment3d(new Vec3d(0.5,2,0.5),
                new Vec3d(1.5,2,0.5)),1);
        assertNormalizedBudgetCase("positive-even-over-power-of-two",positiveEvenShift,BOX,
                rational(1,1),Relation.TOUCHING,legacyWork(1364,53,6),false,reporter);
        assertNormalizedBudgetCase("subnormal-derived",point(Double.MIN_VALUE,0,0,0),origin,
                Rational.exact(Double.MIN_VALUE),Relation.SEPARATED,legacyWork(925,2167,5),false,reporter);
        Capsule nonDyadicStationary=new Capsule(new Segment3d(new Vec3d(-7.0/8,1.0/8,0.5),
                new Vec3d(1.0/4,-11.0/8,0.5)),0);
        assertNormalizedBudgetCase("non-dyadic-stationary",nonDyadicStationary,BOX,
                rational(5,8),Relation.SEPARATED,legacyWork(1622,55,7),false,reporter);

        final int probeOnlyR9ExactOps=1555;
        final int correctedR9ExactOps=1591;
        final int nonDyadicNormalizationCalls=36;
        final int chargedDenominatorProbes=160;
        ContactResult nonDyadic=CapsuleAabbContact3d.query(nonDyadicStationary,BOX,ACCURACY,LIMITS);
        Work nonDyadicWork=nonDyadic.work();
        assertEquals(Relation.SEPARATED,nonDyadic.relation(),nonDyadic::toString);
        assertContains(nonDyadic.witness().orElseThrow().segmentParameter(),rational(1,3));
        assertEquals(1622,legacyWork(1622,55,7).exactOps(),"frozen r7 public-query baseline");
        assertEquals(correctedR9ExactOps,nonDyadicWork.exactOps(),nonDyadic::toString);
        assertEquals(correctedR9ExactOps,nonDyadicWork.exactAttempts(),nonDyadic::toString);
        assertEquals(probeOnlyR9ExactOps+nonDyadicNormalizationCalls,correctedR9ExactOps,
                "the frozen probe-only r9 result plus one gcd charge for each independently counted non-dyadic branch");
        assertEquals(36,nonDyadicNormalizationCalls,"scratch-only branch-count receipt");
        assertEquals(160,chargedDenominatorProbes,"scratch-only denominator-probe receipt");

        ContactResult atProbeOnlyCap=CapsuleAabbContact3d.query(nonDyadicStationary,BOX,ACCURACY,
                limitsWithExactOps(probeOnlyR9ExactOps));
        assertEquals(Relation.UNRESOLVED,atProbeOnlyCap.relation(),atProbeOnlyCap::toString);
        assertEquals(Reason.EXACT_WORK_LIMIT,atProbeOnlyCap.reason().orElseThrow(),atProbeOnlyCap::toString);
        assertEquals(probeOnlyR9ExactOps,atProbeOnlyCap.work().exactOps(),atProbeOnlyCap::toString);
        assertEquals(probeOnlyR9ExactOps+1,atProbeOnlyCap.work().exactAttempts(),atProbeOnlyCap::toString);

        ContactResult laterBitCap=CapsuleAabbContact3d.query(nonDyadicStationary,BOX,ACCURACY,
                limitsWithExactBits(54));
        assertEquals(Relation.UNRESOLVED,laterBitCap.relation(),laterBitCap::toString);
        assertEquals(Reason.EXACT_WORK_LIMIT,laterBitCap.reason().orElseThrow(),laterBitCap::toString);
        assertEquals(1555,laterBitCap.work().exactOps(),laterBitCap::toString);
        assertEquals(1556,laterBitCap.work().exactAttempts(),laterBitCap::toString);
        assertEquals(53,laterBitCap.work().peakExactBits(),laterBitCap::toString);
        assertEquals(55,nonDyadicWork.peakExactBits(),nonDyadic::toString);
        String chargeReceipt="fixture=non-dyadic-stationary witnessParameter=1/3 r7ExactOps=1622"
                +" probeOnlyR9ExactOps="+probeOnlyR9ExactOps+" correctedR9ExactOps="+nonDyadicWork.exactOps()
                +" scratchDenominatorProbes="+chargedDenominatorProbes
                +" scratchNonDyadicBranches="+nonDyadicNormalizationCalls
                +" scratchGcdCharges=36 scratchNumeratorDivideCharges=36 scratchDenominatorDivideCharges=36"
                +" probeOnlyCap="+atProbeOnlyCap.work()+" laterBitCap54="+laterBitCap.work();
        reporter.publishEntry("R9_NON_DYADIC_CHARGING",chargeReceipt);
        System.out.println("R9_NON_DYADIC_CHARGING "+chargeReceipt);
    }

    private static void assertNormalizedBudgetCase(String name,Capsule capsule,Aabb box,Rational distance,
                                                   Relation expected,Work r7Work,boolean requireSavings,
                                                   TestReporter reporter) {
        ContactResult normal=assertExactReference(capsule,box,distance);
        assertEquals(expected,normal.relation(),normal::toString);
        Work work=normal.work();
        assertEquals(r7Work.staticAttempts(),work.staticAttempts(),name);
        assertEquals(r7Work.staticEvaluations(),work.staticEvaluations(),name);
        assertEquals(r7Work.staticCandidateUnits(),work.staticCandidateUnits(),name);
        assertEquals(r7Work.castAttempts(),work.castAttempts(),name);
        assertEquals(r7Work.castQueries(),work.castQueries(),name);
        assertEquals(r7Work.castEvaluations(),work.castEvaluations(),name);
        assertEquals(r7Work.castCandidateUnits(),work.castCandidateUnits(),name);
        assertEquals(r7Work.totalCandidateUnits(),work.totalCandidateUnits(),name);
        assertEquals(r7Work.refinementSteps(),work.refinementSteps(),name);
        assertEquals(work.exactOps(),work.exactAttempts(),name+" successful query has no denied exact attempt");
        assertTrue(work.exactOps()<=r7Work.exactOps(),name+" exact work must not exceed r7: "+work);
        assertTrue(work.peakExactBits()<=r7Work.peakExactBits(),name+" exact bit peak must not exceed r7: "+work);
        if(requireSavings) {
            assertTrue(work.exactOps()<r7Work.exactOps(),name+" must exercise the dyadic work reduction: "+work);
        }

        int exactOps=work.exactOps();
        assertTrue(exactOps>1,name+" exact-op cap threshold needs a positive interior");
        ContactResult below=CapsuleAabbContact3d.query(capsule,box,ACCURACY,
                limitsWithExactOps(exactOps-1));
        assertEquals(Relation.UNRESOLVED,below.relation(),below::toString);
        assertEquals(Reason.EXACT_WORK_LIMIT,below.reason().orElseThrow(),below::toString);
        assertEquals(exactOps-1,below.work().exactOps(),below::toString);
        assertEquals(exactOps,below.work().exactAttempts(),below::toString);
        assertTrue(below.work().peakExactBits()<=LIMITS.maxExactBits(),below::toString);

        ContactResult at=CapsuleAabbContact3d.query(capsule,box,ACCURACY,limitsWithExactOps(exactOps));
        ContactResult above=CapsuleAabbContact3d.query(capsule,box,ACCURACY,limitsWithExactOps(exactOps+1));
        assertEquals(normal,at,name+" must succeed at exact-op N");
        assertEquals(normal,above,name+" must succeed at exact-op N+1");
        String receipt="case="+name+" expected="+expected+" clearance="+normal.signedClearanceMeters()
                +" witness="+normal.witness()+" r7Work="+r7Work+" currentWork="+work
                +" N-1="+below.work()+" N="+at.work()+" N+1="+above.work();
        reporter.publishEntry("R9_NORMALIZED_PUBLIC",receipt);
        System.out.println("R9_NORMALIZED_PUBLIC "+receipt);
    }

    private static QueryLimits limitsWithExactOps(int exactOps) {
        return new QueryLimits(LIMITS.maxExactBits(),exactOps,LIMITS.maxStaticEvaluations(),
                LIMITS.maxCastQueries(),LIMITS.maxCastEvaluations(),LIMITS.maxCandidateUnits(),
                LIMITS.maxRefinementSteps());
    }

    private static QueryLimits limitsWithExactBits(int exactBits) {
        return new QueryLimits(exactBits,LIMITS.maxExactOps(),LIMITS.maxStaticEvaluations(),
                LIMITS.maxCastQueries(),LIMITS.maxCastEvaluations(),LIMITS.maxCandidateUnits(),
                LIMITS.maxRefinementSteps());
    }

    private static Work legacyWork(int exactOps,int peakExactBits,int staticCandidateUnits) {
        return new Work(exactOps,exactOps,peakExactBits,1,1,staticCandidateUnits,
                0,0,0,0,staticCandidateUnits,0);
    }
    private static ContactResult assertExactReference(Capsule capsule,Aabb box,Rational distance) {
        Rational clearance=distance.subtract(Rational.exact(capsule.radius()));
        Relation expected=clearance.signum()>0?Relation.SEPARATED:
                clearance.signum()<0?Relation.OVERLAPPING:Relation.TOUCHING;
        ContactResult actual=CapsuleAabbContact3d.query(capsule,box,ACCURACY,LIMITS);
        assertEquals(expected,actual.relation(),actual::toString);
        Interval interval=actual.signedClearanceMeters().orElseThrow();
        assertTrue(Rational.exact(interval.lower()).compareTo(clearance)<=0,actual::toString);
        assertTrue(Rational.exact(interval.upper()).compareTo(clearance)>=0,actual::toString);
        assertTrue(interval.upper()-interval.lower()<=ACCURACY.maxClearanceWidthMeters(),actual::toString);
        assertTrue(actual.witness().isPresent(),actual::toString);
        return actual;
    }

    private static void assertRadiusNeighbors(Segment3d centerline,Aabb box,Rational distance,
                                              double tangentRadius,WitnessVerifier witnessVerifier) {
        for(double radius:new double[]{Math.nextDown(tangentRadius),tangentRadius,Math.nextUp(tangentRadius)}) {
            Capsule capsule=new Capsule(centerline,radius);
            ContactResult actual=assertExactReference(capsule,box,distance);
            witnessVerifier.verify(actual.witness().orElseThrow());
        }
    }

    private static void assertWitnessAt(Witness witness,Rational parameter,
                                        Rational[] segment,Rational[] box) {
        assertContains(witness.segmentParameter(),parameter);
        Interval[] segmentIntervals={witness.segmentX(),witness.segmentY(),witness.segmentZ()};
        Interval[] boxIntervals={witness.boxX(),witness.boxY(),witness.boxZ()};
        for(int axis=0;axis<3;axis++) {
            assertContains(segmentIntervals[axis],segment[axis]);
            assertContains(boxIntervals[axis],box[axis]);
        }
    }

    private static void assertWitnessInSlab(Witness witness) {
        assertIntervalIntersects(witness.segmentParameter(),rational(1,3),rational(2,3));
        assertWitnessCoordinatesCoincideInsideBox(witness,witness.segmentX(),witness.boxX(),
                rational(0,1),rational(1,1));
        assertWitnessCoordinatesCoincideInsideBox(witness,witness.segmentY(),witness.boxY(),
                rational(0,1),rational(1,1));
        assertContains(witness.segmentZ(),rational(1,2));
        assertContains(witness.boxZ(),rational(1,2));
    }

    private static void assertWitnessOnSlabMinimumSet(Witness witness,Rational first,Rational last) {
        assertIntervalIntersects(witness.segmentParameter(),first,last);
        assertContains(witness.segmentY(),rational(2,1));
        assertContains(witness.boxY(),rational(1,1));
        assertWitnessCoordinatesCoincideInsideBox(witness,witness.segmentX(),witness.boxX(),
                rational(0,1),rational(1,1));
        assertWitnessCoordinatesCoincideInsideBox(witness,witness.segmentZ(),witness.boxZ(),
                rational(0,1),rational(1,1));
    }

    private static void assertWitnessCoordinatesCoincideInsideBox(Witness witness,
            Interval segmentCoordinate,Interval boxCoordinate,Rational lower,Rational upper) {
        assertWithin(segmentCoordinate,lower,upper);
        assertWithin(boxCoordinate,lower,upper);
        Rational segmentLower=Rational.exact(segmentCoordinate.lower());
        Rational segmentUpper=Rational.exact(segmentCoordinate.upper());
        Rational boxLower=Rational.exact(boxCoordinate.lower());
        Rational boxUpper=Rational.exact(boxCoordinate.upper());
        assertTrue(segmentLower.compareTo(boxUpper)<=0 && boxLower.compareTo(segmentUpper)<=0,
                ()->"witness intervals do not contain a common closest coordinate: "+witness);
    }

    private static void assertIntervalIntersects(Interval interval,Rational rangeLow,Rational rangeHigh) {
        Rational low=Rational.exact(interval.lower()),high=Rational.exact(interval.upper());
        assertTrue(low.compareTo(rangeHigh)<=0 && rangeLow.compareTo(high)<=0,
                ()->"witness parameter misses minimizer range: "+interval);
    }

    private static void assertWithin(Interval interval,Rational low,Rational high) {
        assertTrue(Rational.exact(interval.lower()).compareTo(low)>=0
                        && Rational.exact(interval.upper()).compareTo(high)<=0,
                ()->"witness coordinate is outside the box range: "+interval);
    }

    private static void assertContains(Interval interval,Rational expected) {
        assertTrue(Rational.exact(interval.lower()).compareTo(expected)<=0
                        && expected.compareTo(Rational.exact(interval.upper()))<=0,
                ()->"witness interval does not contain "+expected+": "+interval);
    }

    private static Rational rational(long numerator,long denominator) {
        return new Rational(BigInteger.valueOf(numerator),BigInteger.valueOf(denominator));
    }

    @FunctionalInterface
    private interface WitnessVerifier { void verify(Witness witness); }

    /** Test-only exact rational arithmetic; it does not call contact implementation helpers. */
    private record Rational(BigInteger numerator,BigInteger denominator) implements Comparable<Rational> {
        private static final Rational ZERO=new Rational(BigInteger.ZERO,BigInteger.ONE);

        private Rational {
            if(denominator.signum()==0) throw new IllegalArgumentException("zero denominator");
            if(denominator.signum()<0) {
                numerator=numerator.negate();
                denominator=denominator.negate();
            }
            BigInteger gcd=numerator.gcd(denominator);
            numerator=numerator.divide(gcd);
            denominator=denominator.divide(gcd);
        }

        static Rational exact(double value) {
            if(!Double.isFinite(value)) throw new IllegalArgumentException("nonfinite binary64");
            long bits=Double.doubleToRawLongBits(value);
            boolean negative=(bits>>>63)!=0;
            int exponent=(int)((bits>>>52)&0x7ffL);
            long significand=bits&0x000f_ffff_ffff_ffffL;
            if(exponent!=0) significand|=1L<<52;
            if(significand==0) return ZERO;
            int shift=exponent==0?-1074:exponent-1075;
            BigInteger numerator=BigInteger.valueOf(significand);
            BigInteger denominator=BigInteger.ONE;
            if(shift>0) numerator=numerator.shiftLeft(shift);
            else if(shift<0) denominator=denominator.shiftLeft(-shift);
            if(negative) numerator=numerator.negate();
            return new Rational(numerator,denominator);
        }

        Rational subtract(Rational other) {
            return new Rational(numerator.multiply(other.denominator)
                    .subtract(other.numerator.multiply(denominator)),denominator.multiply(other.denominator));
        }

        int signum() { return numerator.signum(); }

        @Override public int compareTo(Rational other) {
            return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
        }

        @Override public String toString() { return numerator+"/"+denominator; }
    }

    private static int gap(int intervalMin,int intervalMax,int boxMin,int boxMax) {
        return intervalMax<boxMin?boxMin-intervalMax:intervalMin>boxMax?intervalMin-boxMax:0;
    }

    private static Capsule point(double x,double y,double z,double radius) {
        Vec3d p = new Vec3d(x,y,z);
        return new Capsule(new Segment3d(p,p),radius);
    }
    private static void assertContact(Capsule capsule,Relation relation,double expected) {
        ContactResult r = CapsuleAabbContact3d.query(capsule,BOX,ACCURACY,LIMITS);
        assertEquals(relation,r.relation(),r::toString);
        Interval enclosure = r.signedClearanceMeters().orElseThrow();
        assertTrue(enclosure.lower()<=expected && expected<=enclosure.upper(),r::toString);
        assertTrue(enclosure.upper()-enclosure.lower()<=ACCURACY.maxClearanceWidthMeters());
        assertTrue(r.witness().isPresent(),r::toString);
    }
}
