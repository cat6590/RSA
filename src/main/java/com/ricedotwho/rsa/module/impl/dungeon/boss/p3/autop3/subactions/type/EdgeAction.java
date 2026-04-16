package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.type;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.component.impl.Edge;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubAction;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionType;

public class EdgeAction extends SubAction {

    public EdgeAction() {
        super(SubActionType.EDGE);
    }

    @Override
    public boolean execute() {
        Edge.edge();
        return true;
    }

    @Override
    public void serialize(JsonObject obj) {
        obj.addProperty(this.getType().name(), true);
    }
}
