package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.module.impl.dungeon.boss.Blink;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.recorder.MovementRecorder;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;

import java.util.List;
import java.util.Map;

public class BlinkRing extends Ring {
    private final String route;
    private final int size;


    public BlinkRing(Pos min, Pos max, ArgumentManager manage, SubActionManager actions, Map<String, Object> extra) {
        this(min, max, (String) extra.getOrDefault("route", MovementRecorder.getData().getFileName()), manage, actions, (int) extra.getOrDefault("blink", 17));
    }

    public BlinkRing(Pos min, Pos max, String route, ArgumentManager manage, SubActionManager actions, int length) {
        super(min, max, RingType.BLINK.getRenderSizeOffset(), manage, actions);
        this.size = Mth.clamp(1, length, 16);
        this.route = route;
    }

    @Override
    public RingType getType() {
        return RingType.BLINK;
    }

    @Override
    public boolean run() {
        if (Minecraft.getInstance().player == null) return false;
        Blink blink = RSM.getModule(Blink.class);

        int packets = Math.min((!blink.isEnabled()) ? 0 : blink.getChargedCount(), this.size);
        List<MovementRecorder.PlayerInput> inputs = MovementRecorder.getInputs(this.route);

        if (inputs.size() <= packets) {
            blink.blinkMovement(inputs);
            return false;
        }

        blink.blinkMovement(inputs.subList(0, packets));
        MovementRecorder.playRecording(this.route);
        MovementRecorder.setPlayIndex(packets);
        return false;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getBlink().getValue();
    }

    @Override
    public int getPriority() {
        return 55;
    }



    @Override
    public boolean tick(MutableInput mutableInput, Input input, AutoP3 autoP3) {
        return true;
    }

    @Override
    public JsonObject serialize() {
        JsonObject obj = super.serialize();
        obj.addProperty("route", this.route);
        obj.addProperty("size", this.size);
        return obj;
    }

    @Override
    public void feedback() {
        //AutoP3.modMessage("Blinking!");
    }
}
