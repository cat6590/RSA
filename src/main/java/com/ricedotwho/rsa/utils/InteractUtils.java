package com.ricedotwho.rsa.utils;

import com.ricedotwho.rsa.IMixin.IMultiPlayerGameMode;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.Accessor;
import com.ricedotwho.rsm.utils.EtherUtils;
import com.ricedotwho.rsm.utils.MathUtils;
import com.ricedotwho.rsm.utils.RotationUtils;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;

@UtilityClass
public class InteractUtils implements Accessor {
    public final double BLOCK_RANGE = 5 * 5;
    public final double ENTITY_RANGE = 4d;

    /// Call this from {@link PacketOrderManager#register(PacketOrderManager.STATE, Runnable)} or risk a ban!
    public boolean interactOnEntity(Entity entity) {
        if (mc.player == null) return false;
        Vec3 eyePos = mc.player.position().add(0.0d, mc.player.getEyeHeight(), 0.0d);
        Vec3 location = MathUtils.clamp(entity.getBoundingBox(), eyePos).subtract(entity.getX(), entity.getY(), entity.getZ());
        return interactOnEntity(entity, location);
    }

    /// Call this from {@link PacketOrderManager#register(PacketOrderManager.STATE, Runnable)} or risk a ban!
    public boolean interactOnEntity(Entity entity, Vec3 location) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return false;

