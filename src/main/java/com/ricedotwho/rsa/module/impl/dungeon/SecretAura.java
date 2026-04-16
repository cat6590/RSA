package com.ricedotwho.rsa.module.impl.dungeon;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.utils.InteractUtils;
import com.ricedotwho.rsm.component.impl.location.Floor;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.Map;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.data.Phase7;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.event.impl.world.BlockChangeEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ModeSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.utils.DungeonUtils;
import com.ricedotwho.rsm.utils.EtherUtils;
import com.ricedotwho.rsm.utils.RotationUtils;
import com.ricedotwho.rsm.utils.Utils;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Getter
@ModuleInfo(aliases = "Secrets", id = "Secrets", category = Category.DUNGEONS, hasKeybind = true)
public class SecretAura extends Module {
    private static final double CHEST_RANGE = 5.745d; // 6.2 // Default block interaction for legit is 4.5
    private static final double SKULL_RANGE = 4.5d;
    private static final double CHEST_RANGE_SQ = CHEST_RANGE * CHEST_RANGE;
    private static final double SKULL_RANGE_SQ = SKULL_RANGE * SKULL_RANGE;


    private static final String REDSTONE_KEY_ID = "fed95410-aba1-39df-9b95-1d4f361eb66e";
    private static final String WITHER_ESSENCE_ID = "e0f3e929-869e-3dca-9504-54c666ee6f23";
    private static final Component CHEST_KEY = Component.translatable("container.chest");
    private static final Component LARGE_CHEST_KEY = Component.translatable("container.chestDouble");

    private final HashSet<Integer> BOSS_LEVERS = new HashSet<>();
    private final HashSet<Integer> LIGHTS_DEV = new HashSet<>();
    private final int jewLeverHash = new BlockPos(61, 134, 142).hashCode();


    private final ModeSetting type = new ModeSetting("Type", "Aura", List.of("Aura", "Triggerbot", "None"));
    private final NumberSetting delay = new NumberSetting("Click Delay", 100, 4000, 150, 50);
    private final NumberSetting reclick = new NumberSetting("Re-Click Delay", 200, 10000, 500, 50);
    private final NumberSetting swapSlot = new NumberSetting("Swap Slot Index", 0, 7, 0, 1);
    private final BooleanSetting invWalk = new BooleanSetting("In inventory", true);
    private final BooleanSetting allowReclick = new BooleanSetting("Allow Re-click", true);
    private final BooleanSetting allowBossReclick = new BooleanSetting("Allow Boss Re-click", true);
    private final BooleanSetting inBoss = new BooleanSetting("In Boss", true);
    private final BooleanSetting autoClose = new BooleanSetting("Auto Close GUI", false);
    private final BooleanSetting forceSkyblock = new BooleanSetting("Force Skyblock", false);


    private boolean hasRedstoneKey = false;
    private final Int2LongOpenHashMap clickedBlocks = new Int2LongOpenHashMap(5);
    private final IntOpenHashSet blocksDone = new IntOpenHashSet();
    private int clickBlockCooldown = 20;
    private int lastSlot = -1;

