package com.ricedotwho.rsa.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.module.impl.dungeon.BloodBlink;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.command.Command;
import com.ricedotwho.rsm.command.api.CommandInfo;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.Map;
import com.ricedotwho.rsm.component.impl.map.map.RoomType;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

@CommandInfo(name = "bloodblink", aliases = "bb", description = "Handles blood blinking rooms")
public class BloodBlinkCommand extends Command {

    @Override
    public LiteralArgumentBuilder<ClientSuggestionProvider> build() {
        return literal(name()).executes((source) -> {
            if (!Location.getArea().is(Island.Dungeon)) {
                RSA.chat("I don't think there's a blood room outside dungeons yo");
                return 0;
            }

            BloodBlink bloodBlink = RSM.getModule(BloodBlink.class);
            if (!bloodBlink.isEnabled()) {
                RSA.chat("Please enable blood blink!");
                return 0;
            }

            if (Map.getCurrentRoom() == null || Map.getCurrentRoom().getData().type() != RoomType.ENTRANCE) {
                RSA.chat("You can't blood blink outside of entrance!");
                return 0;
            }

            RSA.chat("Trying blood blinking!");
            bloodBlink.doBlink();
            return 1;
        });
    }

}