        for (InteractionHand interactionHand : InteractionHand.values()) {
            ItemStack itemStack = mc.player.getItemInHand(interactionHand);
            if (!itemStack.isItemEnabled(mc.level.enabledFeatures())) {
                return false;
            }

            InteractionResult interactionResult = mc.gameMode.interactAt(mc.player, entity, new EntityHitResult(entity, location), interactionHand);
            if (!interactionResult.consumesAction()) {
                interactionResult = mc.gameMode.interact(mc.player, entity, interactionHand);
            }

            if (interactionResult instanceof InteractionResult.Success success) {
                if (success.swingSource() == InteractionResult.SwingSource.CLIENT) {
                    mc.player.swing(interactionHand);
                }
                return true;
            }
        }
        return true;
    }


    /// Call this from {@link PacketOrderManager#register(PacketOrderManager.STATE, Runnable)} or risk a ban!
    public boolean interactOnBlock(BlockPos pos, boolean swing) {
        if (mc.player == null || mc.level == null) return false;
        Vec3 eyePos = mc.player.position().add(0.0d, mc.player.getEyeHeight(), 0.0d);
        return interactOnBlock(pos, eyePos, swing);
    }

    /// Call this from {@link PacketOrderManager#register(PacketOrderManager.STATE, Runnable)} or risk a ban!
    public boolean interactOnBlock(BlockPos pos, Vec3 eyePos, boolean swing) {
        if (mc.level == null) return false;
        BlockState blockState = mc.level.getBlockState(pos);
        AABB blockAABB = blockState.getShape(mc.level, pos).bounds();

        Vec3 center = new Vec3((blockAABB.minX + blockAABB.maxX) * 0.5 + pos.getX(), (blockAABB.minY + blockAABB.maxY) * 0.5 + pos.getY(), (blockAABB.minZ + blockAABB.maxZ) * 0.5 + pos.getZ());
        BlockHitResult result = RotationUtils.collisionRayTrace(pos, blockAABB, eyePos, center);
        if (result == null) return false;

        SwapManager.sendBlockC08(result.getLocation(), result.getDirection(), swing, true);
        return true;
    }

    /// Call this from {@link PacketOrderManager#register(PacketOrderManager.STATE, Runnable)} or risk a ban!
    public boolean interactOnBlock(BlockPos pos, Vec3 eyePos, Vec3 hit, boolean swing) {
        if (mc.level == null) return false;
        BlockState blockState = mc.level.getBlockState(pos);
        AABB blockAABB = blockState.getShape(mc.level, pos).bounds();
        BlockHitResult result = RotationUtils.collisionRayTrace(pos, blockAABB, eyePos, hit);
        if (result == null) return false;

        SwapManager.sendBlockC08(result.getLocation(), result.getDirection(), swing, true);
        return true;
    }

    public boolean interactOnBlock0(BlockPos pos) {
        if (mc.player == null) return false;
        Vec3 eyes = mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        return interactOnBlock0(pos, eyes);
    }

    public boolean interactOnBlock0(BlockPos pos, Vec3 eyePos) {
        if (mc.level == null) return false;
        BlockState blockState = mc.level.getBlockState(pos);
        AABB blockAABB = blockState.getShape(mc.level, pos).bounds();
        Vec3 hit = new Vec3((blockAABB.minX + blockAABB.maxX) * 0.5 + pos.getX(), (blockAABB.minY + blockAABB.maxY) * 0.5 + pos.getY(), (blockAABB.minZ + blockAABB.maxZ) * 0.5 + pos.getZ());
        BlockHitResult result = RotationUtils.collisionRayTrace(pos, blockAABB, eyePos, hit);
        if (result == null) return false;
        startUseItem(result);
        return true;
    }

    public boolean interactOnBlock0(BlockPos pos, Vec3 eyePos, Vec3 hit) {
        if (mc.level == null) return false;
        BlockState blockState = mc.level.getBlockState(pos);
        AABB blockAABB = blockState.getShape(mc.level, pos).bounds();
        BlockHitResult result = RotationUtils.collisionRayTrace(pos, blockAABB, eyePos, hit);
        if (result == null) return false;
        startUseItem(result);
        return true;
    }

    public void interactOnBlockSync(HitResult result, boolean sync) {
        if (sync) {
            IMultiPlayerGameMode manager = ((IMultiPlayerGameMode) Minecraft.getInstance().gameMode);
            int i = Minecraft.getInstance().player.getInventory().getSelectedSlot();
            manager.syncSlot();
            if (!SwapManager.checkServerSlot(i)) {
                RSA.chat("Failed to swap to slot : " + i);
                return;
            }
        }

        startUseItem(result);
    }

    public void startUseItem(HitResult hitResult) {
        for(InteractionHand interactionHand : InteractionHand.values()) {
            ItemStack itemStack = mc.player.getItemInHand(interactionHand);
            if (!itemStack.isItemEnabled(mc.level.enabledFeatures())) {
                return;
            }

            if (hitResult != null) {
                switch (hitResult.getType()) {
                    case ENTITY:
                        EntityHitResult entityHitResult = (EntityHitResult) hitResult;
                        Entity entity = entityHitResult.getEntity();
                        if (!mc.level.getWorldBorder().isWithinBounds(entity.blockPosition())) {
                            return;
                        }

                        InteractionResult interactionResult = mc.gameMode.interactAt(mc.player, entity, entityHitResult, interactionHand);
                        if (!interactionResult.consumesAction()) {
                            interactionResult = mc.gameMode.interact(mc.player, entity, interactionHand);
                        }

                        if (interactionResult instanceof InteractionResult.Success) {
                            InteractionResult.Success success = (InteractionResult.Success)interactionResult;
                            if (success.swingSource() == InteractionResult.SwingSource.CLIENT) {
                                mc.player.swing(interactionHand);
                            }

                            return;
                        }
                        break;
                    case BLOCK:
                        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                        int i = itemStack.getCount();
                        InteractionResult interactionResult2 = mc.gameMode.useItemOn(mc.player, interactionHand, blockHitResult);
                        if (interactionResult2 instanceof InteractionResult.Success) {
                            InteractionResult.Success success2 = (InteractionResult.Success)interactionResult2;
                            if (success2.swingSource() == InteractionResult.SwingSource.CLIENT) {
                                mc.player.swing(interactionHand);
                                if (!itemStack.isEmpty() && (itemStack.getCount() != i || mc.player.hasInfiniteMaterials())) {
                                    mc.gameRenderer.itemInHandRenderer.itemUsed(interactionHand);
                                }
                            }

                            return;
                        }

                        if (interactionResult2 instanceof InteractionResult.Fail) {
                            return;
                        }
                }
            }

            if (!itemStack.isEmpty()) {
                InteractionResult interactionResult3 = mc.gameMode.useItem(mc.player, interactionHand);
                if (interactionResult3 instanceof InteractionResult.Success) {
                    InteractionResult.Success success3 = (InteractionResult.Success)interactionResult3;
                    if (success3.swingSource() == InteractionResult.SwingSource.CLIENT) {
                        mc.player.swing(interactionHand);
                    }

                    mc.gameRenderer.itemInHandRenderer.itemUsed(interactionHand);
                    return;
                }
            }
        }
    }


    /// Call this from {@link PacketOrderManager#register(PacketOrderManager.STATE, Runnable)} in {@link PacketOrderManager.STATE#ATTACK} or risk a ban!
    public boolean attackEntity(Entity entity) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return false;

        ItemStack itemStack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!itemStack.isItemEnabled(mc.level.enabledFeatures())) {
            return false;
        }

        mc.gameMode.attack(mc.player, entity);
        mc.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    public void breakBlock(Pos pos, boolean remove, boolean sync) {
        breakBlock(pos, remove, false, sync);
    }

    public void breakBlock(Pos pos, boolean remove, boolean abort, boolean sync) {
        if (faceDistance(pos.asVec3(), mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0)) > BLOCK_RANGE) return;
        BlockPos bp = pos.asBlockPos();
        BlockState state = mc.level.getBlockState(bp);
        if (state.getShape(mc.level, bp).isEmpty()) {
            RSA.chat("Cannot break empty block!");
            return;
        }
        Direction dir = closestFace(pos.asVec3(), mc.player.getEyePosition());
        PacketOrderManager.register(PacketOrderManager.STATE.ATTACK, () -> {
            SwapManager.sendC07(bp, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, dir, true, sync);
            if (remove) mc.level.setBlock(bp, Blocks.AIR.defaultBlockState(), 0);
            else if (abort) SwapManager.sendC07(bp, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, Direction.DOWN, false, sync);
        });
    }

    public double faceDistance(Vec3 pos, Vec3 player) {
        double minDist = Double.MAX_VALUE;
        for (Direction face : Direction.values()) {
            double offsetX = 0;
            double offsetY = 0;
            double offsetZ = 0;

            switch (face) {
                case DOWN:
                    offsetY = -0.5;
                    break;
                case UP:
                    offsetY = 0.5;
                    break;
                case NORTH:
                    offsetZ = -0.5;
                    break;
                case SOUTH:
                    offsetZ = 0.5;
                    break;
                case WEST:
                    offsetX = -0.5;
                    break;
                case EAST:
                    offsetX = 0.5;
                    break;
            }

            Vec3 faceVec = pos.add(0.5 + offsetX, 0.5 + offsetY, 0.5 + offsetZ);
            double dist = player.distanceToSqr(faceVec);

            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
    }

    public Vec3 getFaceVec(Direction direction, BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        VoxelShape shape = state.getShape(mc.level, pos);

        if (shape.isEmpty()) return null;


        AABB box = shape.bounds();

        double x = (box.minX + box.maxX) * 0.5;
        double y = (box.minY + box.maxY) * 0.5;
        double z = (box.minZ + box.maxZ) * 0.5;

        switch (direction) {
            case DOWN -> y = box.minY + EtherUtils.EPSILON;
            case UP -> y = box.maxY - EtherUtils.EPSILON;
            case NORTH -> z = box.minZ + EtherUtils.EPSILON;
            case SOUTH -> z = box.maxZ - EtherUtils.EPSILON;
            case WEST -> x = box.minX + EtherUtils.EPSILON;
            case EAST -> x = box.maxX - EtherUtils.EPSILON;
        }

        return new Vec3(
                pos.getX() + x,
                pos.getY() + y,
                pos.getZ() + z
        );
    }

    public Direction closestFace(Vec3 pos, Vec3 player) {
        double minDist = Double.MAX_VALUE;
        Direction closest = Direction.UP;

        for (Direction face : Direction.values()) {
            double offsetX = 0;
            double offsetY = 0;
            double offsetZ = 0;

            switch (face) {
                case DOWN:
                    offsetY = -0.5;
                    break;
                case UP:
                    offsetY = 0.5;
                    break;
                case NORTH:
                    offsetZ = -0.5;
                    break;
                case SOUTH:
                    offsetZ = 0.5;
                    break;
                case WEST:
                    offsetX = -0.5;
                    break;
                case EAST:
                    offsetX = 0.5;
                    break;
            }

            Vec3 faceVec = pos.add(0.5 + offsetX, 0.5 + offsetY, 0.5 + offsetZ);
            double dist = player.distanceToSqr(faceVec);

            if (dist < minDist) {
                minDist = dist;
                closest = face;
            }
        }
        return closest;
    }

    public Vec3 getClosestPoint(BlockPos pos, Vec3 eyePos) {
        BlockState state = mc.level.getBlockState(pos);
        VoxelShape shape = state.getShape(mc.level, pos);
        if (shape.isEmpty()) return null;

        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;

        for (AABB box : shape.toAabbs()) {
            double x = Mth.clamp(eyePos.x, pos.getX() + box.minX, pos.getX() + box.maxX);
            double y = Mth.clamp(eyePos.y, pos.getY() + box.minY, pos.getY() + box.maxY);
            double z = Mth.clamp(eyePos.z, pos.getZ() + box.minZ, pos.getZ() + box.maxZ);

            Vec3 point = new Vec3(x, y, z);

            double dist = eyePos.distanceToSqr(point);
            if (dist < bestDist) {
                bestDist = dist;
                best = point;
            }
        }

        if (best == null) return null;

        return pushInside(pos, state, best);
    }

    private Vec3 pushInside(BlockPos pos, BlockState state, Vec3 point) {
        AABB box = state.getShape(mc.level, pos).bounds();

        double minX = pos.getX() + box.minX;
        double minY = pos.getY() + box.minY;
        double minZ = pos.getZ() + box.minZ;
        double maxX = pos.getX() + box.maxX;
        double maxY = pos.getY() + box.maxY;
        double maxZ = pos.getZ() + box.maxZ;

        double x = point.x;
        double y = point.y;
        double z = point.z;

        if (Math.abs(x - minX) < EtherUtils.EPSILON) x += EtherUtils.EPSILON;
        if (Math.abs(x - maxX) < EtherUtils.EPSILON) x -= EtherUtils.EPSILON;
        if (Math.abs(y - minY) < EtherUtils.EPSILON) y += EtherUtils.EPSILON;
        if (Math.abs(y - maxY) < EtherUtils.EPSILON) y -= EtherUtils.EPSILON;
        if (Math.abs(z - minZ) < EtherUtils.EPSILON) z += EtherUtils.EPSILON;
        if (Math.abs(z - maxZ) < EtherUtils.EPSILON) z -= EtherUtils.EPSILON;

        return new Vec3(x, y, z);
    }
}
