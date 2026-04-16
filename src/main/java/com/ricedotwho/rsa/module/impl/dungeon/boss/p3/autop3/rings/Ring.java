package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.Argument;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionType;
import com.ricedotwho.rsm.component.impl.Renderer3D;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.Accessor;
import com.ricedotwho.rsm.utils.FileUtils;
import com.ricedotwho.rsm.utils.render.render3d.type.FilledBox;
import com.ricedotwho.rsm.utils.render.render3d.type.OutlineBox;
import lombok.Getter;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class Ring implements Accessor {
    @Getter
    private final AABB box;
    @Getter
    private final AABB renderBox;
    private final AABB fillBox;
    @Getter
    private boolean triggered;
    @Getter
    private boolean active = false;
    @Getter
    private final SubActionManager subManager;
    @Getter
    private final ArgumentManager argManager;

    protected Ring(Pos pos, double radius, double renderOffset) {
        this(pos.subtract(radius, 0, radius), pos.add(radius, radius * 2, radius), renderOffset, null, null); // Centered at bottom
    }

    protected Ring(Vec3 pos, double radius, double renderOffset) {
        this(pos.subtract(radius, 0, radius), pos.add(radius, radius * 2, radius), renderOffset); // Centered at bottom
    }

    protected Ring(Vec3 min, Vec3 max, double renderOffset) {
        this.box = new AABB(min, max);
        this.renderBox = box.contract(renderOffset, renderOffset, renderOffset);
        this.triggered = false;
        this.subManager = null;
        this.argManager = null;
        this.fillBox = new AABB(min.x(), min.y(), min.z(), max.x(), min.y() + 0.05, max.z());
    }

    protected Ring(Pos min, Pos max, double renderOffset, ArgumentManager manager, SubActionManager subManager) {
        this.box = new AABB(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
        this.renderBox = box.contract(renderOffset, renderOffset, renderOffset);
        this.triggered = false;
        this.subManager = subManager;
        this.argManager = manager;
        this.fillBox = new AABB(min.x(), min.y(), min.z(), max.x(), min.y() + 0.05, max.z());
    }

    public boolean isInNode(Vec3 curr, Vec3 prev) {
        AABB feet = new AABB(curr.x - 0.2, curr.y, curr.z - 0.2, curr.x + 0.3, curr.y + 0.5, curr.z);
        boolean intercept = box.clip(curr, prev).isPresent();
        boolean intersects = box.intersects(feet);
        return intercept || intersects;
    }

    public void setTriggered() {
        this.triggered = true;
    }

    public void setActive() {
        this.active = true;
    }

    public void setInactive() {
        this.active = false;
    }

    public boolean updateState(Vec3 playerPos, Vec3 oldPos) {
        boolean bl = isInNode(playerPos, oldPos);

        if (bl && !this.triggered) {
            // Trigger will be set later
            return true;
        }

        if (!bl && this.triggered) {
            reset();
        }
        return false;
    }

    public float getDistanceSq(Vec3 vec3) {
        float dx = (float) (((this.box.maxX + this.box.minX) / 2f) - vec3.x);
        float dy = (float) (((this.box.maxY + this.box.minY) / 2f) - vec3.y);
        float dz = (float) (((this.box.maxZ + this.box.minZ) / 2f) - vec3.z);
        return dx * dx + dy * dy + dz * dz;
    }

    public abstract RingType getType();

    public void reset() {
        this.triggered = false;
        if (this.argManager != null) this.argManager.reset();
    }

    public void render(boolean depth) {
        Colour colour = getColour();
        Renderer3D.addTask(new FilledBox(fillBox, colour.alpha(colour.getAlpha() * 0.2F), depth));
        Renderer3D.addTask(new OutlineBox(fillBox, colour, depth));
    }

    // Run will always run before tick
    public abstract boolean run(); // Return true if can process another ring

    public boolean execute() {
        if (subManager != null) subManager.run();
        return run();
    }

    public boolean checkArg() {
        return argManager != null && argManager.check();
    }

    public abstract Colour getColour();
    public abstract int getPriority();
    public abstract boolean tick(MutableInput mutableInput, Input input, AutoP3 autoP3);
    public abstract void feedback();

    public boolean isStop () {
        return false;
    }

    public boolean shouldStop () {
        return subManager != null && subManager.has(SubActionType.STOP);
    }

    public <T> void consumeArg(Class<? extends Argument<T>> clazz, T value) {
        if (this.argManager != null) this.argManager.consume(clazz, value);
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", this.getType().name());
        obj.add("min", FileUtils.getGson().toJsonTree(new Pos(box.minX, box.minY, box.minZ)));
        obj.add("max", FileUtils.getGson().toJsonTree(new Pos(box.maxX, box.maxY, box.maxZ)));

        if (argManager != null && !argManager.getArgs().isEmpty()) {
            obj.add("args", argManager.serialize());
        }

        if (subManager != null && !subManager.getActions().isEmpty()) {
            obj.add("sub", subManager.serialize());
        }

        return obj;
    }

}
