package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Input;

import java.util.Map;

public class WalkRing extends Ring {
    @Getter
    private final float yaw;

    @Override
    public RingType getType() {
        return RingType.WALK;
    }

    public WalkRing(Pos min, Pos max, ArgumentManager manage, SubActionManager actions, Map<String, Object> extra) {
        this(min, max, (float) extra.getOrDefault("yaw", Minecraft.getInstance().gameRenderer.getMainCamera().yaw()), manage, actions);
    }

    public WalkRing(Pos min, Pos max, float yaw, ArgumentManager manage, SubActionManager actions) {
        super(min, max, RingType.WALK.getRenderSizeOffset(), manage, actions);
        this.yaw = yaw;
    }

    @Override
    public boolean run() {
        return true;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getWalk().getValue();
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public boolean tick(MutableInput mutableInput, Input input, AutoP3 autoP3) {
        if (hasInputPressed(input)) return true;

        autoP3.setDesync(true);
        if (autoP3.getStrafe().getValue() && !mc.player.onGround()) {
            mc.player.setYRot(yaw - 45);
            mutableInput.right(true);
        } else {
            mc.player.setYRot(yaw);
        }

        mutableInput.forward(true);
        mutableInput.sprint(true);
        return false;
    }

    private boolean hasInputPressed(Input input) {
        return input.forward() || input.backward() || input.left() || input.right() || input.jump();
    }

    @Override
    public JsonObject serialize() {
        JsonObject obj = super.serialize();
        obj.addProperty("yaw", this.yaw);
        return obj;
    }

    @Override
    public boolean shouldStop () {
        return true;
    }

    @Override
    public void feedback() {
        AutoP3.modMessage("Walking");
    }
}
