package com.ricedotwho.rsa.module.impl.dungeon.autoroutes;

import com.mojang.datafixers.util.Function4;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.nodes.*;
import com.ricedotwho.rsm.component.impl.map.map.UniqueRoom;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Arrays;

public enum NodeType {
    ETHERWARP("ew", EtherwarpNode::supply),
    BOOM("boom", BoomNode::supply),
    BAT("bat", BatNode::supply),
    AOTV("aotv", AotvNode::supply),
    BREAK("break", BreakNode::supply),
    USE("use", UseNode::supply);

    @Getter
    private final String name;
    private final Function4<UniqueRoom, LocalPlayer, AwaitManager, Boolean, Node> factory;

    NodeType(String s, Function4<UniqueRoom, LocalPlayer, AwaitManager, Boolean, Node> factory) {
        this.name = s;
        this.factory = factory;
    }

    public Node supply(UniqueRoom fullRoom, AwaitManager awaits, boolean start) {
        if (this.factory == null || Minecraft.getInstance().player == null) return null;
        return this.factory.apply(fullRoom, Minecraft.getInstance().player, awaits, start);
    }

    public static NodeType byName(String name) {
        return Arrays.stream(NodeType.values()).filter(n -> n.getName().equalsIgnoreCase(name)).findAny().orElse(null);
    }
}
