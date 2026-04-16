package com.ricedotwho.rsa.module.impl.dungeon.autoroutes;

import com.google.gson.JsonObject;
import com.ricedotwho.rsm.component.impl.map.map.UniqueRoom;
import com.ricedotwho.rsm.component.impl.map.utils.RoomUtils;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.FileUtils;
import lombok.Getter;
import lombok.Setter;

public abstract class Node {
    protected final Pos localPos;
    @Getter
    private final float radius;
    @Getter
    private final AwaitManager awaitManager;
    @Getter
    private final boolean start;
    @Getter
    @Setter
    private boolean triggered;
    @Getter
    private int lastTickTime;

    @Getter
    protected transient Pos realPos;

    public Node(Pos localPos) {
        this(localPos, null);
    }

    public Node(Pos localPos, AwaitManager awaitManager) {
        this(localPos, awaitManager, 0.5f, false);
    }

    public Node(Pos localPos, AwaitManager awaitManager, boolean start) {
        this(localPos, awaitManager, 0.5f, start);
    }

    public Node(Pos localPos, AwaitManager awaitManager, float r, boolean start) {
        this.localPos = localPos;
        this.radius = r;
        this.awaitManager = awaitManager;
        this.start = start;

        this.triggered = false;
        this.lastTickTime = -1;
    }

    public boolean hasAwaits() {
        return this.awaitManager != null && this.awaitManager.hasAwaits();
    }

    public boolean shouldAwait() {
        return this.awaitManager != null && this.awaitManager.shouldAwait(this);
    }

    public void calculate(UniqueRoom room) {
        this.realPos = RoomUtils.getRealPosition(this.localPos, room.getMainRoom());
        if (this.hasAwaits()) this.getAwaitManager().resetAwaits();
    }

    public abstract boolean run(Pos playerPos);
    public abstract void render(boolean depth);

    protected boolean cancel() {
        this.reset();
        return false;
    }

    public int getPriority() {
        return 8;
    }

    public boolean isInNode(Pos playerPos) {
        if (AutoRoutes.getCenterOnly().getValue()) {
            return this.realPos.x() == playerPos.x()
                    && playerPos.y() >= this.realPos.y() - 0.05 && playerPos.y() <= this.realPos.y() + 0.05
                    && this.realPos.z() == playerPos.z();
        }
        return playerPos.squaredDistanceTo(this.realPos) <= radius * radius;
    }

    public void updateLastTickTime(int lastTickTime) {
        this.lastTickTime = lastTickTime;
    }

    public boolean hasRanThisTick(int tickTime) {
        return (tickTime <= lastTickTime);
    }

    public void preTrigger(int tickTime) {
        this.lastTickTime = tickTime;
        this.triggered = true;
    }

    public boolean updateNodeState(Pos playerPos, int tickTime) {
        if (tickTime <= lastTickTime) return false; // Don't go do the same node twice in 1 tick, also blocks from setting it to untriggered
        boolean bl = isInNode(playerPos);
        if (bl && !this.triggered) {
            // Trigger will be set later
            return true;
        }

        if (!bl && this.triggered) {
            reset();
        }
        return false;
    }

    public abstract String getName();

    public abstract Colour getColour();

    public JsonObject serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("type", this.getName());
        json.add("localPos", FileUtils.getGson().toJsonTree(localPos));
        json.addProperty("radius", radius);
        json.addProperty("start", start);
        if (this.awaitManager == null || !this.awaitManager.hasAwaits()) return json;
        json.add("awaits", this.awaitManager.serialize());
        return json;
    }

    public void reset() {
        this.triggered = false;
    }
}