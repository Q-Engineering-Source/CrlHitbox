package dev.crlhitbox.api.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** An immutable, nonempty finite union of solid geometry leaves. */
public final class Composite implements Solid3d {
    private final List<Solid3d> children;
    private final Aabb bounds;

    /** Creates a union from a snapshot of {@code children}. */
    public Composite(List<? extends Solid3d> children) {
        Objects.requireNonNull(children, "children");
        if (children.isEmpty()) {
            throw new IllegalArgumentException("children must not be empty");
        }
        List<Solid3d> snapshot = new ArrayList<>(children.size());
        for (int index = 0; index < children.size(); index++) {
            Solid3d child = Objects.requireNonNull(children.get(index), "children[" + index + "]");
            if (child instanceof Composite composite) {
                for (int leafIndex = 0; leafIndex < composite.childCount(); leafIndex++) {
                    snapshot.add(composite.child(leafIndex));
                }
            } else {
                snapshot.add(child);
            }
        }
        this.children = List.copyOf(snapshot);
        Aabb firstBounds = this.children.getFirst().bounds();
        double minX = firstBounds.min().x();
        double minY = firstBounds.min().y();
        double minZ = firstBounds.min().z();
        double maxX = firstBounds.max().x();
        double maxY = firstBounds.max().y();
        double maxZ = firstBounds.max().z();
        for (int index = 1; index < this.children.size(); index++) {
            Aabb childBounds = this.children.get(index).bounds();
            minX = Math.min(minX, childBounds.min().x());
            minY = Math.min(minY, childBounds.min().y());
            minZ = Math.min(minZ, childBounds.min().z());
            maxX = Math.max(maxX, childBounds.max().x());
            maxY = Math.max(maxY, childBounds.max().y());
            maxZ = Math.max(maxZ, childBounds.max().z());
        }
        this.bounds = new Aabb(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ));
    }

    /** Returns the number of stored solid leaves. */
    public int childCount() {
        return children.size();
    }

    /** Returns the solid leaf at {@code index}. */
    public Solid3d child(int index) {
        return children.get(index);
    }

    /** Returns the eagerly stored finite union bounds. */
    @Override
    public Aabb bounds() {
        return bounds;
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof Composite other && children.equals(other.children);
    }

    @Override
    public int hashCode() {
        return children.hashCode();
    }

    @Override
    public String toString() {
        return "Composite[children=" + children + "]";
    }
}
