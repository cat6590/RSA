package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.Pos;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class FastBonzoRing extends BonzoRing {
    public FastBonzoRing(Vec3 pos) {
        super(pos);
    }

    public FastBonzoRing(Pos min, Pos max, float yaw, float pitch, ArgumentManager manager, SubActionManager actions) {
        super(min, max, yaw, pitch, manager, actions);
    }

    public FastBonzoRing(Pos min, Pos max, ArgumentManager manager, SubActionManager actions, Map<String, Object> extra) {
        this(min, max, (Float) extra.getOrDefault("yaw", Minecraft.getInstance().gameRenderer.getMainCamera().yaw()), (Float) extra.getOrDefault("pitch", Minecraft.getInstance().gameRenderer.getMainCamera().getXRot()), manager, actions);
    }

    @Override
    protected void registerWaitCondition() {
        PacketOrderManager.registerReceiveListener((p) -> {
            if (Minecraft.getInstance().player == null || this.state < 1) return true;
            if (!(p instanceof ClientboundPingPacket))
                return false;
            this.state++;
            return this.state >= BonzoRing.END_STATE;
        });
    }

    @Override
    public Colour getColour() {
        return AutoP3.getFastBonzo().getValue();
    }

    @Override
    public RingType getType() {
        return RingType.FAST_BONZO;
    }

}
