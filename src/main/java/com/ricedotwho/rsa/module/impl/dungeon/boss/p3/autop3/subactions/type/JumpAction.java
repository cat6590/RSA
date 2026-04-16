package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.type;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.component.impl.Jump;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubAction;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionType;

public class JumpAction extends SubAction {

    public JumpAction() {
        super(SubActionType.JUMP);
    }

    @Override
    public boolean execute() {
        if (mc.player == null || !mc.player.onGround()) return false;
        Jump.jump();
        return true;
    }

    @Override
    public void serialize(JsonObject obj) {
        obj.addProperty(this.getType().name(), true);
    }
}
