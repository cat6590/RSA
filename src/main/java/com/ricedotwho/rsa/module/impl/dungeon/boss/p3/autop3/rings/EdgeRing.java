package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.ricedotwho.rsa.component.impl.Edge;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.type.GroundArg;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import net.minecraft.world.entity.player.Input;

import java.util.Map;

public class EdgeRing extends Ring {

    public EdgeRing(Pos min, Pos max, ArgumentManager manager, SubActionManager actions) {
        super(min, max, RingType.EDGE.getRenderSizeOffset(), manager, actions);
        this.getArgManager().addArg(new GroundArg());
    }

    public EdgeRing(Pos min, Pos max, ArgumentManager manager, SubActionManager actions, Map<String, Object> ignored) {
        super(min, max, RingType.EDGE.getRenderSizeOffset(), manager, actions);
        this.getArgManager().addArg(new GroundArg());
    }

    @Override
    public RingType getType() {
        return RingType.EDGE;
    }

    @Override
    public boolean run() {
        Edge.edge();
        return true;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getEdge().getValue();
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
        AutoP3.modMessage("Edging");
    }

}
