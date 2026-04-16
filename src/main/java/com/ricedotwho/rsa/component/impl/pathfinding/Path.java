package com.ricedotwho.rsa.component.impl.pathfinding;

import com.mojang.datafixers.util.Function7;
import net.minecraft.core.BlockPos;

import java.util.function.Consumer;

public class Path {
    private final BlockPos start;
    private final PathNode startNode;
    private final PathNode endNode;
    private final Goal goal;

    public Path(BlockPos start, PathNode startNode, PathNode endNode, Goal goal) {
        this.start = start;
        this.startNode = startNode;
        this.endNode = endNode;
        this.goal = goal;
    }

    public BlockPos getStart() {
        return start;
    }

    public PathNode getStartNode() {
        return startNode;
    }

    public PathNode getEndNode() {
        return endNode;
    }

    public int length() {
        int count = 0;
        PathNode node = endNode;
        while (node.getParent() != null) {
            count++;
            node = node.getParent();
        }
        return count;
    }

    public<T> int consumeNodes(Consumer<T> consumer, Function7<Integer, Float, Integer, Float, Float, Boolean, Integer, T> provider, int sequenceStart) {
        PathNode node = this.getEndNode();
        PathNode last = null;
        boolean isLast = true;

        while (node != null) {
            if (last != null) {
                consumer.accept(provider.apply(node.getX(), node.getY(), node.getZ(), last.getYaw(), last.getPitch(), isLast, sequenceStart++));
                isLast = false;
            }
            last = node;
            node = node.getParent();
        }
        return sequenceStart;
    }

    public Goal getGoal() {
        return goal;
    }
}
