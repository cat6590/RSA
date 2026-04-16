package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.type;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubAction;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionType;

public class StopAction extends SubAction {

    public StopAction() {
        super(SubActionType.STOP);
    }

    @Override
    public boolean execute() {
        return true;
    }

    @Override
    public void serialize(JsonObject obj) {
        obj.addProperty(this.getType().name(), true);
    }
}
