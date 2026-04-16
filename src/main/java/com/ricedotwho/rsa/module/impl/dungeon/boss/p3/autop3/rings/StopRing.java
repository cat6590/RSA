package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.MovementPredictor;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class StopRing extends Ring {

    public StopRing(Pos min, Pos max, ArgumentManager manage, SubActionManager actions) {
        super(min, max, RingType.STOP.getRenderSizeOffset(), manage, actions);
    }

    public StopRing(Pos min, Pos max, ArgumentManager manage, SubActionManager actions, Map<String, Object> ignored) {
        super(min, max, RingType.STOP.getRenderSizeOffset(), manage, actions);
    }

    @Override
    public RingType getType() {
        return RingType.STOP;
    }

    @Override
    public boolean run() {
        KeyMapping.releaseAll();
        return false;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getStop().getValue();
    }

    @Override
    public int getPriority() {
        return 110;
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public boolean tick(MutableInput mutableInput, Input input, AutoP3 autoP3) {
        if (Minecraft.getInstance().player == null) return true;

        Vec3 velocity = Minecraft.getInstance().player.getDeltaMovement();
        double speedSq = velocity.horizontalDistanceSqr();
        if (speedSq < 0.0001) return true;

        float yaw = (float) Math.toRadians(Minecraft.getInstance().player.getYRot());

        float fwdX = -Mth.sin(yaw);
        float fwdZ = Mth.cos(yaw);
        float rightX = Mth.cos(yaw);
        float rightZ = Mth.sin(yaw);

        double fwdDot   = velocity.x * fwdX   + velocity.z * fwdZ;
        double rightDot = velocity.x * rightX + velocity.z * rightZ;

        double accel = Minecraft.getInstance().player.getSpeed() * 0.98;
        double baseNextSq = MovementPredictor.squaredAfterTick(fwdDot, rightDot, 0, 0);

        boolean pressFwd = fwdDot < -0.01 && MovementPredictor.squaredAfterTick(fwdDot, rightDot, accel, 0) < baseNextSq;
        boolean pressBack = fwdDot > 0.01 && MovementPredictor.squaredAfterTick(fwdDot, rightDot, -accel, 0) < baseNextSq;
        boolean pressLeft = rightDot >  0.01 && MovementPredictor.squaredAfterTick(fwdDot, rightDot, 0, -accel) < baseNextSq;
        boolean pressRight = rightDot < -0.01 && MovementPredictor.squaredAfterTick(fwdDot, rightDot, 0, accel) < baseNextSq;

        mutableInput.forward(pressFwd);
        mutableInput.backward(pressBack);
        mutableInput.left(pressLeft);
        mutableInput.right(pressRight);
        return true;
    }

    @Override
    public boolean isStop () {
        return true;
    }

    @Override
    public void feedback() {
        AutoP3.modMessage("Stopping");
    }
}
