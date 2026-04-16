package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsa.module.impl.movement.VelocityBuffer;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class BonzoRing extends Ring {
    private final float yaw;
    private final float pitch;
    protected byte state;
    protected static final byte END_STATE = 5;

    public BonzoRing(Vec3 pos) {
        super(pos, 0.5, RingType.BONZO.getRenderSizeOffset());
        this.yaw = Minecraft.getInstance().gameRenderer.getMainCamera().yaw();
        this.pitch = Minecraft.getInstance().gameRenderer.getMainCamera().getXRot();
        this.state = 0;
    }

    public BonzoRing(Pos min, Pos max, ArgumentManager manager, SubActionManager actions, Map<String, Object> extra) {
        this(min, max, (float) extra.getOrDefault("yaw", Minecraft.getInstance().gameRenderer.getMainCamera().yaw()), (float) extra.getOrDefault("pitch", Minecraft.getInstance().gameRenderer.getMainCamera().getXRot()), manager, actions);
    }

    public BonzoRing(Pos min, Pos max, float yaw, float pitch, ArgumentManager manager, SubActionManager actions) {
        super(min, max, RingType.BONZO.getRenderSizeOffset(), manager, actions);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public RingType getType() {
        return RingType.BONZO;
    }

    @Override
    public void reset() {
        super.reset();
        this.state = 0;
    }

    protected void registerWaitCondition() {
        PacketOrderManager.registerReceiveListener((p) -> {
            if (Minecraft.getInstance().player == null || this.state != 1) return true;
            if (!(p instanceof ClientboundSetEntityMotionPacket motionPacket) || motionPacket.getId() != Minecraft.getInstance().player.getId())
                return false;
            this.state = END_STATE;
            return true;
        });
    }


    @Override
    public boolean run() {
        if (Minecraft.getInstance().player == null) return false;

        switch (state) {
            case (0) -> {
                super.reset();
                if (!SwapManager.swapItem("STARRED_BONZO_STAFF", "BONZO_STAFF")) return false;
                VelocityBuffer velocityBuffer = RSM.getModule(VelocityBuffer.class);
                if (!velocityBuffer.isEnabled()) velocityBuffer.onKeyToggle();

                PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> SwapManager.sendAirC08(yaw, pitch, true));

                state = 1;
                registerWaitCondition();
                return false;
            }

            case (END_STATE) -> {
                return false;
            }

            default -> {
                super.reset();
                return false;
            }
        }
    }

    @Override
    public JsonObject serialize() {
        JsonObject obj = super.serialize();
        obj.addProperty("yaw", this.yaw);
        obj.addProperty("pitch", this.pitch);
        return obj;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getBonzo().getValue();
    }

    @Override
    public int getPriority() {
        return 75;
    }

    @Override
    public boolean tick(MutableInput mutableInput, Input input, AutoP3 autoP3) {
        return true;
    }

    @Override
    public void feedback() {
        //AutoP3.chat("Bonzo");
    }
}
