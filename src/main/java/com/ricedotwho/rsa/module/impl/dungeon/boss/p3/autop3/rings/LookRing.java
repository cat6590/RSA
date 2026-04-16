package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.Accessor;
import lombok.Getter;
import net.minecraft.world.entity.player.Input;

import java.util.Map;

public class LookRing extends Ring implements Accessor {
    @Getter
    private final float yaw;
    @Getter
    private final float pitch;

    @Override
    public RingType getType() {
        return RingType.LOOK;
    }

    public LookRing(Pos min, Pos max, ArgumentManager manage, SubActionManager actions, Map<String, Object> extra) {
        this(min, max,
                (float) extra.getOrDefault("yaw", mc.gameRenderer.getMainCamera().yaw()),
                (float) extra.getOrDefault("yaw", mc.gameRenderer.getMainCamera().getXRot()),
                manage, actions);
    }

    public LookRing(Pos min, Pos max, float yaw, float pitch, ArgumentManager manage, SubActionManager actions) {
        super(min, max, RingType.LOOK.getRenderSizeOffset(), manage, actions);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public boolean run() {
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        return true;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getLook().getValue();
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public boolean tick(MutableInput mutableInput, Input input, AutoP3 autoP3) {
        return true;
    }

    @Override
    public JsonObject serialize() {
        JsonObject obj = super.serialize();
        obj.addProperty("yaw", this.yaw);
        obj.addProperty("pitch", this.pitch);
        return obj;
    }

    @Override
    public void feedback() {
        AutoP3.modMessage("Looking");
    }
}
