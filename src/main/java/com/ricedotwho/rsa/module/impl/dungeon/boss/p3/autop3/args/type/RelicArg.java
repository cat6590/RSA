package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.type;

import com.ricedotwho.rsa.mixins.ServerboundInteractPacketAccessor;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.Argument;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.RingArgType;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;

public class RelicArg extends Argument<PacketEvent.Send> {
    private boolean hasRelic = false;

    public RelicArg() {
        super(RingArgType.RELIC);
    }

    @Override
    public boolean check() {
        return hasRelic;
    }

    @Override
    public void consume(PacketEvent.Send event) {
        if (!(event.getPacket() instanceof ServerboundInteractPacket interactPacket)) return;
        if (Minecraft.getInstance().level == null) return;
        int id = ((ServerboundInteractPacketAccessor) interactPacket).getEntityID();
        Entity entity = Minecraft.getInstance().level.getEntity(id);
        if (!(entity instanceof ArmorStand armorStand)) return;
        String name = ChatFormatting.stripFormatting(armorStand.getItemBySlot(EquipmentSlot.HEAD).getHoverName().getString());
        if (!name.contains("corrupted") || !name.contains("relic")) return;
        hasRelic = true;
    }

    @Override
    public void reset() {
        hasRelic = false;
    }

    @Override
    public String stringValue() {
        return "relic";
    }

    public static RelicArg create(Object ignored) {
        return new RelicArg();
    }

}
