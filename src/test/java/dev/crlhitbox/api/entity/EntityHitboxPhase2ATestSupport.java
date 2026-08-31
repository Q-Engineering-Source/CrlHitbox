package dev.crlhitbox.api.entity;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Composite;
import dev.crlhitbox.api.geometry.Obb;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Solid3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/** Test-only independent oracle and deterministic fixture for Phase 2A properties. */
final class EntityHitboxPhase2ATestSupport {
    private EntityHitboxPhase2ATestSupport() {
    }

    static ResourceLocation id(Random random) {
        return new ResourceLocation("phase2a", "entry_" + random.nextInt(12));
    }

    /** Uses {@code iteration % 5} so every property deterministically visits every solid kind. */
    static PlacedSolid3d placement(Random random, int iteration) {
        double base = iteration * 0.125D + random.nextInt(17) - 8;
        Vec3d origin = new Vec3d(base, base * 0.5D, -base * 0.25D);
        Solid3d solid = switch (Math.floorMod(iteration, 5)) {
            case 0 -> new Aabb(origin, origin.add(new Vec3d(1.0D, 2.0D, 3.0D)));
            case 1 -> new Sphere(origin, 0.25D + random.nextInt(4));
            case 2 -> new Obb(
                    origin,
                    new Vec3d(0.5D, 1.0D, 1.5D),
                    new Rotation3d(0.0D, 0.0D, 0.5D, 1.0D));
            case 3 -> new Capsule(
                    new Segment3d(origin, origin.add(new Vec3d(0.75D, 1.25D, -0.5D))),
                    0.5D);
            case 4 -> new Composite(List.of(
                    new Aabb(origin, origin.add(new Vec3d(0.5D, 0.5D, 0.5D))),
                    new Sphere(origin.add(new Vec3d(1.0D, 0.0D, 0.0D)), 0.25D)));
            default -> throw new AssertionError("unreachable solid fixture kind");
        };
        return new PlacedSolid3d(
                solid,
                new RigidTransform3d(
                        new Rotation3d(0.0D, 0.0D, 0.25D, 1.0D),
                        new Vec3d(0.25D, -0.5D, 0.75D)));
    }

    static final class ReferenceHolderModel {
        private final LinkedHashMap<ResourceLocation, PlacedSolid3d> entries = new LinkedHashMap<>();
        private long revision;

        boolean put(ResourceLocation id, PlacedSolid3d placement) {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(placement, "placement");
            if (placement.equals(entries.get(id))) return false;
            long nextRevision = checkedNextRevision();
            entries.put(id, placement);
            revision = nextRevision;
            return true;
        }

        boolean remove(ResourceLocation id) {
            Objects.requireNonNull(id, "id");
            if (!entries.containsKey(id)) return false;
            long nextRevision = checkedNextRevision();
            entries.remove(id);
            revision = nextRevision;
            return true;
        }

        boolean clear() {
            if (entries.isEmpty()) return false;
            long nextRevision = checkedNextRevision();
            entries.clear();
            revision = nextRevision;
            return true;
        }

        long revision() {
            return revision;
        }

        Optional<PlacedSolid3d> find(ResourceLocation id) {
            return Optional.ofNullable(entries.get(id));
        }

        ReferenceSnapshot snapshot() {
            return new ReferenceSnapshot(revision, new ArrayList<>(entries.keySet()), new ArrayList<>(entries.values()));
        }

        private long checkedNextRevision() {
            if (revision == Long.MAX_VALUE) {
                throw new IllegalStateException("reference revision overflow");
            }
            return revision + 1L;
        }
    }

    record ReferenceSnapshot(long revision, List<ResourceLocation> ids, List<PlacedSolid3d> placements) {
        ReferenceSnapshot {
            ids = List.copyOf(ids);
            placements = List.copyOf(placements);
        }

        Optional<PlacedSolid3d> find(ResourceLocation id) {
            int index = ids.indexOf(id);
            return index < 0 ? Optional.empty() : Optional.of(placements.get(index));
        }
    }

    record FailureContext(
            long seed,
            int iteration,
            List<String> operations,
            ResourceLocation id,
            PlacedSolid3d placement,
            long expectedRevision,
            long actualRevision,
            List<ResourceLocation> expectedOrderedIds,
            List<ResourceLocation> actualOrderedIds,
            Optional<PlacedSolid3d> expectedLookup,
            Optional<PlacedSolid3d> actualLookup
    ) {
        String describe() {
            return "seed=0x" + Long.toUnsignedString(seed, 16).toUpperCase() + "L"
                    + ", iteration=" + iteration
                    + ", operationSequence=" + operations
                    + ", id=" + id
                    + ", placement=" + placement
                    + ", expectedRevision=" + expectedRevision
                    + ", actualRevision=" + actualRevision
                    + ", expectedOrderedIds=" + expectedOrderedIds
                    + ", actualOrderedIds=" + actualOrderedIds
                    + ", expectedLookup=" + expectedLookup
                    + ", actualLookup=" + actualLookup;
        }
    }
}