    public SecretAura() {
        this.registerProperty(
                type,
                delay,
                reclick,
                swapSlot,
                invWalk,
                allowReclick,
                allowBossReclick,
                inBoss,
                autoClose,
                forceSkyblock
        );

        this.BOSS_LEVERS.add(new BlockPos(106, 124, 113).hashCode()); //S1 Lever 1
        this.BOSS_LEVERS.add(new BlockPos(94, 124, 113).hashCode()); //S1 Lever 2
        this.BOSS_LEVERS.add(new BlockPos(23, 132, 138).hashCode()); //S2 Top Lever
        this.BOSS_LEVERS.add(new BlockPos(27, 124, 127).hashCode()); //S2 Bottom Lever
        this.BOSS_LEVERS.add(new BlockPos(2, 122, 55).hashCode()); //S3 Lever 1
        this.BOSS_LEVERS.add(new BlockPos(14, 122, 55).hashCode()); //S3 Lever 2
        this.BOSS_LEVERS.add(new BlockPos(84, 121, 34).hashCode()); //S4 Bottom Lever
        this.BOSS_LEVERS.add(new BlockPos(86, 128, 46).hashCode()); //S4 Top Lever

        this.LIGHTS_DEV.add(new BlockPos(58, 133, 142).hashCode()); //Lights bottom right
        this.LIGHTS_DEV.add(new BlockPos(58, 136, 142).hashCode()); //Lights top right
        this.LIGHTS_DEV.add(new BlockPos(62, 136, 142).hashCode()); //Lights top left
        this.LIGHTS_DEV.add(new BlockPos(62, 133, 142).hashCode()); //Lights bottom left
        this.LIGHTS_DEV.add(new BlockPos(60, 135, 142).hashCode()); //Lights middle top
        this.LIGHTS_DEV.add(new BlockPos(60, 134, 142).hashCode()); //Lights middle bottom
    }


    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        this.clear();
        this.clickBlockCooldown = 20;
    }

    @SubscribeEvent
    public void onTickEnd(ClientTickEvent.Start event) {
        clickBlockCooldown--;
    }

    @SubscribeEvent
    public void onSendPacket(PacketEvent.Send event) {
        if (!(event.getPacket() instanceof ServerboundUseItemOnPacket)) return;
        clickBlockCooldown = 1;
    }

    @SubscribeEvent
    public void onReceivePacket(PacketEvent.Receive event) {
        if (!this.autoClose.getValue() || !Location.getArea().is(Island.Dungeon)) return;
        if (!(event.getPacket() instanceof ClientboundOpenScreenPacket openScreenPacket) || Minecraft.getInstance().getConnection() == null) return;
        RSA.getLogger().info("Container title: {}", openScreenPacket.getTitle());
        String content = ChatFormatting.stripFormatting(openScreenPacket.getTitle().getString());
        if (!Utils.equalsOneOf(openScreenPacket.getTitle(), CHEST_KEY, LARGE_CHEST_KEY) && !Utils.equalsOneOf(content, "Chest", "Large Chest")) return;

        int windowId = openScreenPacket.getContainerId();
        Minecraft.getInstance().getConnection().send(new ServerboundContainerClosePacket(windowId));
        // This is technically supposed to stop movement inputs when it sends while closing a gui
        // But it doesn't because that would be annoying
        // And hypixel doesn't seem to care
        // It seems that the packet is normally sent off tick, so packet order shouldn't matter
        // https://github.com/GrimAnticheat/Grim/blob/2b621483e7ccd140e6631cd049bab0a09edf24af/common/src/main/java/ac/grim/grimac/checks/impl/multiactions/MultiActionsD.java#L11
        event.setCancelled(true);
    }

    @SubscribeEvent
    public void onTickStart(ClientTickEvent.Start event) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null || type.is("None")) return;
        if (!forceSkyblock.getValue() && (!Location.getArea().is(Island.Dungeon) || isRoomDisabled())) return;
        if (!forceSkyblock.getValue() && Dungeon.isInBoss() && !inBoss.getValue()) return;
        if (!invWalk.getValue() && Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>) return;

        ClientLevel level = Minecraft.getInstance().level;

        Iterable<BlockPos> positions;

        boolean sneaking = Minecraft.getInstance().player.getLastSentInput().shift();
        Vec3 eyePos = Minecraft.getInstance().player.position().add(0.0d, sneaking ? EtherUtils.SNEAK_EYE_HEIGHT : Minecraft.getInstance().player.getEyeHeight(Pose.STANDING), 0.0d);
        Vec3 flooredEyePos = eyePos.subtract(0.5d, 0d, 0.5d);

        if (type.is("Aura")) {
            AABB box = new AABB(eyePos, eyePos).inflate(CHEST_RANGE, CHEST_RANGE, CHEST_RANGE);
            positions = BlockPos.betweenClosed(box);
        } else {
            // Triggerbot
            if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult blockHitResult)) return;
            positions = Collections.singleton(blockHitResult.getBlockPos());
        }

        boolean bl = forceSkyblock.getValue() || (Location.getArea().is(Island.Dungeon) && (Location.getFloor() == Floor.F7 || Location.getFloor() == Floor.M7) && DungeonUtils.isPhase(Phase7.P3));

        double bestDistance = Double.MAX_VALUE;
        BlockPos bestCandidate = null;

        boolean bl2 = !allowReclick.getValue();
        for (BlockPos blockPos : positions) {
            int hash = getBlockPosHash(blockPos);

            BlockState blockState = level.getBlockState(blockPos);
            Block block = blockState.getBlock();
            long delay = this.delay.getValue().longValue();
            if (bl) {
                if (Dungeon.isInBoss() && block != Blocks.LEVER) continue;
                if (block == Blocks.LEVER) {
                    // Either we aren't in boss or we rejoined boss
                    if (checkF7BossBlock(blockPos, blockState)) {
                        if (!inBoss.getValue()) continue;
                        if (allowBossReclick.getValue()) bl2 = false; // Allow reclick
                        delay = 0; // No lever delay in boss
                    } else if (checkLightsDev(blockPos)) continue;
                }
            }

            if (bl2 && blocksDone.contains(hash)) continue; // Allow reclick anyways on boss levers


            if (!isValidBlock(block) && (block != Blocks.PLAYER_HEAD || !isValidSkull(blockPos, level))) continue;
            if (Dungeon.isInBoss() && block == Blocks.PLAYER_HEAD) continue;
            if (getSkullType(blockPos, level).equals(SkullType.KEY)) hasRedstoneKey = false;

            if (!clickedBlocks.containsKey(hash)) {
                if (delay > 0) {
                    clickedBlocks.put(hash, System.currentTimeMillis() + delay);
                    continue;
                }
                clickedBlocks.put(hash, System.currentTimeMillis());
            }

            long nextClickTime = clickedBlocks.get(hash);
            if (nextClickTime > System.currentTimeMillis()) continue;

            double d = flooredEyePos.distanceToSqr(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if ((block == Blocks.PLAYER_HEAD && d > SKULL_RANGE_SQ) || d > CHEST_RANGE_SQ) continue;

            if (d < bestDistance) {
                bestDistance = d;
                bestCandidate = new BlockPos(blockPos);
            }
        }

        // Don't return earlier so we can register than we have seen the clicked blocks
        if (bestCandidate == null) return; // || clickBlockCooldown > 0

        BlockState blockState = level.getBlockState(bestCandidate);
        Block block = blockState.getBlock();
        if ((block == Blocks.PLAYER_HEAD || Minecraft.getInstance().player.getInventory().getSelectedSlot() == 8) && !SwapManager.swapSlot(swapSlot.getValue().intValue())) return;

        clickedBlocks.put(getBlockPosHash(bestCandidate), System.currentTimeMillis() + reclick.getValue().longValue()); // re-click delay

        AABB blockAABB = blockState.getShape(level, bestCandidate).bounds();

        Vec3 center = new Vec3((blockAABB.minX + blockAABB.maxX) * 0.5 + bestCandidate.getX(), (blockAABB.minY + blockAABB.maxY) * 0.5 + bestCandidate.getY(), (blockAABB.minZ + blockAABB.maxZ) * 0.5 + bestCandidate.getZ());
        BlockHitResult result = RotationUtils.collisionRayTrace(bestCandidate, blockAABB, eyePos, center);
        if (result == null) return;

        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
            //SwapManager.sendBlockC08(result.getLocation(), result.getDirection(), block != Blocks.PLAYER_HEAD, true);
            InteractUtils.interactOnBlockSync(result, true);
        });
    }

    private boolean checkLightsDev(BlockPos pos) {
        return pos.getZ() == 142 && pos.getY() <= 136 && pos.getY() >= 133 && pos.getX() >= 58 && pos.getX() <= 62;
    }

    // Only pass in LeverBlock here, otherwise it will crash
    private boolean checkF7BossBlock(BlockPos pos, BlockState block) {
        int hash = pos.hashCode();
        return this.BOSS_LEVERS.contains(hash) || (checkLightsDev(pos) && ((this.LIGHTS_DEV.contains(hash) && !block.getValue(LeverBlock.POWERED)) || hash == jewLeverHash));
    }

    private boolean isValidBlock(Block block) {
        if (block == Blocks.AIR) return false;
        return block == Blocks.LEVER || block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || (block == Blocks.REDSTONE_BLOCK && hasRedstoneKey);
    }

    public static boolean isValidSkull(BlockPos blockPos, ClientLevel level) {
        return isValidSkull(blockPos, level, false);
    }

    public static boolean isValidSkull(BlockPos blockPos, ClientLevel level, boolean keyOnly) {
        BlockEntity entity = level.getBlockEntity(blockPos);
        if (!(entity instanceof SkullBlockEntity skullBlockEntity)) return false;
        return isValidProfile(skullBlockEntity.getOwnerProfile(), keyOnly);
    }

    public static boolean isValidProfile(ResolvableProfile gameProfile, boolean keyOnly) {
        if (gameProfile == null) return false;
        String uuid = gameProfile.partialProfile().id().toString();
        if (keyOnly) return uuid.equals(REDSTONE_KEY_ID);
        return switch (uuid) {
            case WITHER_ESSENCE_ID, REDSTONE_KEY_ID -> true;
            default -> false;
        };
    }

    public static SkullType getSkullType(BlockPos blockPos, ClientLevel level) {
        BlockEntity entity = level.getBlockEntity(blockPos);
        if (!(entity instanceof SkullBlockEntity skullBlockEntity)) return SkullType.NONE;
        return getSkullType(skullBlockEntity.getOwnerProfile());
    }

    public static SkullType getSkullType(ResolvableProfile gameProfile) {
        if (gameProfile == null) return SkullType.NONE;
        String uuid = gameProfile.partialProfile().id().toString();
        return switch (uuid) {
            case WITHER_ESSENCE_ID -> SkullType.ESSENCE;
            case REDSTONE_KEY_ID -> SkullType.KEY;
            default -> SkullType.NONE;
        };
    }



    private boolean isRoomDisabled() {
        if (!Location.getArea().is(Island.Dungeon) || Dungeon.isInBoss()) return false;
        if (Location.getArea().is(Island.Dungeon) && Map.getCurrentRoom() == null) return true; // So it doesn't break when leaping into room, might need to remove this
        return switch (Map.getCurrentRoom().getData().name()) {
            case "Water Board", "Three Weirdos" -> true;
            default -> false;
        };
    }



    // Y range : 0 to 255
    // X range: -2048 to 2047
    // Z range: -2048 to 2047
    private static int getBlockPosHash(BlockPos blockPos) {
        return (blockPos.getY() & 0xFF) | (((blockPos.getX() + 2048) & 0xFFF) << 8) | (((blockPos.getZ() + 2048) & 0xFFF) << 20);
    }

    @SubscribeEvent
    public void onPacket(PacketEvent.Receive event) {
        if (mc.player == null || mc.level == null || type.is("None")) return;
        if (event.getPacket() instanceof ClientboundBlockEventPacket packet) {
            //RSA.chat("block event: " + packet.getBlock());
            // ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            if (packet.getBlock().equals(Blocks.CHERRY_LOG)) {
                blocksDone.add(getBlockPosHash(packet.getPos()));
            }
        } else if (event.getPacket() instanceof ClientboundSetEquipmentPacket packet) {
            Entity entity = mc.level.getEntity(packet.getEntity());
            if (!(entity instanceof ArmorStand) || packet.getSlots().size() < 4) return;
            ItemStack stack = packet.getSlots().get(4).getSecond();
            if (!stack.is(Items.PLAYER_HEAD)) return;
            Optional<? extends ResolvableProfile> profile = stack.getComponentsPatch().get(DataComponents.PROFILE);
            if (profile == null || profile.isEmpty() || !isValidProfile(profile.get(), true)) return;
            blocksDone.add(getBlockPosHash(new BlockPos(entity.getBlockX(), entity.getBlockY() + 2, entity.getBlockZ())));
        }
    }

    @SubscribeEvent
    public void onChat(ChatEvent.Chat event) {
        if (mc.player == null || mc.level == null || type.is("None") || !inBoss.getValue()) return;
        String content = ChatFormatting.stripFormatting(event.getMessage().getString());
        if ("[BOSS] Goldor: Who dares trespass into my domain?".equals(content)) {
            clear();
            RSA.chat("Blocks cleared!");
        }
    }

    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event) {
        if (mc.player == null || mc.level == null || type.is("None")) return;
        if (event.getOldState().is(Blocks.LEVER) && mc.player.distanceToSqr(event.getPos().asVec3()) < 40) {
            blocksDone.add(getBlockPosHash(event.getBlockPos()));
            //RSA.chat("Added %s", event.getPos().toChatString());
        } else if (event.getOldState().is(Blocks.PLAYER_HEAD)) {
            if (!event.getNewState().is(Blocks.AIR)) return;
            if (isValidSkull(event.getBlockPos(), mc.level, true)) {
                hasRedstoneKey = true;
            }
        } else if (event.getOldState().is(Blocks.REDSTONE_BLOCK)) {
            blocksDone.add(getBlockPosHash(event.getBlockPos()));
        }
    }

    public void clear() {
        blocksDone.clear();
        hasRedstoneKey = false;
    }

    @Override
    public void onEnable() {
        this.clear();
    }

    @Override
    public void onDisable() {

    }

    public enum SkullType {
        ESSENCE,
        KEY,
        NONE
    }
}
