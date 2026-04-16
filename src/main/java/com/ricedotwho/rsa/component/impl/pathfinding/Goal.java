package com.ricedotwho.rsa.component.impl.pathfinding;


import net.minecraft.core.BlockPos;

public interface Goal {

    boolean test(int x, int y, int z);

    default boolean test(BlockPos pos) {
        return test(pos.getX(), pos.getY(), pos.getZ());
    }

    double heuristic(int x, int y, int z);

    default double heuristic(BlockPos pos) {
        return heuristic(pos.getX(), pos.getY(), pos.getZ());
    }

    default boolean isPossible() {
        return true;
    }

    BlockPos getEndBlockPos();
}
