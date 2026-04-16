package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.Accessor;
import com.ricedotwho.rsm.utils.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;

import java.util.Map;

public class UseRing extends Ring implements Accessor {
    private final String item;
    private final float yaw;
    private final float pitch;

    @Override
    public RingType getType() {
        return RingType.USE;
    }

    public UseRing(Pos min, Pos max, ArgumentManager manage, SubActionManager actions, Map<String, Object> extra) {
        this(min, max,
                ItemUtils.getID(mc.player.getItemInHand(InteractionHand.MAIN_HAND)),
                (float) extra.getOrDefault("yaw", mc.gameRenderer.getMainCamera().yaw()),
                (float) extra.getOrDefault("yaw", mc.gameRenderer.getMainCamera().getXRot()),
                manage, actions);
    }

    public UseRing(Pos min, Pos max, String item, float yaw, float pitch, ArgumentManager manage, SubActionManager actions) {
        super(min, max, RingType.USE.getRenderSizeOffset(), manage, actions);
        this.item = item;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public boolean run() {
        if (!SwapManager.reserveSwap(this.item)) {
            AutoP3.modMessage("Failed to swap to %s!", this.item);
            return false;
        }
        boolean swap = SwapManager.isDesynced();
        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
            if ((swap && !SwapManager.checkClientItem(this.item)) || (!swap && !SwapManager.checkServerItem(this.item))) {
                RSA.chat("Big fuck up! : " + swap + ", " + Minecraft.getInstance().player.getInventory().getItem(SwapManager.getServerSlot()).getItem());
                return;
            }

            if (!SwapManager.sendAirC08(this.yaw, this.pitch, swap, false)) {
                RSA.chat("Failed to send use packet!");
            }
        });
        return true;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getUse().getValue();
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
        obj.addProperty("item", this.item);
        obj.addProperty("yaw", this.yaw);
        obj.addProperty("pitch", this.pitch);
        return obj;
    }

    @Override
    public void feedback() {
        AutoP3.modMessage("Using");
    }
}
