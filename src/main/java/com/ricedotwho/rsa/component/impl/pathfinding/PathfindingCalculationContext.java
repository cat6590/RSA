package com.ricedotwho.rsa.component.impl.pathfinding;

import com.ricedotwho.rsm.data.Pos;
import net.minecraft.world.phys.Vec3;

public record PathfindingCalculationContext(Pos startPos, int threadCount, float yawStep, float pitchStep, float newNodeCost, float heuristicThreshold, boolean fullBlocks) {
    public static PathfindingCalculationContext simple(Vec3 startPos, int threadCount) {
        return new PathfindingCalculationContext(new Pos(startPos), threadCount, 2f, 2f, 100f, 0.5f, true);
    }

    public Pos getMutableStart() {
        return this.startPos;
    }
}
