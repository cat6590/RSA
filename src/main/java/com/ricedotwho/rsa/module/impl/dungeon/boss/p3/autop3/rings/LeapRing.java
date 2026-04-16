package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.module.impl.dungeon.FastLeap;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import net.minecraft.world.entity.player.Input;

import java.util.Map;

public class LeapRing extends Ring {

    public LeapRing(Pos min, Pos max, ArgumentManager manager, SubActionManager actions) {
        super(min, max, RingType.LEAP.getRenderSizeOffset(), manager, actions);
    }

    public LeapRing(Pos min, Pos max, ArgumentManager manager, SubActionManager actions, Map<String, Object> ignored) {
        super(min, max, RingType.LEAP.getRenderSizeOffset(), manager, actions);
    }

    @Override
    public RingType getType() {
        return RingType.LEAP;
    }

    @Override
    public boolean run() {
        if (!SwapManager.reserveSwap("SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP")) return false;
        FastLeap.doAutoLeap();
        return true;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getLeap().getValue();
    }

    @Override
    public int getPriority() {
        return 60;
    }

    @Override
    public boolean tick(MutableInput mutableInput, Input input, AutoP3 autoP3) {
        return true;
    }

    @Override
    public void feedback() {
        AutoP3.modMessage("Leaping");
    }
}
