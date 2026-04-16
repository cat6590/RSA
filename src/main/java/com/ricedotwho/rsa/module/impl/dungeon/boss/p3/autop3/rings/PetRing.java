package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsa.module.impl.player.autopet.AutoPet;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.Accessor;
import net.minecraft.world.entity.player.Input;

import java.util.Map;

public class PetRing extends Ring implements Accessor {
    private final String uuid;

    @Override
    public RingType getType() {
        return RingType.PET;
    }

    public PetRing(Pos min, Pos max, ArgumentManager manage, SubActionManager actions, Map<String, Object> extra) {
        this(min, max, (String) extra.get("uuid"), manage, actions);
    }

    public PetRing(Pos min, Pos max, String uuid, ArgumentManager manage, SubActionManager actions) {
        super(min, max, RingType.CHAT.getRenderSizeOffset(), manage, actions);
        this.uuid = uuid;
    }

    @Override
    public boolean run() {
        AutoPet ap = RSM.getModule(AutoPet.class);
        if (ap == null || !ap.isEnabled()) {
            AutoP3.modMessage("AutoPet is disabled!");
            return true;
        }
        ap.swapTo(this.uuid);
        return true;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getPet().getValue();
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
        obj.addProperty("uuid", this.uuid);
        return obj;
    }

    @Override
    public void feedback() {
        AutoP3.modMessage("Swapping pet");
    }
}
