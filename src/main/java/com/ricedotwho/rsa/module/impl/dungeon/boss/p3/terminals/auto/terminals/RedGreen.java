package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals;

import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.AutoTerms;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class RedGreen extends Terminal {

    protected RedGreen(ClientboundOpenScreenPacket packet, AbstractContainerMenu menu) {
        super(TerminalType.REDGREEN, packet, menu);
    }

    @Override
    public TerminalState getNextState() {
        if (this.solution == null) throw new IllegalStateException("Tried to get next state without solving!");

        List<HashInfo> infos = new ArrayList<>(this.getType().getSlotCount());
        int changedIndex = solution.getNext().index();
        for (int i = 0; i < this.getType().getSlotCount(); i++) {
            Slot slot = this.terminalContainer.getSlot(i);
            HashInfo hashInfo = new HashInfo(slot.getItem());
            if (slot.index == changedIndex)
                hashInfo.setItem(Items.LIME_STAINED_GLASS_PANE);
            infos.add(hashInfo);
        }

        return Terminal.getTerminalState(TerminalType.REDGREEN, infos);
    }

    @Override
    public TerminalState getCurrentState() {
        List<HashInfo> infos = new ArrayList<>(this.getType().getSlotCount());
        for (int i = 0; i < this.getType().getSlotCount(); i++) {
            Slot slot = this.terminalContainer.getSlot(i);
            infos.add(new HashInfo(slot.getItem()));
        }

        return Terminal.getTerminalState(TerminalType.REDGREEN, infos);
    }

    @Override
    public void solve() {
        super.solve();

        List<SolutionClick> solutionClicks = new ArrayList<>();
        for (Slot slot : this.terminalContainer.slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            if (stack.getItem() != Items.RED_STAINED_GLASS_PANE) continue;

            solutionClicks.add(new SolutionClick(ClickType.CLONE, slot.index, 0));
        }

        this.solution = new Solution(solutionClicks);
        this.solveState = SolveState.SOLVED;
    }

    @Override
    public boolean isEnabled() {
        return AutoTerms.getTerminals().get("Red Green");
    }

    protected static RedGreen supply(ClientboundOpenScreenPacket packet, AbstractContainerMenu menu) {
        return new RedGreen(packet, menu);
    }
}
