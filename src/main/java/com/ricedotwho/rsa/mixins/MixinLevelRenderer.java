package com.ricedotwho.rsa.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.ricedotwho.rsa.module.impl.dungeon.SecretHitboxes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelRenderer.class)
public class MixinLevelRenderer {

    @ModifyExpressionValue(method = "extractBlockOutline(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/state/LevelRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape modifyOutlineShape(VoxelShape original, Camera camera, LevelRenderState levelRenderState) {
        BlockHitResult hit = (BlockHitResult) Minecraft.getInstance().hitResult;
        BlockPos pos = hit.getBlockPos();
        BlockState state = Minecraft.getInstance().level.getBlockState(pos);
        VoxelShape shape = SecretHitboxes.getShape(state, pos);
        return shape != null ? shape : original;
    }
}
