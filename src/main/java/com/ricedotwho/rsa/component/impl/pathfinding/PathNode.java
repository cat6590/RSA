package com.ricedotwho.rsa.component.impl.pathfinding;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.function.Predicate;

public class PathNode {
    @Getter
    private final int x;
    @Getter
    private final float y;
    @Getter
    private final int z;
    @Getter
    private final double heuristicCost;
    public int heapPosition;
    private PathNode parent;
    @Getter
    @Setter
    private float yaw = Float.MIN_VALUE;
    @Getter
    @Setter
    private float pitch = Float.MIN_VALUE;

    @Getter
    private int index;


    public PathNode(int x, float y, int z, PathNode parent, Goal goal) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.parent = parent;
        this.index = (parent == null ? 0 : (parent.index + 1));
        this.heapPosition = -1;
        this.heuristicCost = goal.heuristic(x, (int) y, z);
    }


    public boolean hasBeenScanned() {
        return this.yaw != Float.MIN_VALUE;
    }

//    public List<BlockPos> getNear(Predicate<BlockPos> predicate) {
//        List<BlockPos> blocks = new ArrayList<>();
//        testOffset(predicate, blocks, pos.add(1, 0, 0));
//        testOffset(predicate, blocks, pos.add(-1, 0, 0));
//        testOffset(predicate, blocks, pos.add(0, 0, -1));
//        testOffset(predicate, blocks, pos.add(0, 0, 1));
//        testOffset(predicate, blocks, pos.add(0, -1, 0));
//        testOffset(predicate, blocks, pos.add(0, 1, 0));
//        return blocks;
//    }

    public boolean isOpen() {
        return heapPosition != -1;
    }

    @Override
    public int hashCode() {
        long hash = 3241;
        hash = 3457689L * hash + getX();
        hash = 8734625L * hash + ((int) (getY() * 100f));
        hash = 2873465L * hash + getZ();
        return (int) hash;
    }

    public static int hashCode(BlockPos pos) {
        long hash = 3241;
        hash = 3457689L * hash + pos.getX();
        hash = 8734625L * hash + pos.getY();
        hash = 2873465L * hash + pos.getZ();
        return (int) hash;
    }

    public synchronized PathNode getParent() {
        return this.parent;
    }

    private void testOffset(Predicate<BlockPos> predicate, List<BlockPos> blocks, BlockPos pos) {
        if (predicate.test(pos)) blocks.add(pos);
    }


    public synchronized double getCost(float nodeCost) {
        return getMoveCost(nodeCost) + heuristicCost;
    }

    public synchronized double getMoveCost(float nodeCost) {
        return index * nodeCost;
    }

    public synchronized void updateParent(PathNode parent) {
        this.parent = parent;
        this.index = parent.index + 1;
    }

    public BlockPos getBlockPos() {
        return BlockPos.containing(x, y, z);
    }

    @Override
    public synchronized boolean equals(Object obj) {
        PathNode other = (PathNode) obj;
        return getX() == other.getX() && getY() == other.getY() && getZ() == other.getZ();
    }

}
