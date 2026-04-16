package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.Accessor;
import net.minecraft.world.entity.player.Input;

import java.util.Map;

public class CommandRing extends Ring implements Accessor {
    private final String command;

    @Override
    public RingType getType() {
        return RingType.COMMAND;
    }

    public CommandRing(Pos min, Pos max, ArgumentManager manage, SubActionManager actions, Map<String, Object> extra) {
        this(min, max, (String) extra.get("command"), manage, actions);
    }

    public CommandRing(Pos min, Pos max, String message, ArgumentManager manage, SubActionManager actions) {
        super(min, max, RingType.COMMAND.getRenderSizeOffset(), manage, actions);
        this.command = message;
    }

    @Override
    public boolean run() {
        if (this.command.startsWith("`")) {
            mc.getConnection().sendChat(this.command);
        } else {
            mc.getConnection().sendCommand(this.command);
        }
        return true;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getCommand().getValue();
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
        obj.addProperty("command", this.command);
        return obj;
    }

    @Override
    public void feedback() {
        AutoP3.modMessage("Executing \"" + this.command + "\"");
    }
}
