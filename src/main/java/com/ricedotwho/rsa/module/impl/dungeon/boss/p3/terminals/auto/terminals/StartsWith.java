package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals;

import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.AutoTerms;
import com.ricedotwho.rsm.RSM;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StartsWith extends Terminal {

    protected StartsWith(ClientboundOpenScreenPacket packet, AbstractContainerMenu menu) {
        super(TerminalType.STARTSWITH, packet, menu);
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
                hashInfo.setEnchanted(true);
            infos.add(hashInfo);
        }

        return Terminal.getTerminalState(TerminalType.STARTSWITH, infos);
    }

    @Override
    public TerminalState getCurrentState() {
        List<HashInfo> infos = new ArrayList<>(this.getType().getSlotCount());
        for (int i = 0; i < this.getType().getSlotCount(); i++) {
            Slot slot = this.terminalContainer.getSlot(i);
            infos.add(new HashInfo(slot.getItem()));
        }

        return Terminal.getTerminalState(TerminalType.STARTSWITH, infos);
    }

    @Override
    public void solve() {
        super.solve();
        Pattern pattern = Pattern.compile("What starts with: '(\\w+)'?");
        Matcher matcher = pattern.matcher(this.getTitle());

        if (!matcher.find()) {
            return;
        }

        String matchLetter = matcher.group(1).toLowerCase();

        List<SolutionClick> solutionClicks  = new ArrayList<>();

        for (Slot slot : this.terminalContainer.slots) {
            ItemStack stack = slot.getItem();

            if (stack.isEmpty()) continue;
            if (RSM.getModule(AutoTerms.class).getClickedSlotsTracker().contains(slot)) continue; // Fuck you, isEnchanted check doesn;t work

            String name = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();

            if (name.startsWith(matchLetter)) {
                solutionClicks.add(new SolutionClick(ClickType.CLONE, slot.index, 0));
            }
        }

        this.solution = new Solution(solutionClicks);
        this.solveState = SolveState.SOLVED;
    }

    @Override
    public boolean isEnabled() {
        return AutoTerms.getTerminals().get("Starts With");
    }

    protected static StartsWith supply(ClientboundOpenScreenPacket packet, AbstractContainerMenu menu) {
        return new StartsWith(packet, menu);
    }
}
