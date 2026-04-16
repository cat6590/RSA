package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals;

import lombok.Getter;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.function.BiFunction;

public enum TerminalType {
    NUMBERS(0, "Click in order!", 35, Numbers::supply),
    COLORS(1, "Select all the", 53, Colors::supply),
    STARTSWITH(2, "What starts with:", 44, StartsWith::supply),
    RUBIX(3, "Change all to same color!", 44, Rubix::supply),
    REDGREEN(4, "Correct all the panes!", 44, RedGreen::supply),
    MELODY(5, "Click the button on time!", 44, Melody::supply);
    @Getter
    private final int id;

    @Getter
    private final int slotCount;

    @Getter
    private final String title;

    @Getter
    private final BiFunction<ClientboundOpenScreenPacket, AbstractContainerMenu, Terminal> supplier;

    TerminalType(int id, String title, int slotCount, BiFunction<ClientboundOpenScreenPacket, AbstractContainerMenu, Terminal> supplier) {
        this.id = id;
        this.title = title;
        this.slotCount = slotCount;
        this.supplier = supplier;
    }

    public static TerminalType getType(String s) {
        for (int i = 0; i < values().length; i++) {
            if (s.startsWith(values()[i].getTitle())) return values()[i];
        }
        return null;
    }

    public Terminal supply(ClientboundOpenScreenPacket packet, AbstractContainerMenu menu) {
        if (this.getSupplier() == null) return null;
        return this.getSupplier().apply(packet, menu);
    }
}
