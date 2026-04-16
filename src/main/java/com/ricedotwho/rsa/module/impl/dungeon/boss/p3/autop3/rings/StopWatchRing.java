package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsa.utils.StopWatch;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.Accessor;
import com.ricedotwho.rsm.utils.NumberUtils;
import net.minecraft.world.entity.player.Input;

import java.util.Map;

public class StopWatchRing extends Ring implements Accessor {

    @Override
    public RingType getType() {
        return RingType.STOPWATCH;
    }

    public StopWatchRing(Pos min, Pos max, ArgumentManager manage, SubActionManager actions, Map<String, Object> extra) {
        this(min, max, manage, actions);
    }

    public StopWatchRing(Pos min, Pos max, ArgumentManager manage, SubActionManager actions) {
        super(min, max, RingType.STOPWATCH.getRenderSizeOffset(), manage, actions);
    }

    @Override
    public boolean run() {
        long millis = StopWatch.auto();
        if (millis != -1) {
            AutoP3.modMessage("Elapsed: %s", NumberUtils.millisToOptMSSMS(millis));
        }
        return true;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getStopWatch().getValue();
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
    public void feedback() {

    }
}
