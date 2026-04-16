package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.BreakerAura;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsa.utils.InteractUtils;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Map;

public class BoomRing extends Ring {
    private final Pos target;

    public BoomRing(Pos min, Pos max, Pos target, ArgumentManager manager, SubActionManager actions) {
        super(min, max, RingType.BOOM.getRenderSizeOffset(), manager, actions);
        this.target = target;
    }

    public BoomRing(Pos min, Pos max, ArgumentManager manager, SubActionManager actions, Map<String, Object> ignored) {
        super(min, max, RingType.BOOM.getRenderSizeOffset(), manager, actions);
        if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult blockHitResult) || blockHitResult.getType() == HitResult.Type.MISS) {
            this.target = null;
            return;
        }
        //Vec3 eyePos = mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        //Vec3 dir = blockHitResult.getLocation().subtract(eyePos).normalize().scale(EtherUtils.EPSILON);

        this.target = new Pos(blockHitResult.getBlockPos());
        //this.target.selfAdd(dir.x, dir.y, dir.z);
    }

    @Override
    public RingType getType() {
        return RingType.BOOM;
    }

    @Override
    public boolean run() {
        if (!SwapManager.reserveSwap("INFINITE_SUPERBOOM_TNT", "SUPERBOOM_TNT")) return false;
        BreakerAura.delay();

//        Vec3 eyePos = mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
//        Vec3 targetVec = target.asVec3();

        boolean swap = SwapManager.isDesynced();
        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
//            BlockPos blockPos = BlockPos.containing(targetVec);
//            BlockState blockState = Minecraft.getInstance().level.getBlockState(blockPos);
//            if (blockState.getBlock() == Blocks.AIR) return;
//            VoxelShape voxelShape = blockState.getShape(Minecraft.getInstance().level, blockPos);
//            if (voxelShape.isEmpty()) return;
//            AABB blockAABB = voxelShape.bounds();
//            Vec3 center = new Vec3((blockAABB.minX + blockAABB.maxX) * 0.5 + blockPos.getX(), (blockAABB.minY + blockAABB.maxY) * 0.5 + blockPos.getY(), (blockAABB.minZ + blockAABB.maxZ) * 0.5 + blockPos.getZ());
//            BlockHitResult result = RotationUtils.collisionRayTrace(blockPos, blockAABB, eyePos, center);
//            if (result == null) {
//                AutoP3.modMessage("Failed to find block hit result!");
//                return;
//            }
            InteractUtils.breakBlock(target, false, true, swap);
        });
        return true;
    }

    @Override
    public Colour getColour() {
        return AutoP3.getBoom().getValue();
    }

    @Override
    public int getPriority() {
        return 60;
    }

    @Override
    public boolean tick(MutableInput mutableInput, Input input, AutoP3 autoP3) {
        return true;
    }

    @Override
    public JsonObject serialize() {
        JsonObject obj = super.serialize();
        obj.add("target", FileUtils.getGson().toJsonTree(target));
        return obj;
    }

    @Override
    public void feedback() {
        AutoP3.modMessage("Booming");
    }
}
