package dev.crlhitbox.api.geometry.contact;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Vec3d;
import dev.crlhitbox.api.geometry.contact.CapsuleAabbContact3d.*;
import dev.crlhitbox.api.geometry.cast.CapsuleAabbCast3d;
import dev.crlhitbox.api.geometry.cast.CapsuleAabbCastResult3d;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Bounded exact arithmetic for finite binary64 capsule/box queries. */
final class CapsuleAabbContactMath {
    private static final double COORD_LIMIT = 0x1.0p25;
    private CapsuleAabbContactMath() { }

    private static int magnitudeBits(BigInteger value) {
        int bits=value.bitLength();
        return value.signum()<0 && value.getLowestSetBit()==bits ? bits+1:bits;
    }

    static ContactResult query(Capsule capsule, Aabb box, ContactAccuracy accuracy, QueryLimits limits) {
        Budget budget = new Budget(limits);
        return query(capsule, box, accuracy, budget);
    }

    private static ContactResult query(Capsule capsule, Aabb box, ContactAccuracy accuracy, Budget budget) {
        budget.staticAttempts++;
        try {
            if (!domain(capsule, box, budget)) return unresolved(Reason.UNSUPPORTED_DOMAIN, budget);
            Q[] a = coordinates(capsule.centerline().start(), budget);
            Q[] b = coordinates(capsule.centerline().end(), budget);
            Q[] lo = coordinates(box.min(), budget);
            Q[] hi = coordinates(box.max(), budget);
            return queryExact(a,b,lo,hi,capsule.radius(),accuracy,budget,true);
        } catch (Limit ex) {
            return unresolved(Reason.EXACT_WORK_LIMIT, budget);
        } catch (ArithmeticException ex) {
            return unresolved(Reason.NUMERICAL_ENCLOSURE, budget);
        }
    }

    private static ContactResult queryExact(Q[] a,Q[] b,Q[] lo,Q[] hi,double radiusValue,
                                            ContactAccuracy accuracy,Budget budget,boolean publicWitness) {
            budget.staticEvaluations++;
            if (!budget.limits.recordsOnly()
                    && budget.staticEvaluations > budget.limits.maxStaticEvaluations()) throw new Limit();
            Q radius = Q.of(radiusValue, budget);
            List<Q> cuts = new ArrayList<>();
            Q[] slopes = new Q[3];
            cuts.add(Q.ZERO); cuts.add(Q.ONE);
            for (int axis = 0; axis < 3; axis++) {
                Q motion = b[axis].sub(a[axis], budget);
                slopes[axis] = motion;
                if (motion.signum() == 0) continue;
                for (Q plane : new Q[] { lo[axis], hi[axis] }) {
                    Q t = plane.sub(a[axis], budget).div(motion, budget);
                    if (t.compareTo(Q.ZERO, budget) > 0 && t.compareTo(Q.ONE, budget) < 0) cuts.add(t);
                }
            }
            cuts.sort((first, second) -> first.compareTo(second, budget));
            List<Q> unique = new ArrayList<>();
            for (Q cut : cuts) if (unique.isEmpty() || !unique.getLast().equals(cut)) unique.add(cut);
            Candidate best = null;
            List<Candidate> candidates=publicWitness?new ArrayList<>():null;
            for (Q cut : unique) {
                Candidate candidate=evaluate(cut,a,b,lo,hi,slopes,budget);
                if(publicWitness) candidates.add(candidate);
                best=choose(best,candidate,budget);
            }
            for (int i = 0; i + 1 < unique.size(); i++) {
                Q left = unique.get(i), right = unique.get(i + 1);
                Q mid = left.add(right, budget).div(Q.TWO, budget);
                Q numerator = Q.ZERO, denominator = Q.ZERO;
                for (int axis = 0; axis < 3; axis++) {
                    Q slope = slopes[axis];
                    Q atMid = a[axis].add(slope.mul(mid, budget), budget);
                    Q target = atMid.compareTo(lo[axis], budget) < 0 ? lo[axis]
                            : atMid.compareTo(hi[axis], budget) > 0 ? hi[axis] : null;
                    if (target != null) {
                        Q offset = a[axis].sub(target, budget);
                        numerator = numerator.sub(slope.mul(offset, budget), budget);
                        denominator = denominator.add(slope.mul(slope, budget), budget);
                    }
                }
                if (denominator.signum() > 0) {
                    Q stationary = numerator.div(denominator, budget);
                    if (stationary.compareTo(left, budget) > 0 && stationary.compareTo(right, budget) < 0)
                    {
                        Candidate candidate=evaluate(stationary,a,b,lo,hi,slopes,budget);
                        if(publicWitness) candidates.add(candidate);
                        best=choose(best,candidate,budget);
                    }
                }
            }
            Q radiusSquared = radius.mul(radius, budget);
            int sign = best.distanceSquared.compareTo(radiusSquared, budget);
            budget.lastCandidate=best;
            Relation relation = sign > 0 ? Relation.SEPARATED : sign < 0 ? Relation.OVERLAPPING : Relation.TOUCHING;
            Interval clearance = clearance(best.distanceSquared, radiusValue, accuracy, budget);
            if (clearance == null) return unresolved(Reason.ACCURACY_UNREACHABLE, budget);
            Optional<Witness> witness=Optional.empty();
            if(publicWitness) {
                FeatureAttribution features=features(best,candidates,unique,a,b,lo,hi,budget);
                witness=witness(best,features,accuracy,budget);
                if (witness.isEmpty()) return unresolved(Reason.ACCURACY_UNREACHABLE,budget);
            }
            return new ContactResult(relation, Optional.of(clearance), witness, Optional.empty(),
                    Optional.empty(), budget.snapshot());
    }

    private static ContactResult queryAt(Capsule capsule,Vec3d delta,double fraction,Aabb box,
                                         ContactAccuracy accuracy,Budget budget) {
        budget.staticAttempts++;
        try {
            Q parameter=Q.of(fraction,budget),limit=Q.of(COORD_LIMIT,budget);
            return queryAtExact(capsule,delta,parameter,box,accuracy,budget,limit);
        } catch(Limit ex) { return unresolved(Reason.EXACT_WORK_LIMIT,budget); }
        catch(ArithmeticException ex) { return unresolved(Reason.NUMERICAL_ENCLOSURE,budget); }
    }

    private static ContactResult queryAt(Capsule capsule,Vec3d delta,Q parameter,Aabb box,
                                         ContactAccuracy accuracy,Budget budget) {
        budget.staticAttempts++;
        try {
            Q limit=Q.of(COORD_LIMIT,budget);
            if(parameter.compareTo(Q.ZERO,budget)<0 || parameter.compareTo(Q.ONE,budget)>0)
                return unresolved(Reason.UNSUPPORTED_DOMAIN,budget);
            return queryAtExact(capsule,delta,parameter,box,accuracy,budget,limit);
        } catch(Limit ex) { return unresolved(Reason.EXACT_WORK_LIMIT,budget); }
        catch(ArithmeticException ex) { return unresolved(Reason.NUMERICAL_ENCLOSURE,budget); }
    }

    private static ContactResult queryAtExact(Capsule capsule,Vec3d delta,Q parameter,Aabb box,
                                               ContactAccuracy accuracy,Budget budget,Q limit) {
        Q[] translation=coordinates(delta,budget),a=coordinates(capsule.centerline().start(),budget),
                b=coordinates(capsule.centerline().end(),budget);
        try {
            for(int axis=0;axis<3;axis++) {
                Q offset=translation[axis].mul(parameter,budget);
                a[axis]=a[axis].add(offset,budget); b[axis]=b[axis].add(offset,budget);
                if(a[axis].compareTo(limit,budget)>0 || a[axis].compareTo(limit.negate(budget),budget)<0
                        || b[axis].compareTo(limit,budget)>0 || b[axis].compareTo(limit.negate(budget),budget)<0)
                    return unresolved(Reason.UNSUPPORTED_DOMAIN,budget);
            }
            return queryExact(a,b,coordinates(box.min(),budget),coordinates(box.max(),budget),
                    capsule.radius(),accuracy,budget,false);
        } catch(Limit ex) { return unresolved(Reason.EXACT_WORK_LIMIT,budget); }
        catch(ArithmeticException ex) { return unresolved(Reason.NUMERICAL_ENCLOSURE,budget); }
    }

