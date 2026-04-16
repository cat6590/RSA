package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.Pos;

import java.util.Map;

public class FastAlign extends AlignRing {

    public FastAlign(Pos min, Pos max, ArgumentManager manager, SubActionManager actions) {
        super(min, max, RingType.FAST_ALIGN.getRenderSizeOffset(), manager, actions);
    }

    public FastAlign(Pos min, Pos max, ArgumentManager manager, SubActionManager actions, Map<String, Object> ignored) {
        super(min, max, RingType.FAST_ALIGN.getRenderSizeOffset(), manager, actions);
    }

    @Override
    public RingType getType() {
        return RingType.FAST_ALIGN;
    }

    @Override
    protected double getPrecision() {
        return 0.25d * 0.25d; // The velocity will carry and it will end up much better than 0.25
    }

    @Override
    public int getPriority() {
        return 105;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getFastAlign().getValue();
    }

    @Override
    public void feedback() {
        AutoP3.modMessage("Aligning faster");
    }
}
