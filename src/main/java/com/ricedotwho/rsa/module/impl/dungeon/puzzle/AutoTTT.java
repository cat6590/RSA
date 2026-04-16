package com.ricedotwho.rsa.module.impl.dungeon.puzzle;

import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsm.module.api.SubModuleInfo;
import com.ricedotwho.rsm.module.impl.dungeon.puzzle.TicTacToe;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.utils.EtherUtils;
import com.ricedotwho.rsm.utils.RotationUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@Getter
@SubModuleInfo(name = "Tic Tac Toe", alwaysDisabled = false)
public class AutoTTT extends TicTacToe {

    private final BooleanSetting auto = new BooleanSetting("Auto", false);
    private final NumberSetting range = new NumberSetting("Range", 1, 6, 4.5, 0.1);
    private final NumberSetting cooldown = new NumberSetting("Cooldown", 100, 1000, 500, 25);

    private long nextClick = 0;

    public AutoTTT(Puzzles puzzles) {
        super(puzzles);
        this.registerProperty(
                auto,
                range,
                cooldown
        );
    }

    @Override
    public void reset() {
        super.reset();
        nextClick = 0;
    }

    @Override
    protected void postSolve() {
        if (getBestMove() == null || !auto.getValue() || System.currentTimeMillis() < nextClick) return;

        Vec3 eyePos = mc.player.position().add(0.0d, mc.player.getLastSentInput().shift() ? EtherUtils.SNEAK_EYE_HEIGHT : Minecraft.getInstance().player.getEyeHeight(Pose.STANDING), 0.0d);
        BlockPos best = getBestMove();
        double dist = eyePos.distanceToSqr(best.getX(), best.getY(), best.getZ());
        double range = getRange().getValue().doubleValue();
        if (dist > range * range) return;
        clickButton(best, eyePos);
    }

    private void clickButton(BlockPos pos, Vec3 eyePos) {
        BlockState blockState = mc.level.getBlockState(pos);
        if (!(blockState.getBlock() instanceof ButtonBlock)) return;

        AABB blockAABB = blockState.getShape(mc.level, pos).bounds();

        Vec3 center = new Vec3((blockAABB.minX + blockAABB.maxX) * 0.5 + pos.getX(), (blockAABB.minY + blockAABB.maxY) * 0.5 + pos.getY(), (blockAABB.minZ + blockAABB.maxZ) * 0.5 + pos.getZ());
        BlockHitResult result = RotationUtils.collisionRayTrace(pos, blockAABB, eyePos, center);
        if (result == null) return;

        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> SwapManager.sendBlockC08(result.getLocation(), result.getDirection(), !mc.player.getLastSentInput().shift() , true));
        nextClick = System.currentTimeMillis() + cooldown.getValue().longValue();
    }
}