    private static ContactResult unresolved(Reason reason, Budget budget) {
        return new ContactResult(Relation.UNRESOLVED, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(reason), budget.snapshot());
    }

    private static Q[] coordinates(Vec3d v, Budget b) {
        return new Q[] { Q.of(v.x(), b), Q.of(v.y(), b), Q.of(v.z(), b) };
    }

    private static boolean domain(Capsule capsule, Aabb box, Budget budget) {
        Vec3d a = capsule.centerline().start(), b = capsule.centerline().end();
        Vec3d lo = box.min(), hi = box.max();
        for (Vec3d p : new Vec3d[] {a,b,lo,hi})
            if (Math.abs(p.x()) > COORD_LIMIT || Math.abs(p.y()) > COORD_LIMIT
                    || Math.abs(p.z()) > COORD_LIMIT) return false;
        if (capsule.radius() > 64) return false;
        Q[] first = coordinates(a, budget), last = coordinates(b, budget);
        Q lengthSquared = Q.ZERO;
        for (int axis = 0; axis < 3; axis++) {
            Q difference = last[axis].sub(first[axis], budget);
            lengthSquared = lengthSquared.add(difference.square(budget), budget);
        }
        if (lengthSquared.compareTo(Q.of(16384, budget), budget) > 0) return false;
        Q[] low = coordinates(lo, budget), high = coordinates(hi, budget);
        for (int axis = 0; axis < 3; axis++)
            if (high[axis].sub(low[axis], budget).compareTo(Q.of(128,budget),budget) > 0) return false;
        Q localLimit=Q.of(512*512,budget);
        for(Q[] endpoint:new Q[][]{first,last}) {
            Q recenteredSquared=Q.ZERO;
            for(int axis=0;axis<3;axis++) {
                Q component=endpoint[axis].sub(low[axis],budget);
                recenteredSquared=recenteredSquared.add(component.square(budget),budget);
            }
            if(recenteredSquared.compareTo(localLimit,budget)>0) return false;
        }
        return true;
    }

    private record Candidate(Q t, Q[] segment, Q[] box, Q distanceSquared) { }
    private record BridgeProof(Reason reason,boolean nonpenetrating) { }
    private record StationaryResult(Relation relation,Reason reason) { }
    private static Candidate evaluate(Q t, Q[] a, Q[] b, Q[] lo, Q[] hi, Budget budget) {
        return evaluate(t,a,b,lo,hi,null,budget);
    }
    private static Candidate evaluate(Q t, Q[] a, Q[] b, Q[] lo, Q[] hi,
                                      Q[] slopes, Budget budget) {
        budget.staticCandidateUnits++;
        if (!budget.limits.recordsOnly()
                && budget.staticCandidateUnits > budget.limits.maxCandidateUnits()) throw new Limit();
        Q[] p = new Q[3], q = new Q[3]; Q sum = Q.ZERO;
        for (int axis = 0; axis < 3; axis++) {
            Q slope=slopes==null?b[axis].sub(a[axis],budget):slopes[axis];
            p[axis] = a[axis].add(slope.mul(t, budget), budget);
            q[axis] = p[axis].compareTo(lo[axis], budget) < 0 ? lo[axis]
                    : p[axis].compareTo(hi[axis], budget) > 0 ? hi[axis] : p[axis];
            Q residual = p[axis].sub(q[axis], budget);
            sum = sum.add(residual.mul(residual, budget), budget);
        }
        return new Candidate(t, p, q, sum);
    }
    private static Candidate choose(Candidate old, Candidate next, Budget b) {
        return old == null || next.distanceSquared.compareTo(old.distanceSquared, b) < 0 ? next : old;
    }

    private static Interval clearance(Q distanceSquared, double radius, ContactAccuracy accuracy, Budget b) {
        if (distanceSquared.signum() == 0) return new Interval(-radius, -radius);
        double approximate = new BigDecimal(distanceSquared.n())
                .divide(new BigDecimal(distanceSquared.d()), MathContext.DECIMAL128)
                .sqrt(MathContext.DECIMAL128).doubleValue();
        if (!Double.isFinite(approximate)) return null;
        double lower = approximate, upper = approximate;
        for (int i = 0; i < 16 && Q.of(lower, b).square(b).compareTo(distanceSquared, b) > 0; i++)
            lower = Math.nextDown(lower);
        for (int i = 0; i < 16 && Q.of(upper, b).square(b).compareTo(distanceSquared, b) < 0; i++)
            upper = Math.nextUp(upper);
        if (Q.of(lower, b).square(b).compareTo(distanceSquared, b) > 0
                || Q.of(upper, b).square(b).compareTo(distanceSquared, b) < 0) return null;
        double signedLower = Math.nextDown(lower-radius), signedUpper = Math.nextUp(upper-radius);
        if (!Double.isFinite(signedLower) || !Double.isFinite(signedUpper)
                || signedUpper-signedLower > accuracy.maxClearanceWidthMeters()) return null;
        return new Interval(signedLower, signedUpper);
    }

    private static Optional<Witness> witness(Candidate best, FeatureAttribution feature,
                                             ContactAccuracy accuracy, Budget b) {
        Interval t = enclosure(best.t, b);
        Interval[] p = new Interval[3], q = new Interval[3];
        for (int i = 0; i < 3; i++) {
            p[i] = enclosure(best.segment[i], b); q[i] = enclosure(best.box[i], b);
            double coordinateAllowance = accuracy.maxWitnessErrorMeters()/Math.sqrt(3);
            if (p[i].upper()-p[i].lower() > coordinateAllowance
                    || q[i].upper()-q[i].lower() > coordinateAllowance) return Optional.empty();
        }
        return Optional.of(new Witness(t, p[0], p[1], p[2], q[0], q[1], q[2], feature));
    }

    private static FeatureAttribution features(Candidate best,List<Candidate> candidates,List<Q> cuts,
            Q[] a,Q[] b,Q[] lo,Q[] hi,Budget budget) {
        Q first=best.t,last=best.t;
        for(Candidate candidate:candidates) if(candidate.distanceSquared.compareTo(best.distanceSquared,budget)==0) {
            if(candidate.t.compareTo(first,budget)<0) first=candidate.t;
            if(candidate.t.compareTo(last,budget)>0) last=candidate.t;
        }
        List<Q> knots=new ArrayList<>();
        knots.add(first);knots.add(last);
        for(Q cut:cuts) if(cut.compareTo(first,budget)>0 && cut.compareTo(last,budget)<0) knots.add(cut);
        knots.sort((left,right)->left.compareTo(right,budget));
        Set<Feature> all=new java.util.HashSet<>();
        for(int i=0;i<knots.size();i++) {
            Q at=knots.get(i);
            all.add(featureAt(evaluate(at,a,b,lo,hi,budget),lo,hi,budget));
            if(i+1<knots.size() && at.compareTo(knots.get(i+1),budget)<0) {
                Q mid=at.add(knots.get(i+1),budget).div(Q.TWO,budget);
                all.add(featureAt(evaluate(mid,a,b,lo,hi,budget),lo,hi,budget));
            }
        }
        return new FeatureAttribution(all,true);
    }

