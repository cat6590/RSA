package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.recorder.MovementRecorder;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.Accessor;
import net.minecraft.world.entity.player.Input;

import java.util.Map;

public class MovementRing extends Ring implements Accessor {
    private final String route;

    @Override
    public RingType getType() {
        return RingType.MOVEMENT;
    }

    public MovementRing(Pos min, Pos max, ArgumentManager manage, SubActionManager actions, Map<String, Object> extra) {
        this(min, max, (String) extra.getOrDefault("route", MovementRecorder.getData().getFileName()), manage, actions);
    }

    public MovementRing(Pos min, Pos max, String route, ArgumentManager manage, SubActionManager actions) {
        super(min, max, RingType.MOVEMENT.getRenderSizeOffset(), manage, actions);
        this.route = route;
    }

    @Override
    public boolean run() {
        MovementRecorder.playRecording(this.route);
        return true;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getMovement().getValue();
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
    public boolean isStop () {
        return true;
    }

    @Override
    public JsonObject serialize() {
        JsonObject obj = super.serialize();
        obj.addProperty("route", this.route);
        return obj;
    }

    @Override
    public boolean shouldStop () {
        return true;
    }

    @Override
    public void feedback() {
        AutoP3.modMessage("Playing \"%s\"", this.route);
    }
}
