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
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Pair;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class AlignRing extends Ring {
    private Queue<Pair<Float, Boolean>> yaws;

    public AlignRing(Pos min, Pos max, double renderOffset, ArgumentManager manager, SubActionManager actions) {
        super(min, max, renderOffset, manager, actions);
    }

    public AlignRing(Pos min, Pos max, ArgumentManager manager, SubActionManager actions) {
        super(min, max, RingType.ALIGN.getRenderSizeOffset(), manager, actions);
    }

    public AlignRing(Pos min, Pos max, ArgumentManager manager, SubActionManager actions, Map<String, Object> ignored) {
        super(min, max, RingType.ALIGN.getRenderSizeOffset(), manager, actions);
    }

    @Override
    public RingType getType() {
        return RingType.ALIGN;
    }

    @Override
    public boolean run() {
        yaws = null; // need to set for checking if has run
        if (Minecraft.getInstance().player == null || !Minecraft.getInstance().player.onGround()) {
            reset();
            return false;
        }

        Vec3 initialVelocity = Minecraft.getInstance().player.getDeltaMovement();
        Vec2 initialDisplacement = MovementPredictor.getDisplacementVector(new Vec2((float) initialVelocity.x, (float) initialVelocity.z));

        Vec3 position = Minecraft.getInstance().player.position();
        Vec3 boxCenter = this.getBox().getCenter();
        Vec3 target = new Vec3(boxCenter.x, position.y, boxCenter.z);
        Vec3 delta = target.subtract(position.add(initialDisplacement.x, 0d, initialDisplacement.y));
        double deltaLength = delta.length();

        boolean sneaking = true;
        double displacement = MovementPredictor.getDisplacementFromInput(Minecraft.getInstance().player.getSpeed() * 10, sneaking);

        if (deltaLength < 0.01) {
            yaws = new LinkedList<>();
            return false;
        }

        if (deltaLength > 2 * displacement) {
            sneaking = false;
            displacement = MovementPredictor.getDisplacementFromInput(Minecraft.getInstance().player.getSpeed() * 10, sneaking);
            if (deltaLength > 2 * displacement) {
                AutoP3.modMessage("Too far!");
                reset();
                return false;
            }
        }
        KeyMapping.releaseAll();

        double yaw = (float) Math.atan2(-delta.z, delta.x);
        double theta = Math.acos(deltaLength / (2 * displacement));

        yaws = new LinkedList<>();
        yaws.add(new Pair<>((float) -Math.toDegrees(yaw + theta) - 90f, sneaking));
        yaws.add(new Pair<>((float) -Math.toDegrees(yaw - theta) - 90f, sneaking));
        return false;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getAlign().getValue();
    }

    @Override
    public int getPriority() {
        return 100;
    }

    protected double getPrecision() {
        return 0.01 * 0.01;
    }

    @Override
    public boolean tick(MutableInput mutableInput, Input input, AutoP3 autoP3) {
        if (yaws == null) {
            return Minecraft.getInstance().player != null && !Minecraft.getInstance().player.onGround(); // Not run yet
        }

        if (Minecraft.getInstance().player == null) {
            return true;
        }

        if (yaws.isEmpty()) {
            Vec3 vel = Minecraft.getInstance().player.getDeltaMovement();
            if (vel.x == 0 && vel.z == 0) return true;
            if (vel.lengthSqr() > (0.3 * 0.3d)) return false; // Too high yet, can't stop early

            Vec3 boxCenter = this.getBox().getCenter();
            Vec3 target = new Vec3(boxCenter.x, Minecraft.getInstance().player.position().y, boxCenter.z);
            return Minecraft.getInstance().player.position().distanceToSqr(target) <= getPrecision();
        }

        if (yaws.peek().getB() && !Minecraft.getInstance().player.getLastSentInput().shift()) {
            mutableInput.shift(true);
            return false;
        }

        autoP3.setDesync(true);

        Pair<Float, Boolean> entry = yaws.poll();
        Minecraft.getInstance().player.setYRot(entry.getA());
        mutableInput.shift(entry.getB());
        mutableInput.forward(true);
        return false;
    }

    @Override
    public boolean isStop () {
        return true;
    }

    @Override
    public void feedback() {
        AutoP3.modMessage("Aligning!");
    }
}