    private static Feature featureAt(Candidate candidate,Q[] lo,Q[] hi,Budget budget) {
        EnumSet<Face> faces=EnumSet.noneOf(Face.class);
        Face[] minima={Face.X_MIN,Face.Y_MIN,Face.Z_MIN};
        Face[] maxima={Face.X_MAX,Face.Y_MAX,Face.Z_MAX};
        for(int axis=0;axis<3;axis++) {
            if(candidate.box[axis].compareTo(lo[axis],budget)==0) faces.add(minima[axis]);
            if(candidate.box[axis].compareTo(hi[axis],budget)==0) faces.add(maxima[axis]);
        }
        return new Feature(faces);
    }
    private static Interval enclosure(Q value, Budget b) {
        double x = value.toDouble();
        if (!Double.isFinite(x)) throw new ArithmeticException("non-finite enclosure");
        int cmp = Q.of(x, b).compareTo(value, b);
        return cmp == 0 ? new Interval(x,x) : cmp < 0 ? new Interval(x, Math.nextUp(x))
                : new Interval(Math.nextDown(x),x);
    }

    static BoundaryResult refine(Capsule capsule, Vec3d delta, Aabb obstacle,
            BoundarySearchHint hint, StoredInstallCandidate installation,
            ContactAccuracy accuracy, QueryLimits limits) {
        Budget budget = new Budget(limits);
        ContactResult start = query(capsule, obstacle, accuracy, budget);
        Candidate startWitness=budget.lastCandidate;
        if (start.relation() == Relation.UNRESOLVED) return unresolvedBoundary(start.relation(),
                start.reason().orElseThrow(), budget);
        if (start.relation() == Relation.OVERLAPPING)
            return new BoundaryResult(BoundaryKind.INITIAL_OVERLAP, Relation.OVERLAPPING,
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), budget.snapshot());
        try {
            if (!translationDomain(capsule,delta,obstacle,budget))
                return unresolvedBoundary(start.relation(),Reason.UNSUPPORTED_DOMAIN,budget);
            boolean stationary=delta.x()==0 && delta.y()==0 && delta.z()==0;
            boolean zeroRadius=capsule.radius()==0;
            boolean touchingNonpenetrating=zeroRadius && start.relation()==Relation.TOUCHING;
            boolean endTouchNonpenetrating=false;
            boolean interiorNonpenetrating=false;
            Candidate endWitness=null;
            Relation endRelation=null;
            if(start.relation()==Relation.TOUCHING && !stationary && !zeroRadius) {
                int derivative=contactDirectionalDerivative(startWitness,delta,budget);
                if(derivative<0) return blockedAtStart(capsule,delta,obstacle,installation,accuracy,budget);
                touchingNonpenetrating=true;
            }
            if (start.relation()==Relation.SEPARATED && !stationary) {
                ContactResult end=queryAt(capsule,delta,1.0,obstacle,accuracy,budget);
                endWitness=budget.lastCandidate;
                endRelation=end.relation();
                if (end.relation()==Relation.UNRESOLVED)
                    return unresolvedBoundary(start.relation(),end.reason().orElseThrow(),budget);
                if (start.relation()==Relation.SEPARATED && end.relation()==Relation.OVERLAPPING)
                    return refineOverlap(capsule,delta,obstacle,installation,accuracy,budget);
                if (end.relation()==Relation.TOUCHING) {
                    // Convex clearance has nondecreasing derivative. A nonpositive
                    // derivative at the final exact touch bounds every earlier
                    // clearance below by c(1)=0.
                    endTouchNonpenetrating=zeroRadius
                            || contactDirectionalDerivative(budget.lastCandidate,delta,budget)<=0;
                }
            }
            if (!domain(installation.actualStoredCapsule(),obstacle,budget))
                return unresolvedBoundary(start.relation(),Reason.UNSUPPORTED_DOMAIN,budget);
            if (!validInstallIdentity(capsule,delta,installation,budget))
                return unresolvedBoundary(start.relation(),Reason.ROUNDED_INSTALL_UNSAFE,budget);
            if (installation.fraction()!=1.0)
                return unresolvedBoundary(start.relation(),Reason.NO_PREFIX_CERTIFICATE,budget);
            BridgeProof bridge=bridgeReason(capsule,delta,obstacle,installation,accuracy,budget);
            if (bridge.reason()!=null) return unresolvedBoundary(start.relation(),bridge.reason(),budget);
            boolean strictFullPathProved=false;
            if (!stationary && !touchingNonpenetrating && !endTouchNonpenetrating) {
                budget.castAttempts++;
                if (!limits.recordsOnly() && (budget.castQueries >= limits.maxCastQueries()
                        || budget.castEvaluations+73 > limits.maxCastEvaluations()
                        || budget.staticCandidateUnits+budget.castCandidateUnits+1095 > limits.maxCandidateUnits()))
                    throw new Limit();
                CapsuleAabbCastResult3d cast = CapsuleAabbCast3d.cast(capsule,delta,obstacle);
                budget.castQueries++;
                int evaluations = cast.work().evaluationCount();
                budget.castEvaluations+=evaluations;
                budget.castCandidateUnits+=15*evaluations;
                if (start.relation()==Relation.SEPARATED
                        && cast.kind()==CapsuleAabbCastResult3d.Kind.CLEAR_SEPARATED) {
                    strictFullPathProved=true;
                } else if (zeroRadius && start.relation()==Relation.SEPARATED) {
                    // c(t)=distance(t,C)-0 is nonnegative by definition, even if
                    // the point path enters the closed AABB.
                    interiorNonpenetrating=true;
                } else if (start.relation()==Relation.SEPARATED && endRelation==Relation.SEPARATED) {
                    StationaryResult minimum=stationaryMinimum(capsule,delta,obstacle,accuracy,
                            startWitness,endWitness,budget);
                    if (minimum.relation()==Relation.UNRESOLVED)
                        return unresolvedBoundary(start.relation(),minimum.reason(),budget);
                    if (minimum.relation()==Relation.SEPARATED) strictFullPathProved=true;
                    else if (minimum.relation()==Relation.TOUCHING) interiorNonpenetrating=true;
                    else return unresolvedBoundary(start.relation(),Reason.NO_PREFIX_CERTIFICATE,budget);
                } else {
                    return unresolvedBoundary(start.relation(),Reason.NO_PREFIX_CERTIFICATE,budget);
                }
            }
            ContactResult stored = query(installation.actualStoredCapsule(),obstacle,accuracy,budget);
            if (stored.relation()==Relation.UNRESOLVED)
                return unresolvedBoundary(start.relation(),stored.reason().orElseThrow(),budget);
            if (stored.relation()==Relation.OVERLAPPING)
                return unresolvedBoundary(start.relation(),Reason.CORRECTION_PATH_UNPROVED,budget);
            if (start.relation()==Relation.SEPARATED && !endTouchNonpenetrating && !interiorNonpenetrating
                    && !strictFullPathProved
                    && stored.relation()!=Relation.SEPARATED)
                return unresolvedBoundary(start.relation(),Reason.CORRECTION_PATH_UNPROVED,budget);
            if (stored.relation()!=start.relation() && stationary)
                return unresolvedBoundary(start.relation(),Reason.CORRECTION_PATH_UNPROVED,budget);
            boolean nonpenetratingFull=touchingNonpenetrating || endTouchNonpenetrating
                    || interiorNonpenetrating || bridge.nonpenetrating();
            BoundaryKind kind = start.relation()==Relation.SEPARATED && !nonpenetratingFull
                    ? BoundaryKind.SEPARATED_FULL:BoundaryKind.NONPENETRATING_FULL;
            PrefixKind prefixKind = start.relation()==Relation.SEPARATED && !nonpenetratingFull
                    ? PrefixKind.CLEAR_SEPARATED:PrefixKind.NONPENETRATING;
            InstalledPoseReceipt receipt = new InstalledPoseReceipt(1.0,
                    installation.acceptedStoredCenter(),installation.acceptedHalfLine(),
                    installation.actualStoredCapsule(),stored);
            return new BoundaryResult(kind,start.relation(),Optional.of(new PrefixReceipt(prefixKind,1.0)),
                    Optional.empty(),Optional.of(receipt),Optional.empty(),budget.snapshot());
        } catch (Limit ex) {
            return unresolvedBoundary(start.relation(),Reason.EXACT_WORK_LIMIT,budget);
        } catch (IllegalArgumentException ex) {
            return unresolvedBoundary(start.relation(),Reason.UNSUPPORTED_DOMAIN,budget);
        }
    }

    static OriginalPrefixResult proveOriginalPrefix(Capsule start, Vec3d originalDelta,
            double endFraction, Aabb obstacle, StoredInstallCandidate installation,
            ContactAccuracy accuracy, QueryLimits limits) {
        Budget budget=new Budget(limits);
        ContactResult initial=query(start,obstacle,accuracy,budget);
        if(initial.relation()==Relation.UNRESOLVED)
            return originalPrefixFailure(initial.relation(),initial.reason().orElseThrow(),budget);
        if(initial.relation()==Relation.OVERLAPPING)
            return originalPrefixFailure(initial.relation(),Reason.NO_PREFIX_CERTIFICATE,budget);
        try {
            if(!Double.isFinite(endFraction) || endFraction<=0 || endFraction>1
                    || !translationDomain(start,originalDelta,obstacle,budget)
                    || !domain(installation.actualStoredCapsule(),obstacle,budget)
                    || start.radius()<=0
                    || !start.centerline().start().equals(start.centerline().end())
                    || !installation.actualStoredCapsule().centerline().start()
                            .equals(installation.actualStoredCapsule().centerline().end())
                    || !installation.acceptedHalfLine().equals(new Vec3d(0,0,0)))
                return originalPrefixFailure(initial.relation(),Reason.UNSUPPORTED_DOMAIN,budget);
            if(Double.doubleToRawLongBits(endFraction)
                    !=Double.doubleToRawLongBits(installation.fraction())
                    || !validInstallIdentity(start,originalDelta,installation,budget))
                return originalPrefixFailure(initial.relation(),Reason.ROUNDED_INSTALL_UNSAFE,budget);

            // Affinity in the exact original parameter, including both endpoints of the
            // point centerline, proves [0,t]; the stored endpoint also proves the bridge.
            if(!sameFaceOriginalPrefix(start,originalDelta,endFraction,
                    installation.actualStoredCapsule(),obstacle,budget))
                return originalPrefixFailure(initial.relation(),Reason.NO_PREFIX_CERTIFICATE,budget);
            BridgeProof bridge=bridgeReason(start,originalDelta,obstacle,installation,accuracy,budget);
            if(bridge.reason()!=null)
                return originalPrefixFailure(initial.relation(),bridge.reason(),budget);
            ContactResult stored=query(installation.actualStoredCapsule(),obstacle,accuracy,budget);
            if(stored.relation()==Relation.UNRESOLVED)
                return originalPrefixFailure(initial.relation(),stored.reason().orElseThrow(),budget);
            if(stored.relation()!=Relation.TOUCHING || stored.witness().isEmpty())
                return originalPrefixFailure(initial.relation(),Reason.CORRECTION_PATH_UNPROVED,budget);
            PrefixReceipt prefix=new PrefixReceipt(PrefixKind.NONPENETRATING,endFraction);
            InstalledPoseReceipt installed=new InstalledPoseReceipt(endFraction,
                    installation.acceptedStoredCenter(),installation.acceptedHalfLine(),
                    installation.actualStoredCapsule(),stored);
            OriginalPrefixReceipt proof=new OriginalPrefixReceipt(start,originalDelta,endFraction,
                    obstacle,installation,prefix,installed);
            return new OriginalPrefixResult(initial.relation(),Optional.of(proof),
                    Optional.empty(),budget.snapshot());
        } catch(Limit ex) {
            return originalPrefixFailure(initial.relation(),Reason.EXACT_WORK_LIMIT,budget);
        } catch(ArithmeticException ex) {
            return originalPrefixFailure(initial.relation(),Reason.NUMERICAL_ENCLOSURE,budget);
        } catch(IllegalArgumentException ex) {
            return originalPrefixFailure(initial.relation(),Reason.UNSUPPORTED_DOMAIN,budget);
        }
    }

    static ElongatedOriginalPrefixResult proveOriginalCapsulePrefix(Capsule start, Vec3d originalDelta,
            double endFraction, Aabb obstacle, StoredInstallCandidate installation,
            ContactAccuracy accuracy, QueryLimits limits) {
        Budget budget=new Budget(limits);
        ContactResult initial=query(start,obstacle,accuracy,budget);
        if(initial.relation()==Relation.UNRESOLVED)
            return elongatedPrefixFailure(initial.relation(),initial.reason().orElseThrow(),budget);
        if(initial.relation()==Relation.OVERLAPPING)
            return elongatedPrefixFailure(initial.relation(),Reason.NO_PREFIX_CERTIFICATE,budget);
        try {
            if(!Double.isFinite(endFraction) || endFraction<0 || endFraction>1
                    || !translationDomain(start,originalDelta,obstacle,budget)
                    || !domain(installation.actualStoredCapsule(),obstacle,budget))
                return elongatedPrefixFailure(initial.relation(),Reason.UNSUPPORTED_DOMAIN,budget);
            if(Double.doubleToRawLongBits(endFraction)
                    !=Double.doubleToRawLongBits(installation.fraction())
                    || !validInstallIdentity(start,originalDelta,installation,budget)
                    || !canonicalInstallIdentity(start,originalDelta,endFraction,installation))
                return elongatedPrefixFailure(initial.relation(),Reason.ROUNDED_INSTALL_UNSAFE,budget);

            if(!sameFaceOriginalCapsulePrefix(start,originalDelta,endFraction,
                    installation,obstacle,budget))
                return elongatedPrefixFailure(initial.relation(),Reason.NO_PREFIX_CERTIFICATE,budget);

            // The same-face proof covers the complete ideal path and terminal bridge. This
            // overload still charges and checks the existing bounded correspondence allowance.
            BridgeProof bridge=bridgeReason(start,originalDelta,obstacle,installation,accuracy,budget,true);
            if(bridge.reason()!=null)
                return elongatedPrefixFailure(initial.relation(),bridge.reason(),budget);

            ContactResult stored=query(installation.actualStoredCapsule(),obstacle,accuracy,budget);
            if(stored.relation()==Relation.UNRESOLVED)
                return elongatedPrefixFailure(initial.relation(),stored.reason().orElseThrow(),budget);
            if((stored.relation()!=Relation.SEPARATED && stored.relation()!=Relation.TOUCHING)
                    || stored.witness().isEmpty())
                return elongatedPrefixFailure(initial.relation(),Reason.CORRECTION_PATH_UNPROVED,budget);

            Work work=budget.snapshot();
            PrefixReceipt prefix=new PrefixReceipt(PrefixKind.NONPENETRATING,endFraction);
            InstalledPoseReceipt installed=new InstalledPoseReceipt(endFraction,
                    installation.acceptedStoredCenter(),installation.acceptedHalfLine(),
                    installation.actualStoredCapsule(),stored);
            ElongatedOriginalPrefixReceipt proof=new ElongatedOriginalPrefixReceipt(start,
                    originalDelta,endFraction,obstacle,accuracy,limits,installation,prefix,installed,work);
            return new ElongatedOriginalPrefixResult(initial.relation(),Optional.of(proof),
                    Optional.empty(),work);
        } catch(Limit ex) {
            return elongatedPrefixFailure(initial.relation(),Reason.EXACT_WORK_LIMIT,budget);
        } catch(ArithmeticException ex) {
            return elongatedPrefixFailure(initial.relation(),Reason.NUMERICAL_ENCLOSURE,budget);
        }
    }

    private static ElongatedOriginalPrefixResult elongatedPrefixFailure(Relation startRelation,
            Reason reason,Budget budget) {
        return new ElongatedOriginalPrefixResult(startRelation,Optional.empty(),Optional.of(reason),
                budget.snapshot());
    }

    private static boolean canonicalInstallIdentity(Capsule start,Vec3d originalDelta,
            double fraction,StoredInstallCandidate candidate) {
        Vec3d acceptedStart=candidate.acceptedStartCenter(),halfLine=candidate.acceptedHalfLine();
        Segment3d expectedStart=new Segment3d(acceptedStart.subtract(halfLine),acceptedStart.add(halfLine));
        if(!sameRawSegment(start.centerline(),expectedStart)) return false;
        Vec3d expectedStoredCenter=acceptedStart.add(originalDelta.multiply(fraction));
        if(!sameRawVector(expectedStoredCenter,candidate.acceptedStoredCenter())) return false;
        Vec3d acceptedStored=candidate.acceptedStoredCenter();
        Segment3d expectedStored=new Segment3d(acceptedStored.subtract(halfLine),acceptedStored.add(halfLine));
        return sameRawSegment(candidate.actualStoredCapsule().centerline(),expectedStored)
                && Double.doubleToRawLongBits(start.radius())
                    ==Double.doubleToRawLongBits(candidate.actualStoredCapsule().radius());
    }

    private static boolean sameRawSegment(Segment3d first,Segment3d second) {
        return sameRawVector(first.start(),second.start()) && sameRawVector(first.end(),second.end());
    }

    private static boolean sameRawVector(Vec3d first,Vec3d second) {
        return Double.doubleToRawLongBits(first.x())==Double.doubleToRawLongBits(second.x())
                && Double.doubleToRawLongBits(first.y())==Double.doubleToRawLongBits(second.y())
                && Double.doubleToRawLongBits(first.z())==Double.doubleToRawLongBits(second.z());
    }

    private static boolean sameFaceOriginalCapsulePrefix(Capsule start,Vec3d originalDelta,
            double endFraction,StoredInstallCandidate install,Aabb obstacle,Budget budget) {
        Q[] movement=coordinates(originalDelta,budget),min=coordinates(obstacle.min(),budget),
                max=coordinates(obstacle.max(),budget);
        Q t=Q.of(endFraction,budget),radius=Q.of(start.radius(),budget);
        Vec3d center=install.acceptedStartCenter(),halfLine=install.acceptedHalfLine();
        Vec3d[] startRounded={center.subtract(halfLine),center.add(halfLine)};
        Vec3d storedCenter=install.acceptedStoredCenter();
        Vec3d[] storedRounded={storedCenter.subtract(halfLine),storedCenter.add(halfLine)};
        Q[][] beginning=new Q[2][3],ideal=new Q[2][3],roundedEnd=new Q[2][3];
        for(int endpoint=0;endpoint<2;endpoint++) {
            beginning[endpoint]=coordinates(startRounded[endpoint],budget);
            roundedEnd[endpoint]=coordinates(storedRounded[endpoint],budget);
            for(int axis=0;axis<3;axis++)
                ideal[endpoint][axis]=beginning[endpoint][axis]
                        .add(movement[axis].mul(t,budget),budget);
        }

        /*
         * UG's accepted body path uses fl(delta*s), then fl(startCenter+offset), then
         * fl(center +/- fixedHalfLine). With every operand except s fixed, each IEEE
         * round-to-nearest operation is monotone in that coordinate (including subnormals).
         * Thus each actual endpoint for 0<=s<=t lies between the verified start and stored
         * RN endpoints. The ideal endpoint is affine in exact s and lies between its exact
         * start and exact t values. The terminal ideal-to-stored bridge is affine too.
         * Requiring all six endpoint values on one common radius-expanded exterior face
         * proves these three closed paths separately; it is sufficient, never inferred from
         * samples, and deliberately fails closed when no one face works.
         */
        for(int axis=0;axis<3;axis++) {
            Q below=min[axis].sub(radius,budget),above=max[axis].add(radius,budget);
            boolean allBelow=true,allAbove=true;
            for(int endpoint=0;endpoint<2;endpoint++) {
                for(Q point:new Q[]{beginning[endpoint][axis],ideal[endpoint][axis],
                        roundedEnd[endpoint][axis]}) {
                    if(point.compareTo(below,budget)>0) allBelow=false;
                    if(point.compareTo(above,budget)<0) allAbove=false;
                }
            }
            if(allBelow || allAbove) return true;
        }
        return false;
    }

    private static OriginalPrefixResult originalPrefixFailure(Relation startRelation,
            Reason reason,Budget budget) {
        return new OriginalPrefixResult(startRelation,Optional.empty(),Optional.of(reason),budget.snapshot());
    }

    private static boolean sameFaceOriginalPrefix(Capsule start,Vec3d originalDelta,
            double endFraction,Capsule stored,Aabb obstacle,Budget budget) {
        Q[] movement=coordinates(originalDelta,budget),
                min=coordinates(obstacle.min(),budget),max=coordinates(obstacle.max(),budget);
        Q t=Q.of(endFraction,budget);
        Q radius=Q.of(start.radius(),budget);
        Vec3d[] starts={start.centerline().start(),start.centerline().end()};
        Vec3d[] actuals={stored.centerline().start(),stored.centerline().end()};
        Q[][] beginning=new Q[2][3],ideal=new Q[2][3],actual=new Q[2][3];
        for(int endpoint=0;endpoint<2;endpoint++) {
            beginning[endpoint]=coordinates(starts[endpoint],budget);
            actual[endpoint]=coordinates(actuals[endpoint],budget);
            for(int axis=0;axis<3;axis++)
                ideal[endpoint][axis]=beginning[endpoint][axis]
                        .add(movement[axis].mul(t,budget),budget);
        }
        for(int axis=0;axis<3;axis++) {
            Q below=min[axis].sub(radius,budget),above=max[axis].add(radius,budget);
            boolean allBelow=true,allAbove=true;
            for(Q[][] pose:new Q[][][]{beginning,ideal,actual})
                for(int endpoint=0;endpoint<2;endpoint++) {
                    if(pose[endpoint][axis].compareTo(below,budget)>0) allBelow=false;
                    if(pose[endpoint][axis].compareTo(above,budget)<0) allAbove=false;
                }
            if(allBelow || allAbove) return true;
        }
        return false;
    }

    private static BoundaryResult unresolvedBoundary(Relation start, Reason reason, Budget budget) {
        return new BoundaryResult(BoundaryKind.UNRESOLVED,start,Optional.empty(),Optional.empty(),
                Optional.empty(),Optional.of(reason),budget.snapshot());
    }

    private static int contactDirectionalDerivative(Candidate witness,Vec3d delta,Budget budget) {
        return contactDirectionalDerivativeValue(witness,delta,budget).signum();
    }

    private static Q contactDirectionalDerivativeValue(Candidate witness,Vec3d delta,Budget budget) {
        Q[] movement=coordinates(delta,budget);
        Q dot=Q.ZERO;
        for(int axis=0;axis<3;axis++)
            dot=dot.add(witness.segment[axis].sub(witness.box[axis],budget)
                    .mul(movement[axis],budget),budget);
        return dot;
    }

    /**
     * A zero derivative of squared distance to the convex Minkowski difference
     * is a global minimum. The exact query at that rational fraction then
     * proves whether the complete translation is separated, tangent, or blocked.
     */
    private static StationaryResult stationaryMinimum(Capsule capsule,Vec3d delta,Aabb obstacle,
            ContactAccuracy accuracy,Candidate start,Candidate end,Budget budget) {
        Q low=Q.ZERO,high=Q.ONE;
        Q lowDerivative=contactDirectionalDerivativeValue(start,delta,budget);
        Q highDerivative=contactDirectionalDerivativeValue(end,delta,budget);
        if(lowDerivative.signum()>=0 || highDerivative.signum()<=0)
            return new StationaryResult(Relation.SEPARATED,null);

        int attempts=0;
        while(true) {
            budget.refinementSteps++;
            attempts++;
            if(!budget.limits.recordsOnly() && attempts>budget.limits.maxRefinementSteps())
                return new StationaryResult(Relation.UNRESOLVED,Reason.EXACT_WORK_LIMIT);

            Q width=high.sub(low,budget);
            Q fraction;
            // Exact regula falsi usually lands on the active quadratic's root in
            // one step. Periodic bisection prevents a stagnant endpoint from
            // consuming the entire bounded search when active features change.
            if(attempts%3==0) {
                fraction=low.add(high,budget).div(Q.TWO,budget);
            } else {
                fraction=low.sub(lowDerivative.mul(width,budget)
                        .div(highDerivative.sub(lowDerivative,budget),budget),budget);
                if(fraction.compareTo(low,budget)<=0 || fraction.compareTo(high,budget)>=0)
                    fraction=low.add(high,budget).div(Q.TWO,budget);
            }

            ContactResult sample=queryAt(capsule,delta,fraction,obstacle,accuracy,budget);
            if(sample.relation()==Relation.UNRESOLVED)
                return new StationaryResult(Relation.UNRESOLVED,
                        sample.reason().orElse(Reason.NO_PREFIX_CERTIFICATE));
            Q derivative=contactDirectionalDerivativeValue(budget.lastCandidate,delta,budget);
            if(derivative.signum()==0)
                return new StationaryResult(sample.relation(),null);
            if(derivative.signum()<0) {
                low=fraction;
                lowDerivative=derivative;
            } else {
                high=fraction;
                highDerivative=derivative;
            }
        }
    }

    private static BoundaryResult refineOverlap(Capsule capsule,Vec3d delta,Aabb obstacle,
            StoredInstallCandidate installation,ContactAccuracy accuracy,Budget budget) {
        double low=0,high=1;
        PrefixKind prefixKind=PrefixKind.CLEAR_SEPARATED;
        double length=Math.hypot(Math.hypot(delta.x(),delta.y()),delta.z());
        try {
        while(Math.nextUp(Math.nextUp(length)*(high-low))>accuracy.maxPathWidthMeters()) {
            budget.refinementSteps++;
            if(!budget.limits.recordsOnly() && budget.refinementSteps>budget.limits.maxRefinementSteps())
                return unresolvedWithPrefix(prefixKind,low,Reason.EXACT_WORK_LIMIT,budget);
            double mid=low+(high-low)/2;
            if(mid==low || mid==high)
                return unresolvedWithPrefix(prefixKind,low,Reason.ACCURACY_UNREACHABLE,budget);
            ContactResult sample=queryAt(capsule,delta,mid,obstacle,accuracy,budget);
            if(sample.relation()==Relation.UNRESOLVED)
                return new BoundaryResult(BoundaryKind.UNRESOLVED,Relation.SEPARATED,
                        Optional.of(new PrefixReceipt(prefixKind,low)),Optional.empty(),Optional.empty(),
                        sample.reason(),budget.snapshot());
            if(sample.relation()==Relation.OVERLAPPING) high=mid;
            else {low=mid;if(sample.relation()==Relation.TOUCHING) prefixKind=PrefixKind.NONPENETRATING;}
        }
        PrefixReceipt prefix=new PrefixReceipt(prefixKind,low);
        Interval bracket=new Interval(low,high);
        if(installation.fraction()>low)
            return new BoundaryResult(BoundaryKind.UNRESOLVED,Relation.SEPARATED,
                    Optional.of(prefix),Optional.of(bracket),Optional.empty(),
                    Optional.of(Reason.ROUNDED_INSTALL_UNSAFE),budget.snapshot());
        if(!domain(installation.actualStoredCapsule(),obstacle,budget))
            return new BoundaryResult(BoundaryKind.UNRESOLVED,Relation.SEPARATED,
                    Optional.of(prefix),Optional.of(bracket),Optional.empty(),
                    Optional.of(Reason.UNSUPPORTED_DOMAIN),budget.snapshot());
        if(!validInstallIdentity(capsule,delta,installation,budget))
            return new BoundaryResult(BoundaryKind.UNRESOLVED,Relation.SEPARATED,
                    Optional.of(prefix),Optional.of(bracket),Optional.empty(),
                    Optional.of(Reason.ROUNDED_INSTALL_UNSAFE),budget.snapshot());
        BridgeProof bridge=bridgeReason(capsule,delta,obstacle,installation,accuracy,budget);
        if(bridge.reason()!=null) return new BoundaryResult(BoundaryKind.UNRESOLVED,Relation.SEPARATED,
                Optional.of(prefix),Optional.of(bracket),Optional.empty(),Optional.of(bridge.reason()),budget.snapshot());
        ContactResult stored=query(installation.actualStoredCapsule(),obstacle,accuracy,budget);
        if(stored.relation()==Relation.UNRESOLVED || stored.relation()==Relation.OVERLAPPING)
            return new BoundaryResult(BoundaryKind.UNRESOLVED,Relation.SEPARATED,
                    Optional.of(prefix),Optional.of(bracket),Optional.empty(),
                    Optional.of(stored.reason().orElse(Reason.CORRECTION_PATH_UNPROVED)),budget.snapshot());
        InstalledPoseReceipt receipt=new InstalledPoseReceipt(installation.fraction(),
                installation.acceptedStoredCenter(),installation.acceptedHalfLine(),
                installation.actualStoredCapsule(),stored);
        return new BoundaryResult(BoundaryKind.CERTIFIED_BOUNDARY,Relation.SEPARATED,
                Optional.of(prefix),Optional.of(bracket),Optional.of(receipt),Optional.empty(),budget.snapshot());
        } catch(Limit ex) {
            return unresolvedWithPrefix(prefixKind,low,Reason.EXACT_WORK_LIMIT,budget);
        } catch(ArithmeticException ex) {
            return unresolvedWithPrefix(prefixKind,low,Reason.NUMERICAL_ENCLOSURE,budget);
        } catch(IllegalArgumentException ex) {
            return unresolvedWithPrefix(prefixKind,low,Reason.UNSUPPORTED_DOMAIN,budget);
        }
    }

    private static BoundaryResult unresolvedWithPrefix(PrefixKind kind,double low,Reason reason,Budget budget) {
        return new BoundaryResult(BoundaryKind.UNRESOLVED,Relation.SEPARATED,
                Optional.of(new PrefixReceipt(kind,low)),Optional.empty(),Optional.empty(),
                Optional.of(reason),budget.snapshot());
    }

    private static BoundaryResult blockedAtStart(Capsule capsule,Vec3d delta,Aabb obstacle,
            StoredInstallCandidate installation,ContactAccuracy accuracy,Budget budget) {
        // A negative one-sided derivative of convex clearance proves immediate overlap.
        if(!domain(installation.actualStoredCapsule(),obstacle,budget))
            return unresolvedBoundary(Relation.TOUCHING,Reason.UNSUPPORTED_DOMAIN,budget);
        if(installation.fraction()!=0 || !validInstallIdentity(capsule,delta,installation,budget)
                || !exactIdealEqualsStored(capsule,delta,0,installation.actualStoredCapsule(),budget))
            return unresolvedBoundary(Relation.TOUCHING,Reason.ROUNDED_INSTALL_UNSAFE,budget);
        ContactResult stored=query(installation.actualStoredCapsule(),obstacle,accuracy,budget);
        if(stored.relation()!=Relation.TOUCHING)
            return unresolvedBoundary(Relation.TOUCHING,stored.reason().orElse(Reason.CORRECTION_PATH_UNPROVED),budget);
        return new BoundaryResult(BoundaryKind.BLOCKED_AT_START,Relation.TOUCHING,
                Optional.empty(),Optional.empty(),Optional.of(new InstalledPoseReceipt(0,
                installation.acceptedStoredCenter(),installation.acceptedHalfLine(),
                installation.actualStoredCapsule(),stored)),Optional.empty(),budget.snapshot());
    }

    private static boolean translationDomain(Capsule capsule,Vec3d delta,Aabb obstacle,Budget budget) {
        Q[] movement=coordinates(delta,budget),origin=coordinates(obstacle.min(),budget);
        Q movementSquared=Q.ZERO, localLimit=Q.of(512*512,budget), worldLimit=Q.of(COORD_LIMIT,budget);
        for(Q component:movement) movementSquared=movementSquared.add(component.square(budget),budget);
        if(movementSquared.compareTo(Q.of(64*64,budget),budget)>0) return false;
        for(Vec3d endpoint:new Vec3d[]{capsule.centerline().start(),capsule.centerline().end()}) {
            Q[] start=coordinates(endpoint,budget);
            Q startLocal=Q.ZERO,finishLocal=Q.ZERO;
            for(int axis=0;axis<3;axis++) {
                Q finish=start[axis].add(movement[axis],budget);
                if(finish.compareTo(worldLimit,budget)>0 || finish.compareTo(worldLimit.negate(budget),budget)<0)
                    return false;
                Q relative=start[axis].sub(origin[axis],budget),endRelative=finish.sub(origin[axis],budget);
                startLocal=startLocal.add(relative.square(budget),budget);
                finishLocal=finishLocal.add(endRelative.square(budget),budget);
            }
            if(startLocal.compareTo(localLimit,budget)>0 || finishLocal.compareTo(localLimit,budget)>0)
                return false;
        }
        return true;
    }

    private static boolean validInstallIdentity(Capsule start, Vec3d delta,
            StoredInstallCandidate candidate, Budget budget) {
        Q[] half = coordinates(candidate.acceptedHalfLine(),budget);
        Q norm = Q.ZERO;
        for (Q component:half) norm=norm.add(component.square(budget),budget);
        if (norm.compareTo(Q.of(4096,budget),budget)>0) return false;
        Vec3d center = candidate.acceptedStartCenter(), offset = candidate.acceptedHalfLine();
        Segment3d reconstructedStart = new Segment3d(center.subtract(offset),center.add(offset));
        if (!start.centerline().equals(reconstructedStart)) return false;
        Vec3d expectedCenter = center.add(delta.multiply(candidate.fraction()));
        if (!expectedCenter.equals(candidate.acceptedStoredCenter())) return false;
        Vec3d storedCenter = candidate.acceptedStoredCenter();
        Segment3d reconstructedStored = new Segment3d(storedCenter.subtract(offset),storedCenter.add(offset));
        return candidate.actualStoredCapsule().centerline().equals(reconstructedStored)
                && Double.doubleToRawLongBits(start.radius())
                ==Double.doubleToRawLongBits(candidate.actualStoredCapsule().radius());
    }

    private static boolean exactIdealEqualsStored(Capsule start, Vec3d delta, double fraction,
                                                   Capsule stored, Budget budget) {
        Q[] translation = coordinates(delta,budget);
        Q t=Q.of(fraction,budget);
        Vec3d[] first={start.centerline().start(),start.centerline().end()};
        Vec3d[] second={stored.centerline().start(),stored.centerline().end()};
        for (int endpoint=0;endpoint<2;endpoint++) {
            Q[] initial=coordinates(first[endpoint],budget), actual=coordinates(second[endpoint],budget);
            for (int axis=0;axis<3;axis++)
                if (initial[axis].add(translation[axis].mul(t,budget),budget).compareTo(actual[axis],budget)!=0)
                    return false;
        }
        return true;
    }

    private static BridgeProof bridgeReason(Capsule start,Vec3d delta,Aabb obstacle,
            StoredInstallCandidate install,ContactAccuracy accuracy,Budget budget) {
        return bridgeReason(start,delta,obstacle,install,accuracy,budget,false);
    }

    private static BridgeProof bridgeReason(Capsule start,Vec3d delta,Aabb obstacle,
            StoredInstallCandidate install,ContactAccuracy accuracy,Budget budget,
            boolean commonFaceBridgeAlreadyProved) {
        Q fraction=Q.of(install.fraction(),budget),maximum=Q.of(128,budget);
        Q[] movement=coordinates(delta,budget);
        Q[][] ideal=new Q[2][3],actual=new Q[2][3];
        Vec3d[] startEndpoints={start.centerline().start(),start.centerline().end()};
        Vec3d[] storedEndpoints={install.actualStoredCapsule().centerline().start(),
                install.actualStoredCapsule().centerline().end()};
        for(int endpoint=0;endpoint<2;endpoint++) {
            Q[] initial=coordinates(startEndpoints[endpoint],budget);
            actual[endpoint]=coordinates(storedEndpoints[endpoint],budget);
            for(int axis=0;axis<3;axis++) {
                ideal[endpoint][axis]=initial[axis].add(movement[axis].mul(fraction,budget),budget);
                maximum=max(maximum,initial[axis].abs(budget),budget);
                maximum=max(maximum,ideal[endpoint][axis].abs(budget),budget);
                maximum=max(maximum,movement[axis].abs(budget),budget);
            }
        }
        Q sixtyFour=Q.of(64,budget);
        Q rawB=sixtyFour.mul(Q.of(0x1.0p-53,budget),budget).mul(maximum,budget)
                .add(sixtyFour.mul(Q.of(Double.MIN_VALUE,budget),budget),budget);
        double coordinateBound=enclosure(rawB,budget).upper();
        double radius=Math.nextUp(Math.sqrt(3)*coordinateBound);
        Q three=Q.of(3,budget),coordinateBoundSquared=Q.of(coordinateBound,budget).square(budget);
        for(int i=0;i<16 && Q.of(radius,budget).square(budget)
                .compareTo(three.mul(coordinateBoundSquared,budget),budget)<0;i++) radius=Math.nextUp(radius);
        if(!Double.isFinite(radius) || radius>1.0/4096.0
                || Q.of(radius,budget).square(budget)
                .compareTo(three.mul(coordinateBoundSquared,budget),budget)<0)
            return new BridgeProof(Reason.ROUNDED_INSTALL_UNSAFE,false);
        Q radiusSquared=Q.of(radius,budget).square(budget);
        boolean matching=correspondenceWithin(ideal,actual,0,radiusSquared,budget);
        boolean swapped=correspondenceWithin(ideal,actual,1,radiusSquared,budget);
        if(!matching || swapped && !start.centerline().start().equals(start.centerline().end()))
            return new BridgeProof(Reason.ROUNDED_INSTALL_UNSAFE,false);
        boolean zero=true;
        for(int endpoint=0;endpoint<2;endpoint++) for(int axis=0;axis<3;axis++)
            if(ideal[endpoint][axis].compareTo(actual[endpoint][axis],budget)!=0) zero=false;
        if(zero) return new BridgeProof(null,false);
        if(commonFaceBridgeAlreadyProved) return new BridgeProof(null,true);
        ContactResult idealContact=queryAt(start,delta,install.fraction(),obstacle,accuracy,budget);
        if(idealContact.relation()==Relation.UNRESOLVED)
            return new BridgeProof(idealContact.reason().orElseThrow(),false);
        if(idealContact.relation()==Relation.SEPARATED) {
            if(idealContact.signedClearanceMeters().orElseThrow().lower()>=radius)
                return new BridgeProof(null,false);
            if(sameFaceNonpenetrating(ideal,actual,obstacle,start.radius(),budget))
                return new BridgeProof(null,true);
        }
        if(idealContact.relation()==Relation.TOUCHING) {
            Q dot=Q.ZERO;
            Candidate idealWitness=budget.lastCandidate;
            for(int axis=0;axis<3;axis++) {
                Q correction=actual[0][axis].sub(ideal[0][axis],budget);
                if(correction.compareTo(actual[1][axis].sub(ideal[1][axis],budget),budget)!=0)
                    return new BridgeProof(Reason.CORRECTION_PATH_UNPROVED,false);
                dot=dot.add(idealWitness.segment[axis].sub(idealWitness.box[axis],budget)
                        .mul(correction,budget),budget);
            }
            if(dot.signum()>=0) return new BridgeProof(null,false);
        }
        return new BridgeProof(Reason.CORRECTION_PATH_UNPROVED,false);
    }

    private static boolean sameFaceNonpenetrating(Q[][] ideal,Q[][] actual,Aabb obstacle,
            double radiusValue,Budget budget) {
        Q[] min=coordinates(obstacle.min(),budget),max=coordinates(obstacle.max(),budget);
        Q radius=Q.of(radiusValue,budget);
        for(int axis=0;axis<3;axis++) {
            Q minThreshold=min[axis].sub(radius,budget);
            Q maxThreshold=max[axis].add(radius,budget);
            boolean outsideMin=true,outsideMax=true;
            for(int endpoint=0;endpoint<2;endpoint++) {
                if(ideal[endpoint][axis].compareTo(minThreshold,budget)>0
                        || actual[endpoint][axis].compareTo(minThreshold,budget)>0)
                    outsideMin=false;
                if(ideal[endpoint][axis].compareTo(maxThreshold,budget)<0
                        || actual[endpoint][axis].compareTo(maxThreshold,budget)<0)
                    outsideMax=false;
            }
            if(outsideMin || outsideMax) return true;
        }
        return false;
    }

    private static boolean correspondenceWithin(Q[][] ideal,Q[][] actual,int swapped,
                                                Q radiusSquared,Budget budget) {
        for(int endpoint=0;endpoint<2;endpoint++) {
            Q norm=Q.ZERO;
            for(int axis=0;axis<3;axis++) {
                Q residual=actual[endpoint^swapped][axis].sub(ideal[endpoint][axis],budget);
                norm=norm.add(residual.square(budget),budget);
            }
            if(norm.compareTo(radiusSquared,budget)>0) return false;
        }
        return true;
    }

    private static Q max(Q first,Q second,Budget budget) {
        return first.compareTo(second,budget)>=0?first:second;
    }

    private static final class Budget {
        final QueryLimits limits;
        Candidate lastCandidate;
        int exactAttempts, exactOps, peakExactBits, staticAttempts, staticEvaluations, staticCandidateUnits;
        int castAttempts, castQueries, castEvaluations, castCandidateUnits, refinementSteps;
        Budget(QueryLimits limits) { this.limits = limits; }
        void operation(BigInteger left, BigInteger right, int predictedBits) {
            exactAttempts++;
            peakExactBits = Math.max(peakExactBits, Math.max(magnitudeBits(left), magnitudeBits(right)));
            if (!limits.recordsOnly()
                    && (exactOps >= limits.maxExactOps() || predictedBits > limits.maxExactBits())) throw new Limit();
            exactOps++;
        }
        void unary(BigInteger value,int predictedBits) {
            operation(value,BigInteger.ZERO,predictedBits);
        }
        void result(BigInteger value) {
            peakExactBits = Math.max(peakExactBits, magnitudeBits(value));
            if (!limits.recordsOnly() && peakExactBits > limits.maxExactBits()) throw new Limit();
        }
        Work snapshot() {
            return new Work(exactAttempts, exactOps, peakExactBits, staticAttempts, staticEvaluations,
                    staticCandidateUnits,castAttempts,castQueries,castEvaluations,castCandidateUnits,
                    staticCandidateUnits+castCandidateUnits,refinementSteps);
        }
    }
    private static final class Limit extends RuntimeException { }

    private record Q(BigInteger n, BigInteger d) {
        static final Q ZERO = new Q(BigInteger.ZERO, BigInteger.ONE);
        static final Q ONE = new Q(BigInteger.ONE, BigInteger.ONE);
        static final Q TWO = new Q(BigInteger.TWO, BigInteger.ONE);
        Q { if (d.signum() <= 0) throw new IllegalArgumentException("denominator"); }
        static Q of(double value, Budget b) {
            long bits = Double.doubleToRawLongBits(value);
            boolean negative = (bits >>> 63) != 0;
            int exponent = (int)((bits >>> 52) & 0x7ffL);
            long significand = bits & 0x000f_ffff_ffff_ffffL;
            if (exponent != 0) significand |= 1L << 52;
            int shift = exponent == 0 ? -1074 : exponent - 1075;
            BigInteger n = BigInteger.valueOf(significand);
            if (n.signum() == 0) return ZERO;
            b.unary(n,n.bitLength());
            int trailing = n.getLowestSetBit();
            if (trailing>0) {
                b.unary(n,n.bitLength());
                n=n.shiftRight(trailing);
            }
            shift+=trailing;
            BigInteger denominator=BigInteger.ONE;
            if(shift>0) {
                b.unary(n,n.bitLength()+shift);
                n=n.shiftLeft(shift);
            } else if(shift<0) {
                b.unary(BigInteger.ONE,1-shift);
                denominator=BigInteger.ONE.shiftLeft(-shift);
            }
            if(negative) {
                b.unary(n,n.bitLength());
                n=n.negate();
            }
            Q q = new Q(n,denominator);
            b.result(q.n); b.result(q.d);
            return q;
        }
        Q add(Q other, Budget b) {
            BigInteger x = mul(n, other.d, b), y = mul(other.n, d, b);
            return normalized(add(x,y,b), mul(d,other.d,b), b);
        }
        Q sub(Q other, Budget b) { return add(other.negate(b),b); }
        Q negate(Budget b) {
            b.unary(n,magnitudeBits(n));
            return new Q(n.negate(),d);
        }
        Q abs(Budget b) { return n.signum()<0?negate(b):this; }
        Q mul(Q other, Budget b) { return normalized(mul(n,other.n,b),mul(d,other.d,b),b); }
        Q square(Budget b) { return mul(this,b); }
        Q div(Q other, Budget b) {
            if (other.n.signum() == 0) throw new ArithmeticException("divide by zero");
            BigInteger divisor=other.n;
            if(divisor.signum()<0) {
                b.unary(divisor,magnitudeBits(divisor));
                divisor=divisor.abs();
            }
            BigInteger num = mul(n,other.d,b), den = mul(d,divisor,b);
            if(other.n.signum()<0) {
                b.unary(num,magnitudeBits(num));
                num=num.negate();
            }
            return normalized(num, den,b);
        }
        int compareTo(Q other, Budget b) {
            BigInteger left = mul(n,other.d,b), right = mul(other.n,d,b);
            b.operation(left,right,Math.max(magnitudeBits(left),magnitudeBits(right)));
            return left.compareTo(right);
        }
        int signum() { return n.signum(); }
        double toDouble() { return new BigDecimal(n).divide(new BigDecimal(d), MathContext.DECIMAL128).doubleValue(); }
        private static Q normalized(BigInteger n, BigInteger d, Budget b) {
            if (n.signum() == 0) return ZERO;
            b.operation(n,d,Math.max(magnitudeBits(n),d.bitLength()));
            if (d.bitCount() == 1) {
                int denominatorPower = d.bitLength() - 1;
                if (denominatorPower == 0) return new Q(n,d);
                b.unary(n,magnitudeBits(n));
                int lowestSetBit = n.getLowestSetBit();
                int trailing = Math.min(lowestSetBit,denominatorPower);
                if (trailing == 0) return new Q(n,d);
                b.unary(n,magnitudeBits(n));
                BigInteger num = n.shiftRight(trailing);
                b.result(num);
                b.unary(d,d.bitLength());
                BigInteger den = d.shiftRight(trailing);
                b.result(den);
                return new Q(num,den);
            }
            b.operation(n,d,Math.max(magnitudeBits(n),d.bitLength()));
            BigInteger gcd = n.gcd(d);
            b.result(gcd);
            b.operation(n,gcd,magnitudeBits(n)); BigInteger num = n.divide(gcd);
            b.operation(d,gcd,d.bitLength()); BigInteger den = d.divide(gcd);
            return new Q(num,den);
        }
        private static BigInteger mul(BigInteger x, BigInteger y, Budget b) {
            b.operation(x,y,magnitudeBits(x)+magnitudeBits(y));
            BigInteger result = x.multiply(y); b.result(result); return result;
        }
        private static BigInteger add(BigInteger x, BigInteger y, Budget b) {
            b.operation(x,y,Math.max(magnitudeBits(x),magnitudeBits(y))+1);
            BigInteger result = x.add(y); b.result(result); return result;
        }
    }
}
