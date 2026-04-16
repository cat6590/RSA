package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals;

import com.ricedotwho.rsa.RSA;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;


public abstract class Terminal {
    @Getter
    private final TerminalType type;

    @Getter
    protected SolveState solveState;

    @Getter
    private final String title;
    @Getter
    private final int windowID;

    protected final AbstractContainerMenu terminalContainer;

    @Getter
    protected Solution solution;

    protected Terminal(TerminalType type, ClientboundOpenScreenPacket packet, AbstractContainerMenu terminalContainer) {
        this.type = type;
        this.windowID = packet.getContainerId();
        this.title = packet.getTitle().getString();
        this.solveState = SolveState.NOT_LOADED;
        this.terminalContainer = terminalContainer;
    }

    public void loadSlot(ClientboundContainerSetSlotPacket packet) {
        if (packet.getContainerId() != this.getWindowID()) {
            RSA.chat("Window ID slot load mismatch! -> term : " + this.getWindowID() + " packet : " + packet.getContainerId());
            return;
        }

        if (packet.getSlot() == this.type.getSlotCount() - 1) {
            if (this.solveState == SolveState.NOT_LOADED)
                this.solveState = SolveState.LOADED;
            return;
        }
    }

    public abstract TerminalState getNextState();
    public abstract TerminalState getCurrentState();

    protected static TerminalState getTerminalState(TerminalType type, List<HashInfo> stacks) {
        int hash = 1;
//        RSA.chat("Items Start!");
        for (int i = 0; i < stacks.size(); i++) {
            HashInfo stack = stacks.get(i);

            hash = 31 * hash + stack.getItem();
            hash = 31 * hash + stack.getSize();
            hash = 31 * hash + (stack.isEnchanted() ? 1 : 0);
//            RSA.chat(i + " : " + hash);
        }

        return new TerminalState(type, hash);
    }

    public boolean shouldSolve() {
        return this.solveState != SolveState.NOT_LOADED;
    }

    public boolean isSolved() {
        return this.solution != null && this.solveState != SolveState.NOT_LOADED;
    }

    public void solve() {
        if (this.solveState == SolveState.NOT_LOADED) throw new IllegalStateException("Tried to solve incomplete terminal!");
    }


    public static Terminal fromPacket(ClientboundOpenScreenPacket packet, AbstractContainerMenu menu) {
        MenuType<?> menuType = packet.getType();
        if (menuType != MenuType.GENERIC_9x4 && menuType != MenuType.GENERIC_9x5 && menuType != MenuType.GENERIC_9x6) return null;
        return findTerminalClass(packet, menu);
    }


    private static Terminal findTerminalClass(ClientboundOpenScreenPacket packet, AbstractContainerMenu menu) {
        TerminalType terminalType = TerminalType.getType(packet.getTitle().getString());
        if (terminalType == null) return null;
        return terminalType.supply(packet, menu);
    }

    public abstract boolean isEnabled();

    protected static class HashInfo {
        @Getter
        private boolean enchanted;
        @Getter
        private int item;
        @Getter
        private int size;

        protected HashInfo(ItemStack stack) {
            this.enchanted = stack.isEnchantable() || Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
            this.item = stack.getItem().hashCode();
            this.size = stack.getCount();
        }

        protected void setEnchanted(boolean bl) {
            this.enchanted = bl;
        }

        protected void setItem(Item item) {
            this.item = item.hashCode(); // This is a hash code of the object pointer in memory, items all share the same base objects
        }

        protected void setSize(int size) {
            this.size = size;
        }
    }
}