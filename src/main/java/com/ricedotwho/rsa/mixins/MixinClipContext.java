package com.ricedotwho.rsa.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.ricedotwho.rsa.module.impl.dungeon.SecretHitboxes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClipContext.class)
public class MixinClipContext {
    @ModifyExpressionValue(method = "getBlockShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ClipContext$Block;get(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape modifyBlockShape(
            VoxelShape original,
            BlockState blockState,
            BlockGetter blockGetter,
            BlockPos blockPos
    ) {
        VoxelShape shape = SecretHitboxes.getShape(blockState, blockPos);
        return shape != null ? shape : original;
    }
}