package dev.crlhitbox.api.entity;

import dev.crlhitbox.api.collider.ColliderSnapshot;
import dev.crlhitbox.api.collider.CompoundColliderSnapshot;
import dev.crlhitbox.api.collider.RayColliderSnapshot;
import dev.crlhitbox.api.collider.SolidColliderSnapshot;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Vec3d;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity-local to world coordinate adapters for collider snapshots.
 *
 * <p>{@link #translationOnly(Entity)} is deliberately the simplest correct frame: the entity's
 * position point with local axes parallel to the world axes. It does not read head or body yaw,
 * pitch, model pose, or partial ticks, so no unsupported orientation claim is made. A caller that
 * needs a rotated frame supplies its own {@code entityLocalToWorld} explicitly.</p>
 *
 * <p>{@link #placeInWorld(ColliderSnapshot, RigidTransform3d)} composes the snapshot's own placement
 * with the frame exactly once and returns a new snapshot; it never mutates the cache. Queries and
 * rendering therefore share one transform contract.</p>
 */
public final class EntityColliderFrames {
    private EntityColliderFrames() {
    }

    /**
     * Returns the entity-local to world frame that only translates by the entity position.
     *
     * @param entity the entity whose current position point is used
     */
    public static RigidTransform3d translationOnly(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return new RigidTransform3d(
                Rotation3d.identity(), new Vec3d(entity.posX, entity.posY, entity.posZ));
    }

    /**
     * Places one entity-local snapshot into the world frame.
     *
     * <p>The composition is {@code entityLocal.localToParent().andThen(entityLocalToWorld)}: the
     * snapshot's own placement applies first, then the frame. Compound children keep their
     * placements relative to the compound's own local frame, so no level is applied twice.</p>
     *
     * @param entityLocal the snapshot in the entity-local frame
     * @param entityLocalToWorld the entity-local to world frame
     * @return a new snapshot expressed in the world frame
     */
    public static ColliderSnapshot placeInWorld(
            ColliderSnapshot entityLocal,
            RigidTransform3d entityLocalToWorld
    ) {
        Objects.requireNonNull(entityLocal, "entityLocal");
        Objects.requireNonNull(entityLocalToWorld, "entityLocalToWorld");
        RigidTransform3d worldPlacement =
                entityLocal.localToParent().andThen(entityLocalToWorld);
        if (entityLocal instanceof SolidColliderSnapshot solid) {
            return new SolidColliderSnapshot(
                    solid.solid(), worldPlacement, solid.enabled());
        }
        if (entityLocal instanceof RayColliderSnapshot ray) {
            return new RayColliderSnapshot(ray.ray(), worldPlacement, ray.enabled());
        }
        CompoundColliderSnapshot compound = (CompoundColliderSnapshot) entityLocal;
        List<ColliderSnapshot> children = new ArrayList<>(compound.childCount());
        for (int index = 0; index < compound.childCount(); index++) {
            children.add(compound.child(index));
        }
        return new CompoundColliderSnapshot(children, worldPlacement, compound.enabled());
    }
}
