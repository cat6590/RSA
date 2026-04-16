package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.ricedotwho.rsa.component.impl.Jump;
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

public class JumpRing extends Ring {

    public JumpRing(Pos min, Pos max, ArgumentManager manager, SubActionManager actions) {
        super(min, max, RingType.JUMP.getRenderSizeOffset(), manager, actions);
        this.getArgManager().addArg(new GroundArg());
    }

    public JumpRing(Pos min, Pos max, ArgumentManager manager, SubActionManager actions, Map<String, Object> ignored) {
        super(min, max, RingType.JUMP.getRenderSizeOffset(), manager, actions);
        this.getArgManager().addArg(new GroundArg());
    }

    @Override
    public RingType getType() {
        return RingType.JUMP;
    }

    @Override
    public boolean run() {
        Jump.jump();
        return true;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getJump().getValue();
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
        AutoP3.modMessage("Jumping");
    }
}
