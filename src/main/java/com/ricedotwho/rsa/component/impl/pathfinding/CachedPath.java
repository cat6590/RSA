package com.ricedotwho.rsa.component.impl.pathfinding;

import java.util.HashMap;

public class CachedPath extends Path {
    HashMap<Integer, PathNode> cache;

    public CachedPath(PathNode endNode) {
        super(null, null, endNode, null);
        cache = new HashMap<>();
        updateCache();
    }

    public void updateCache() {
        cache.clear();

        PathNode node = getEndNode();
        while (node != null) {
            cache.put(node.getIndex(), node);
            node = node.getParent();
        }
    }

    public PathNode getByIndex(int index) {
        return cache.get(index);
    }


}
