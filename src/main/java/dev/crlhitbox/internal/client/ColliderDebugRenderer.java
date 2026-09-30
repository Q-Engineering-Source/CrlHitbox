package dev.crlhitbox.internal.client;

import dev.crlhitbox.api.collider.ColliderSnapshot;
import dev.crlhitbox.api.collider.CompoundColliderSnapshot;
import dev.crlhitbox.api.collider.RayColliderSnapshot;
import dev.crlhitbox.api.collider.SolidColliderSnapshot;
import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Composite;
import dev.crlhitbox.api.geometry.Obb;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Solid3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Client-only debug renderer that draws collider snapshots as real outlines.
 *
 * <p>Shapes are drawn from their actual geometry — oriented boxes use their own axes, spheres and
 * capsules use circle outlines, and a ray is drawn at its true finite length — rather than as a
 * single bounding box. Compounds are expanded iteratively, so a user-controlled nesting depth cannot
 * overflow the stack.</p>
 *
 * <p>Every vertex is camera-relative, and all GL state changes are confined to one method whose
 * {@code finally} block restores the previous state, so an exception cannot leave the pipeline
 * polluted. Drawing is a pure read of the snapshots: it never fires events, recomputes shapes,
 * touches the cache or contacts the network.</p>
 */
final class ColliderDebugRenderer {
    private static final int CIRCLE_SEGMENTS = 24;
    private static final int CAPSULE_SEGMENTS = 16;
    private static final int[][] EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}};

    private ColliderDebugRenderer() {
    }

    /** Draws every enabled snapshot, expressed in the world frame, relative to {@code camera}. */
    static void render(List<ColliderSnapshot> snapshots, Vec3d camera) {
        List<Vec3d[]> lines = new ArrayList<>();
        for (ColliderSnapshot snapshot : snapshots) {
            collect(snapshot, RigidTransform3d.identity(), camera, lines);
        }
        if (lines.isEmpty()) {
            return;
        }
        draw(lines);
    }

    private static void collect(
            ColliderSnapshot root,
            RigidTransform3d rootToWorld,
            Vec3d camera,
            List<Vec3d[]> lines
    ) {
        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(root, rootToWorld));
        while (!stack.isEmpty()) {
            Frame frame = stack.pop();
            ColliderSnapshot snapshot = frame.snapshot();
            if (!snapshot.enabled()) {
                continue;
            }
            if (snapshot instanceof CompoundColliderSnapshot compound) {
                RigidTransform3d childToWorld = compound.localToParent().andThen(frame.parentToWorld());
                for (int index = compound.childCount() - 1; index >= 0; index--) {
                    stack.push(new Frame(compound.child(index), childToWorld));
                }
                continue;
            }
            RigidTransform3d world = snapshot.localToParent().andThen(frame.parentToWorld());
            if (snapshot instanceof SolidColliderSnapshot solid) {
                appendSolid(lines, solid.solid(), world, camera);
            } else if (snapshot instanceof RayColliderSnapshot ray) {
                lines.add(new Vec3d[] {
                        world.transformPoint(ray.ray().origin()).subtract(camera),
                        world.transformPoint(ray.ray().end()).subtract(camera)});
            }
        }
    }

    /** One pending node: a snapshot plus the transform from its parent frame to the world. */
    private record Frame(ColliderSnapshot snapshot, RigidTransform3d parentToWorld) {
    }

    private static void appendSolid(
            List<Vec3d[]> lines,
            Solid3d solid,
            RigidTransform3d world,
            Vec3d camera
    ) {
        switch (solid) {
            case Aabb box -> appendBox(lines, boxCorners(box.min(), box.max()), world, camera);
            case Obb box -> {
                Vec3d[] corners = new Vec3d[8];
                int index = 0;
                for (int xSign = -1; xSign <= 1; xSign += 2) {
                    for (int ySign = -1; ySign <= 1; ySign += 2) {
                        for (int zSign = -1; zSign <= 1; zSign += 2) {
                            corners[index++] = box.localToWorld(new Vec3d(
                                    xSign * box.halfExtents().x(),
                                    ySign * box.halfExtents().y(),
                                    zSign * box.halfExtents().z()));
                        }
                    }
                }
                appendBox(lines, corners, world, camera);
            }
            case Sphere sphere -> {
                Vec3d center = sphere.center();
                appendCircle(lines, center, new Vec3d(1.0D, 0.0D, 0.0D),
                        new Vec3d(0.0D, 1.0D, 0.0D), sphere.radius(), CIRCLE_SEGMENTS, world, camera);
                appendCircle(lines, center, new Vec3d(0.0D, 1.0D, 0.0D),
                        new Vec3d(0.0D, 0.0D, 1.0D), sphere.radius(), CIRCLE_SEGMENTS, world, camera);
                appendCircle(lines, center, new Vec3d(0.0D, 0.0D, 1.0D),
                        new Vec3d(1.0D, 0.0D, 0.0D), sphere.radius(), CIRCLE_SEGMENTS, world, camera);
            }
            case Capsule capsule -> appendCapsule(lines, capsule, world, camera);
            case Composite composite -> {
                for (int index = 0; index < composite.childCount(); index++) {
                    appendSolid(lines, composite.child(index), world, camera);
                }
            }
        }
    }

    private static void appendCapsule(
            List<Vec3d[]> lines,
            Capsule capsule,
            RigidTransform3d world,
            Vec3d camera
    ) {
        Vec3d start = capsule.centerline().start();
        Vec3d end = capsule.centerline().end();
        Vec3d axis = end.subtract(start);
        double length = Math.hypot(Math.hypot(axis.x(), axis.y()), axis.z());
        Vec3d direction = length > 0.0D
                ? new Vec3d(axis.x() / length, axis.y() / length, axis.z() / length)
                : new Vec3d(0.0D, 1.0D, 0.0D);
        Vec3d basisA = Math.abs(direction.y()) < 0.9D
                ? new Vec3d(0.0D, 1.0D, 0.0D)
                : new Vec3d(1.0D, 0.0D, 0.0D);
        Vec3d perpendicularA = direction.cross(basisA);
        double perpendicularLength = Math.hypot(Math.hypot(
                perpendicularA.x(), perpendicularA.y()), perpendicularA.z());
        Vec3d unitA = perpendicularLength > 0.0D
                ? new Vec3d(perpendicularA.x() / perpendicularLength,
                        perpendicularA.y() / perpendicularLength,
                        perpendicularA.z() / perpendicularLength)
                : new Vec3d(1.0D, 0.0D, 0.0D);
        Vec3d unitB = direction.cross(unitA);

        appendCircle(lines, start, unitA, unitB, capsule.radius(), CAPSULE_SEGMENTS, world, camera);
        appendCircle(lines, end, unitA, unitB, capsule.radius(), CAPSULE_SEGMENTS, world, camera);
        for (int index = 0; index < 4; index++) {
            double angle = index * Math.PI / 2.0D;
            Vec3d offset = unitA.multiply(Math.cos(angle) * capsule.radius())
                    .add(unitB.multiply(Math.sin(angle) * capsule.radius()));
            appendLine(lines, start.add(offset), end.add(offset), world, camera);
        }
    }

    private static void appendCircle(
            List<Vec3d[]> lines,
            Vec3d center,
            Vec3d axisA,
            Vec3d axisB,
            double radius,
            int segments,
            RigidTransform3d world,
            Vec3d camera
    ) {
        Vec3d previous = null;
        Vec3d first = null;
        for (int index = 0; index <= segments; index++) {
            double angle = 2.0D * Math.PI * index / segments;
            Vec3d point = center
                    .add(axisA.multiply(Math.cos(angle) * radius))
                    .add(axisB.multiply(Math.sin(angle) * radius));
            if (previous != null) {
                appendLine(lines, previous, point, world, camera);
            } else {
                first = point;
            }
            previous = point;
        }
        if (first != null) {
            appendLine(lines, previous, first, world, camera);
        }
    }

    private static void appendBox(
            List<Vec3d[]> lines,
            Vec3d[] corners,
            RigidTransform3d world,
            Vec3d camera
    ) {
        for (int[] edge : EDGES) {
            appendLine(lines, corners[edge[0]], corners[edge[1]], world, camera);
        }
    }

    private static void appendLine(
            List<Vec3d[]> lines,
            Vec3d from,
            Vec3d to,
            RigidTransform3d world,
            Vec3d camera
    ) {
        Vec3d worldFrom = world.transformPoint(from);
        Vec3d worldTo = world.transformPoint(to);
        lines.add(new Vec3d[] {
                worldFrom.subtract(camera),
                worldTo.subtract(camera)});
    }

    private static Vec3d[] boxCorners(Vec3d min, Vec3d max) {
        Vec3d[] corners = new Vec3d[8];
        corners[0] = new Vec3d(min.x(), min.y(), min.z());
        corners[1] = new Vec3d(max.x(), min.y(), min.z());
        corners[2] = new Vec3d(max.x(), max.y(), min.z());
        corners[3] = new Vec3d(min.x(), max.y(), min.z());
        corners[4] = new Vec3d(min.x(), min.y(), max.z());
        corners[5] = new Vec3d(max.x(), min.y(), max.z());
        corners[6] = new Vec3d(max.x(), max.y(), max.z());
        corners[7] = new Vec3d(min.x(), max.y(), max.z());
        return corners;
    }

    private static void draw(List<Vec3d[]> lines) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableCull();
        GlStateManager.glLineWidth(2.0F);
        try {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            for (Vec3d[] line : lines) {
                buffer.pos(line[0].x(), line[0].y(), line[0].z())
                        .color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
                buffer.pos(line[1].x(), line[1].y(), line[1].z())
                        .color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
            }
            tessellator.draw();
        } finally {
            GlStateManager.glLineWidth(1.0F);
            GlStateManager.enableCull();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }
    }
}
