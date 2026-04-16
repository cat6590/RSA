package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions;

import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.type.EdgeAction;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.type.JumpAction;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.type.LookAction;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.type.StopAction;

import java.util.function.Supplier;

public enum SubActionType {
    LOOK(LookAction::new),
    JUMP(JumpAction::new),
    EDGE(EdgeAction::new),
    STOP(StopAction::new);

    private final Supplier<SubAction> factory;

    SubActionType(Supplier<SubAction> factory) {
        this.factory = factory;
    }

    public SubAction create() {
        return this.factory.get();
    }
}
